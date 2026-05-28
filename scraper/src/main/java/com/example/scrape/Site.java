package com.example.scrape;

public enum Site {
    ROYAL_ROAD("royalroad.com"),
    FANFICTION("fanfiction.net"),
    AO3("archiveofourown.org|transformativeworks.org");
    
    final String domain;

    private Site(String domain) {
        this.domain = domain;
    }
    
    public static Site fromUrl(String url) {
        for (Site site : values()) {
            for (String domain : site.domain.split("\\|")) {
                if (url.contains(domain)) return site;
            }
        }
        throw new IllegalArgumentException("Unknown site: " + url);
    }
}
