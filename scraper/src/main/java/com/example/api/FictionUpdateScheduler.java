package com.example.api;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.example.Config;
import com.example.FicScraper;
import com.example.model.Fiction;

@Component
public class FictionUpdateScheduler {
    private static final Logger log =
        LoggerFactory.getLogger(FictionUpdateScheduler.class);

    private final FicScraper ficScraper = new FicScraper();
    private final AtomicBoolean updateRunning = new AtomicBoolean(false);

    @Async
    @EventListener(ApplicationReadyEvent.class)
    public void initialUpdate() {
        log.info("Starting initial fiction update in background");
        updateFictions();
    }

    @Scheduled(fixedDelay = 3 * 60 * 60 * 1000)
    public void updateFictions() {
        if (!updateRunning.compareAndSet(false, true)) {
            log.info("Fiction update already running. Skipping this run.");
            return;
        }

        try {
            log.info("Starting fiction update");

            List<Fiction> fictions =
                Config.fictionStorage().getAll();

            for (Fiction fiction : fictions) {
                try {
                    ficScraper.updateFiction(fiction);
                } catch (Exception e) {
                    log.error(
                        "Error updating fiction: {}",
                        fiction.getTitle(),
                        e
                    );
                }
            }

            log.info("Fiction update finished");
        } finally {
            updateRunning.set(false);
        }
    }
}