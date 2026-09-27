package com.example;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

public class ChapterHtmlCleaner {

    public static String clean(String html) {
        Document document = Jsoup.parseBodyFragment(html);

        document.select("script, noscript, iframe").remove();

        return document.body().html();
    }
}