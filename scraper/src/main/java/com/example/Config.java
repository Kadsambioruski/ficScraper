package com.example;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import com.example.storage.FicJsonHandler;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Config {
    private static final String DATA_DIR = "data";
    private static final String FICS_JSON_PATH = "fics.json";
    private static final String FINISHED_FICS_JSON_PATH = "finishedFics.json";
    private static final ObjectMapper instance = new ObjectMapper();
    private static final Connection SESSION = Jsoup.newSession().timeout(30000).userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
    private static final FicJsonHandler ficJsonHandler = new FicJsonHandler();
    public static FicJsonHandler ficJsonHandler() { return ficJsonHandler; }
    
    public static Document fetch(String url) throws IOException {
        return SESSION.url(url).get();
    }

    public static ObjectMapper objectMapper(){
        return instance;
    }

    public static Path ficsJsonPath(){
        return Paths.get(DATA_DIR, FICS_JSON_PATH);
    }

    public static Path finishedFicsJsonPath(){
        return Paths.get(DATA_DIR, FINISHED_FICS_JSON_PATH);
    }



}
