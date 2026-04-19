package com.github.mpalambonisi.product.scraper;

import com.github.mpalambonisi.product.model.Category;
import com.github.mpalambonisi.product.model.Product;
import com.github.mpalambonisi.product.repository.CategoryRepository;
import com.github.mpalambonisi.product.repository.ProductRepository;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Service for scraping product data from external sources. */
@Service
public class ScraperService {

  private static final Logger log = LoggerFactory.getLogger(ScraperService.class);

  private final CategoryRepository categoryRepository;
  private final ProductRepository productRepository;

  // --- Constants for Scraping ---
  private static final String SELECTOR_TOTAL_COUNT = "span.refinements__total-items";
  private static final String SELECTOR_PRODUCT_TILE = "li.product-grid__item";
  private static final String SELECTOR_NAME = "div.product-tile__name";
  private static final String SELECTOR_PRICE = "div.price-tile__sales";
  private static final String SELECTOR_IMAGE = "meta[itemprop=\"image\"]";
  private static final String SELECTOR_GTM_DATA = "form.pdpForm";
  private static final String BASE_URL = "https://zakupy.biedronka.pl";
  private static final String USER_AGENT =
      "Mozilla/5.0 (Windows NT 10.0; Win64; x64)"
          + "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/108.0.0.0 Safari/537.36";
  private static final Gson gson = new Gson();
  private static final int ITEMS_PER_PAGE = 32;

  /**
   * Constructs the scraper service.
   *
   * @param categoryRepository the repository for categories
   * @param productRepository the repository for products
   */
  public ScraperService(
      CategoryRepository categoryRepository, ProductRepository productRepository) {
    this.categoryRepository = categoryRepository;
    this.productRepository = productRepository;
  }

  /**
   * Scrapes product data from a single category's URL across multiple pages.
   *
   * <p>This method now updates existing products instead of creating duplicates. It identifies
   * products by their name, unit, and category combination.
   *
   * @param categoryName The name of the category to scrape. This category must exist in the
   *     database.
   * @param categoryUrl The relative URL path for the category (e.g., "/dzial/warzywa-i-owoce").
   * @return A list of scraped Product entities (both new and updated).
   * @throws RuntimeException if the specified category name is not found in the database.
   */
  @Transactional
  public List<Product> scrapeCategory(String categoryName, String categoryUrl) {
    List<Product> masterProductList = new ArrayList<>();

    // Fetch category
    Category category =
        categoryRepository
            .findByName(categoryName)
            .orElseThrow(
                () ->
                    new RuntimeException(
                        "Category not found: "
                            + categoryName
                            + ". Please run the init_categories.sql script first."));

    int totalItems;
    try {
      totalItems = getTotalItemCount(categoryUrl);
    } catch (IOException e) {
      log.error("Failed to get total item count for {}: {}", categoryName, e.getMessage());
      return masterProductList;
    }

    if (totalItems == 0) {
      log.warn("No items found or count failed for {}", categoryName);
      return masterProductList;
    }

    int totalPages = (int) Math.ceil((double) totalItems / ITEMS_PER_PAGE);
    log.info(
        "Scraping '{}'. Found {} items across {} pages...", categoryName, totalItems, totalPages);

    int newProductsCount = 0;
    int updatedProductsCount = 0;

    for (int pageNumber = 1; pageNumber <= totalPages; pageNumber++) {
      String urlToScrape =
          BASE_URL + categoryUrl + "/?format=ajax&onlyRefinements=false&page=" + pageNumber;
      log.debug("Scraping page: {}/{}", pageNumber, totalPages);

      try {
        Document doc =
            Jsoup.connect(urlToScrape)
                .header("X-Requested-With", "XMLHttpRequest")
                .userAgent(USER_AGENT)
                .timeout(15000)
                .get();
        Elements productTiles = doc.select(SELECTOR_PRODUCT_TILE);

        if (productTiles.isEmpty()) {
          log.warn("Page {} was empty, stopping scrape for this category early.", pageNumber);
          break;
        }

        for (Element tile : productTiles) {
          String name = tile.select(SELECTOR_NAME).text();
          String priceText = tile.select(SELECTOR_PRICE).text();
          String imageUrl = tile.select(SELECTOR_IMAGE).attr("content");
          String brand = parseBrandFromGtmData(tile, name);

          log.debug("Product: {} | Raw price text: '{}'", name, priceText);

          PriceUnit priceUnit = parsePriceAndUnit(priceText);

          if (priceUnit.price().compareTo(BigDecimal.ZERO) == 0) {
            log.warn("Failed to parse price for product: {} | Raw text: '{}'", name, priceText);
          }

          // Check if product already exists (by name, unit, and category)
          Optional<Product> existingProduct = findExistingProduct(name, priceUnit.unit(), category);

          Product product;
          if (existingProduct.isPresent()) {
            // Update existing product
            product = existingProduct.get();
            product.setPrice(priceUnit.price());
            product.setImageUrl(makeAbsoluteUrl(imageUrl));
            product.setBrand(brand);
            updatedProductsCount++;
            log.debug("Updating existing product: {} ({})", name, priceUnit.unit());
          } else {
            // Create new product
            product =
                new Product(
                    name,
                    priceUnit.price(),
                    priceUnit.unit(),
                    makeAbsoluteUrl(imageUrl),
                    brand,
                    category);
            newProductsCount++;
            log.debug("Creating new product: {} ({})", name, priceUnit.unit());
          }

          masterProductList.add(product);
        }
        Thread.sleep(500); // Be respectful to the server
      } catch (IOException | InterruptedException e) {
        log.error("Error scraping page {}: {}", pageNumber, e.getMessage());
        Thread.currentThread().interrupt();
      }
    }

    log.info(
        "Finished scraping '{}'. New products: {}, Updated products: {}, Total: {}",
        categoryName,
        newProductsCount,
        updatedProductsCount,
        masterProductList.size());

    return masterProductList;
  }

