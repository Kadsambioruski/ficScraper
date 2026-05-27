package com.example.scrape;

public enum Site {
    ROYAL_ROAD("royalroad.com"),
    FANFICTION("fanfiction.net");
    
    final String domain;

    private Site(String domain) {
        this.domain = domain;
    }
    
    public static Site fromUrl(String url) {
        for (Site site : values()) {
            if (url.contains(site.domain)) return site;
        }
        throw new IllegalArgumentException("Unknown site: " + url);
    }
}
