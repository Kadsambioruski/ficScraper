
package com.example.scrape;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

public final class AO3BrowserFetch {

    private static final HttpClient CLIENT = HttpClient.newHttpClient();

    private AO3BrowserFetch() {
    }

    public static Document fetch(String url) throws IOException {
        String encodedUrl = URLEncoder.encode(
            url,
            StandardCharsets.UTF_8
        );

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(
                "http://127.0.0.1:8765/fetch?url=" + encodedUrl
            ))
            .GET()
            .build();

        try {
            HttpResponse<String> response = CLIENT.send(
                request,
                HttpResponse.BodyHandlers.ofString()
            );

            if (response.statusCode() != 200) {
                throw new IOException(
                    "Nodriver service returned HTTP "
                    + response.statusCode()
                    + ": "
                    + response.body()
                );
            }

            return Jsoup.parse(response.body(), url);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException(
                "Interrupted while fetching AO3 page",
                e
            );
        }
    }
}
