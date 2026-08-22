package com.example;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import com.example.storage.FicJsonHandler;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.cdimascio.dotenv.Dotenv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Config {
    private static final Logger log = LoggerFactory.getLogger(Config.class);
    private static final String DATA_DIR = "data";
    private static final String FICS_JSON_PATH = "fics.json";
    private static final String FINISHED_FICS_JSON_PATH = "finishedFics.json";
    private static final ObjectMapper instance = new ObjectMapper();
    private static final Dotenv dotEnv = Dotenv.configure().directory("scraper").load();
    private static final String FF_COOKIE = dotEnv.get("FF_COOKIE");
    private static final String AO3_COOKIES = dotEnv.get("AO3_COOKIES");
    private static final Connection SESSION;
    static {
        Connection session = Jsoup.newSession()
            .timeout(30000)
            .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            .header("Accept-Language", "en-US,en;q=0.5")
            .header("Accept-Encoding", "gzip, deflate")
            .header("Connection", "keep-alive")
            .header("Upgrade-Insecure-Requests", "1");
        if (FF_COOKIE != null && !FF_COOKIE.isEmpty()) {
            session.cookie("cf_clearance", FF_COOKIE);
            log.info("Loaded cf_clearance cookie");
        }
        if (AO3_COOKIES != null && !AO3_COOKIES.isEmpty()) {
            for (String cookie : AO3_COOKIES.split(";\\s*")) {
                String[] parts = cookie.split("=", 2);
                if (parts.length == 2) {
                    session.cookie(parts[0].trim(), parts[1].trim());
                }
            }
            log.info("Loaded AO3 cookies");
        }
        SESSION = session;
    }

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
