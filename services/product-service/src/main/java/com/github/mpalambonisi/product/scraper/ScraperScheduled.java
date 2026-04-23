package com.github.mpalambonisi.product.scraper;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Scheduled tasks for running the scraper. */
@Component
public class ScraperScheduled {
  private static final Logger LOG = LoggerFactory.getLogger(ScraperScheduled.class);
  private final ScraperService scraperService;

  // Rate limiting to prevent excessive scraping
  private Instant lastScrapedTime;
  private static final Duration MIN_SCRAPE_INTERVAL = Duration.ofHours(6);

  // Configuration property to control startup scraping
  @Value("${scraper.run-on-startup:true}")
  private boolean runOnStartup;

  // Define categories to scrape here
  private static final Map<String, String> CATEGORIES_TO_SCRAPE = new LinkedHashMap<>();

  static {
    // Category names must match exactly with names in the database
    CATEGORIES_TO_SCRAPE.put("Warzywa", "/warzywa");
    CATEGORIES_TO_SCRAPE.put("Owoce", "/owoce");
    CATEGORIES_TO_SCRAPE.put("Piekarnia", "/piekarnia");
    CATEGORIES_TO_SCRAPE.put("Nabiał", "/nabial");
    CATEGORIES_TO_SCRAPE.put("Mięso", "/mieso");
    CATEGORIES_TO_SCRAPE.put("Dania Gotowe", "/dania-gotowe");
    CATEGORIES_TO_SCRAPE.put("Napoje", "/napoje-2");
    CATEGORIES_TO_SCRAPE.put("Mrożone", "/mrozone");
    CATEGORIES_TO_SCRAPE.put("Artykuły spożywcze", "/artykuly-spozywcze");
    CATEGORIES_TO_SCRAPE.put("Drogeria", "/drogeria");
    CATEGORIES_TO_SCRAPE.put("Dla domu", "/dla-domu");
    CATEGORIES_TO_SCRAPE.put("Dla dzieci", "/dla-dzieci-");
    CATEGORIES_TO_SCRAPE.put("Dla zwierząt", "/dla-zwierzat");
  }

  public ScraperScheduled(ScraperService scraperService) {
    this.scraperService = scraperService;
  }

  /**
   * Runs the scraper when the application starts up. Uses @EventListener with ApplicationReadyEvent
   * to ensure all beans are initialized. @Async makes it non-blocking so the application can start
   * without waiting for scraping to complete.
   */
  @Async
  @EventListener(ApplicationReadyEvent.class)
  public void runScraperOnStartup() {
    if (runOnStartup) {
      LOG.info("Application started - Running initial product scrape...");
      runScrape();
    } else {
      LOG.info("Startup scraping is disabled. Set scraper.run-on-startup=true to enable.");
    }
  }

  /** Runs the scraper manually (for testing/admin use only). */
  public void runScraper() {
    LOG.info("Running scraper immediately...");
    runScrape();
  }

  /**
   * Runs the scraper at midnight (00:00) every day in Warsaw timezone. Cron expression: "0 0 0 * *
   * *" means: - 0 seconds - 0 minutes - 0 hours (midnight) - every day of month - every month -
   * every day of week
   */
  @Scheduled(cron = "0 0 0 * * *", zone = "Europe/Warsaw")
  public void runDailyScrape() {
    LOG.info("Starting scheduled daily scrape at midnight (Europe/Warsaw timezone)...");
    runScrape();
  }

  /**
   * Checks if enough time has passed since the last scrape to prevent excessive scraping during
   * frequent redeployments.
   *
   * @return true if scraping should proceed, false if rate limit is active
   */
  private synchronized boolean shouldScrape() {
    if (lastScrapedTime == null) {
      return true;
    }
    Duration timeSinceLastScrape = Duration.between(lastScrapedTime, Instant.now());
    if (timeSinceLastScrape.compareTo(MIN_SCRAPE_INTERVAL) < 0) {
      long hoursRemaining = MIN_SCRAPE_INTERVAL.minus(timeSinceLastScrape).toHours();
      LOG.info(
          "Rate limit active: Last scrape was {} hours ago. "
              + "Minimum interval is {} hours. Skipping scrape. "
              + "Next scrape available in ~{} hours.",
          timeSinceLastScrape.toHours(),
          MIN_SCRAPE_INTERVAL.toHours(),
          hoursRemaining);
      return false;
    }
    return true;
  }

  /** Core scraping logic that can be called from multiple triggers. */
  private void runScrape() {
    if (!shouldScrape()) {
      return;
    }

    Instant startTime = Instant.now();
    int totalNew = 0;
    int totalUpdated = 0;
    int totalFailed = 0;

    try {
      LOG.info("Starting product scrape from Biedronka...");

      for (Map.Entry<String, String> entry : CATEGORIES_TO_SCRAPE.entrySet()) {
        String categoryName = entry.getKey();
        String categoryUrl = entry.getValue();

        LOG.info("Scraping category: {} ({})", categoryName, categoryUrl);

        try {
          ScraperService.ScrapeSummary summary =
              scraperService.scrapeCategory(categoryName, categoryUrl);

          totalNew += summary.newProducts();
          totalUpdated += summary.updatedProducts();
          totalFailed += summary.failedProducts();

          LOG.info(
              "Category '{}' done. New: {}, Updated: {}, Failed: {}",
              categoryName,
              summary.newProducts(),
              summary.updatedProducts(),
              summary.failedProducts());

        } catch (Exception e) {
          // Category-level failure — log and continue to next category
          LOG.error("Category '{}' scrape failed entirely: {}", categoryName, e.getMessage());
        }
      }

      long durationSeconds = Duration.between(startTime, Instant.now()).toSeconds();
      LOG.info(
          "Scrape complete. New: {}, Updated: {}, Failed: {}, Duration: {}s",
          totalNew,
          totalUpdated,
          totalFailed,
          durationSeconds);

      lastScrapedTime = Instant.now();

    } catch (Exception e) {
      long durationSeconds = Duration.between(startTime, Instant.now()).toSeconds();
      LOG.error("Scrape run failed after {}s", durationSeconds, e);
    }
  }
}
