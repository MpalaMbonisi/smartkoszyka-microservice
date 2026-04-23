package com.github.mpalambonisi.product.scraper;

import com.github.mpalambonisi.product.model.Category;
import com.github.mpalambonisi.product.model.Product;
import com.github.mpalambonisi.product.repository.CategoryRepository;
import com.github.mpalambonisi.product.repository.ProductRepository;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.math.BigDecimal;
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

  private static final Logger LOG = LoggerFactory.getLogger(ScraperScheduled.class);

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
  public ScrapeSummary scrapeCategory(String categoryName, String categoryUrl) {
    int newCount = 0;
    int updatedCount = 0;
    int failedCount = 0;

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
      LOG.error("Failed to get total item count for {}: {}", categoryName, e.getMessage());
      return new ScrapeSummary(newCount, updatedCount, failedCount);
    }

    if (totalItems == 0) {
      LOG.warn("No items found or count failed for {}", categoryName);
      return new ScrapeSummary(newCount, updatedCount, failedCount);
    }

    int totalPages = (int) Math.ceil((double) totalItems / ITEMS_PER_PAGE);
    LOG.info(
        "Scraping '{}'. Found {} items across {} pages...", categoryName, totalItems, totalPages);

    for (int pageNumber = 1; pageNumber <= totalPages; pageNumber++) {
      String urlToScrape =
          BASE_URL + categoryUrl + "/?format=ajax&onlyRefinements=false&page=" + pageNumber;

      try {
        Document doc =
            Jsoup.connect(urlToScrape)
                .header("X-Requested-With", "XMLHttpRequest")
                .userAgent(USER_AGENT)
                .timeout(15000)
                .get();

        Elements productTiles = doc.select(SELECTOR_PRODUCT_TILE);
        if (productTiles.isEmpty()) {
          LOG.warn("Page {} was empty, stopping early.", pageNumber);
          break;
        }

        for (Element tile : productTiles) {
          try {
            String name = tile.select(SELECTOR_NAME).text();
            String priceText = tile.select(SELECTOR_PRICE).text();
            String imageUrl = tile.select(SELECTOR_IMAGE).attr("content");
            String brand = parseBrandFromGtmData(tile, name);
            PriceUnit priceUnit = parsePriceAndUnit(priceText);

            Optional<Product> existing =
                productRepository.findByNameAndUnitAndCategoryId(
                    name, priceUnit.unit(), category.getId());

            if (existing.isPresent()) {
              // Update — managed entity, dirty checking will flush automatically
              Product product = existing.get();
              product.setPrice(priceUnit.price());
              product.setImageUrl(makeAbsoluteUrl(imageUrl));
              product.setBrand(brand);
              updatedCount++;
              LOG.debug("Updated: {} ({})", name, priceUnit.unit());
            } else {
              // Insert — new transient entity
              Product product =
                  new Product(
                      name,
                      priceUnit.price(),
                      priceUnit.unit(),
                      makeAbsoluteUrl(imageUrl),
                      brand,
                      category);
              productRepository.save(product);
              newCount++;
              LOG.debug("Inserted: {} ({})", name, priceUnit.unit());
            }
          } catch (Exception e) {
            // Log and continue — one bad product should not stop the whole category
            failedCount++;
            LOG.error("Failed to process product tile on page {}: {}", pageNumber, e.getMessage());
          }
        }

        Thread.sleep(500);

      } catch (IOException | InterruptedException e) {
        LOG.error("Error scraping page {}: {}", pageNumber, e.getMessage());
        Thread.currentThread().interrupt();
        break;
      }
    }

    LOG.info(
        "Finished '{}'. New: {}, Updated: {}, Failed: {}",
        categoryName,
        newCount,
        updatedCount,
        failedCount);

    return new ScrapeSummary(newCount, updatedCount, failedCount);
  }

  /** Simple value object to carry scrape results back to the scheduler. */
  public record ScrapeSummary(int newProducts, int updatedProducts, int failedProducts) {
    public int total() {
      return newProducts + updatedProducts;
    }
  }

  private int getTotalItemCount(String categoryUrl) throws IOException {
    String url = BASE_URL + categoryUrl;
    LOG.debug("Finding total item count from: {}", url);
    Document doc = Jsoup.connect(url).userAgent(USER_AGENT).timeout(10000).get();
    Element countElement = doc.selectFirst(SELECTOR_TOTAL_COUNT);
    if (countElement == null) {
      LOG.warn("Could not find total count selector! Assuming 0 items.");
      return 0;
    }
    String countText = countElement.text().split(" ")[0];
    try {
      return Integer.parseInt(countText);
    } catch (NumberFormatException e) {
      LOG.error("Could not parse total count: {}", countText);
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
      LOG.error("Error parsing GTM brand for {}: {}", productName, e.getMessage());
    }
    return "N/A";
  }

  /**
   * Parses price and unit from Biedronka format like "12 99 /kg" or "13 99 /szt." Returns a record
   * containing both price and unit.
   */
  private PriceUnit parsePriceAndUnit(String priceString) {
    if (priceString == null || priceString.isBlank()) {
      LOG.warn("Price string is null or blank");
      return new PriceUnit(BigDecimal.ZERO, "N/A");
    }

    LOG.debug("Raw price string: '{}'", priceString);

    Pattern pattern = Pattern.compile("(\\d+)\\s+(\\d{2})\\s*/([a-zżźćńółęąśA-ZŻŹĆŃÓŁĘĄŚ.]+)");
    Matcher matcher = pattern.matcher(priceString);

    if (matcher.find()) {
      try {
        String priceValue = matcher.group(1) + "." + matcher.group(2);
        String unit = matcher.group(3).trim();
        LOG.debug("Matched Biedronka format - Price: {}, Unit: {}", priceValue, unit);
        return new PriceUnit(new BigDecimal(priceValue), unit);
      } catch (NumberFormatException e) {
        LOG.error("Could not parse price using Biedronka format: {}", priceString, e);
      }
    }

    Pattern fallbackPattern = Pattern.compile("(\\d+)\\s+(\\d{2})");
    Matcher fallbackMatcher = fallbackPattern.matcher(priceString);
    if (fallbackMatcher.find()) {
      try {
        String priceValue = fallbackMatcher.group(1) + "." + fallbackMatcher.group(2);
        LOG.debug("Matched fallback format - Price: {}, Unit: N/A", priceValue);
        return new PriceUnit(new BigDecimal(priceValue), "N/A");
      } catch (NumberFormatException e) {
        LOG.error("Could not parse price using fallback format: {}", priceString, e);
      }
    }

    LOG.error("All patterns failed for price string: '{}'", priceString);
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