  /**
   * Finds an existing product by name, unit, and category.
   *
   * @param name The product name
   * @param unit The product unit
   * @param category The product category
   * @return Optional containing the product if found
   */
  private Optional<Product> findExistingProduct(String name, String unit, Category category) {
    return productRepository.findByNameAndUnitAndCategoryId(name, unit, category.getId());
  }

  private int getTotalItemCount(String categoryUrl) throws IOException {
    String url = BASE_URL + categoryUrl;
    log.debug("Finding total item count from: {}", url);
    Document doc = Jsoup.connect(url).userAgent(USER_AGENT).timeout(10000).get();
    Element countElement = doc.selectFirst(SELECTOR_TOTAL_COUNT);
    if (countElement == null) {
      log.warn("Could not find total count selector! Assuming 0 items.");
      return 0;
    }
    String countText = countElement.text().split(" ")[0];
    try {
      return Integer.parseInt(countText);
    } catch (NumberFormatException e) {
      log.error("Could not parse total count: {}", countText);
      return 0;
    }
  }

  private String parseBrandFromGtmData(Element tile, String productName) {
    try {
      Element formElement = tile.selectFirst(SELECTOR_GTM_DATA);
      if (formElement != null) {
        String gtmDataString = formElement.attr("data-product-gtm");
        JsonObject gtmJson = gson.fromJson(gtmDataString, JsonObject.class);
        if (gtmJson.has("item_brand")) {
          return gtmJson.get("item_brand").getAsString();
        }
      }
    } catch (Exception e) {
      log.error("Error parsing GTM brand for {}: {}", productName, e.getMessage());
    }
    return "N/A";
  }

  /**
   * Parses price and unit from Biedronka format like "12 99 /kg" or "13 99 /szt." Returns a record
   * containing both price and unit.
   */
  private PriceUnit parsePriceAndUnit(String priceString) {
    if (priceString == null || priceString.isBlank()) {
      log.warn("Price string is null or blank");
      return new PriceUnit(BigDecimal.ZERO, "N/A");
    }

    log.debug("Raw price string: '{}'", priceString);

    Pattern pattern = Pattern.compile("(\\d+)\\s+(\\d{2})\\s*/([a-zżźćńółęąśA-ZŻŹĆŃÓŁĘĄŚ.]+)");
    Matcher matcher = pattern.matcher(priceString);

    if (matcher.find()) {
      try {
        String priceValue = matcher.group(1) + "." + matcher.group(2);
        String unit = matcher.group(3).trim();
        log.debug("Matched Biedronka format - Price: {}, Unit: {}", priceValue, unit);
        return new PriceUnit(new BigDecimal(priceValue), unit);
      } catch (NumberFormatException e) {
        log.error("Could not parse price using Biedronka format: {}", priceString, e);
      }
    }

    Pattern fallbackPattern = Pattern.compile("(\\d+)\\s+(\\d{2})");
    Matcher fallbackMatcher = fallbackPattern.matcher(priceString);
    if (fallbackMatcher.find()) {
      try {
        String priceValue = fallbackMatcher.group(1) + "." + fallbackMatcher.group(2);
        log.debug("Matched fallback format - Price: {}, Unit: N/A", priceValue);
        return new PriceUnit(new BigDecimal(priceValue), "N/A");
      } catch (NumberFormatException e) {
        log.error("Could not parse price using fallback format: {}", priceString, e);
      }
    }

    log.error("All patterns failed for price string: '{}'", priceString);
    return new PriceUnit(BigDecimal.ZERO, "N/A");
  }

  private String makeAbsoluteUrl(String url) {
    if (url != null && !url.startsWith("http")) {
      return BASE_URL + url;
    }
    return url;
  }

  /** Record to hold price and unit together. */
  private record PriceUnit(BigDecimal price, String unit) {}
}
