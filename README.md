# ficScraper
[![License](https://img.shields.io/badge/license-PolyForm_NC-orange?style=flat-square)](LICENSE)

A Java Discord bot that scrapes web fiction and fanfiction sites to track your reading progress. It periodically checks for new chapters on tracked fictions and notifies you via Discord slash commands.

## Features

- **Multi-site scraping** — Supports Royal Road, FanFiction.net, and Archive of Our Own
- **Automatic chapter checking** — A background loop checks all tracked fictions every 3 hours and sends notifications when new chapters are found
- **Stub detection** — Detects when a fiction has been stubbed (removed for publishing) and updates the chapter count accordingly
- **Paginated Discord menus** — Browse and select fictions and chapters with interactive paginated select menus and navigation buttons
- **Finished-fic tracking** — Mark fictions as finished to move them to a separate list
- **Reading statistics** — View total words read from completed fictions

## Supported Sites

| Site | Domain(s) | Notes |
|------|-----------|-------|
| Royal Road | `royalroad.com` | Full support |
| FanFiction.net | `fanfiction.net` | Requires a `cf_clearance` cookie for CloudFlare bypass |
| Archive of Our Own | `archiveofourown.org`, `transformativeworks.org` | Full support |

## Prerequisites

- Java 25+
- Apache Maven

## Setup

1. Clone the repository
2. Create `scraper/.env` in the `scraper/` directory:

```
DISCORD_BOT_TOKEN=your_bot_token
DISCORD_SERVER_ID=your_guild_id
DISCORD_CHANNEL_ID=your_channel_id
FF_COOKIE=your_cf_clearance_cookie (optional, for fanfiction.net)
```

   - `DISCORD_BOT_TOKEN`, `DISCORD_SERVER_ID`, `DISCORD_CHANNEL_ID` are required
   - `FF_COOKIE` is optional but needed if FanFiction.net returns CloudFlare challenges — set it to the value of the `cf_clearance` cookie from your browser

3. The `data/` directory and JSON files (`fics.json`, `finishedFics.json`) are created automatically on first run

## Building

```bash
mvn clean package
```

This produces `scraper/target/ficScraper.jar`.

## Running

```bash
java -jar scraper/target/ficScraper.jar
```

For low-memory environments (e.g., Raspberry Pi):

```bash
java -Xmx128m -Xss256k -XX:+UseSerialGC \
    -Dio.netty.allocator.maxOrder=3 \
    -jar scraper/target/ficScraper.jar
```

## Discord Slash Commands

| Command | Description |
|---------|-------------|
| `/read` | Shows a list of fictions that have new chapters. Select one, then select the chapter you've read — the bot updates your progress and clears the notification |
| `/add <ficlink>` | Adds a new fiction by its URL. The bot scrapes the link and saves it to your reading list |
| `/finish` | Shows all tracked fictions. Select one to mark it as finished — it moves to the finished list with its final word count |
| `/startloop` | Starts the background scraping loop that checks for new chapters every 3 hours |
| `/endloop` | Stops the scraping loop |
| `/wordcount` | Displays the total word count summed from all finished fictions |

## How the Scrape Loop Works

1. The loop runs on a background thread started via `/startloop`
2. Every 3 hours (on the hour), it iterates all fictions in `fics.json`
3. For each fiction, it fetches the current chapter list from the source site
4. **Stub check:** If the scraped chapter count is *less* than the stored count, the fiction was likely stubbed — the bot updates the stored count and sends an alert
5. **Update check:** If the scraped count is *greater* than the stored count, a new chapter is available — the bot sends a notification (with a link) and adds the fiction to the "updated" list
6. Duplicate notifications for the same chapter are prevented by tracking the last-seen chapter count per fiction session
7. Use `/read` to select a fiction from the updated list and mark which chapter you read
