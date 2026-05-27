package com.example.scrape;

import java.util.Map;

public class ScraperFactory {
    private static final Map<Site, SiteScraper> scrapers = Map.of(
        Site.ROYAL_ROAD, new RoyalRoadScraper(),
        Site.FANFICTION, new FanfictionScraper()
    );

    public static SiteScraper forFic(Site site) {
        SiteScraper scraper = scrapers.get(site);
        if (scraper == null) {
            throw new IllegalArgumentException("No scraper for this site as of yet: " + site);
        }
        return scraper;
    }
}
