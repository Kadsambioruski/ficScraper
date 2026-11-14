# ficScraper
[![License]\([https://img.shields.io/badge/license-PolyForm_NC-orange?style=flat-square)\](LICENSE](https://img.shields.io/badge/license-PolyForm_NC-orange?style=flat-square\)]\(LICENSE))

A web fiction reader and tracker built with Java, Spring Boot, Flutter, and PostgreSQL. It scrapes supported fiction websites, stores chapters locally, tracks reading progress, and provides an interface for reading and managing your fiction library.

## Features

- Scrape fiction from supported websites
- Automatically check for new chapters
- Store fiction and chapter data in PostgreSQL database
- Read chapters and track reading progress through Flutter GUI
- Manage unfinished and finished fictions

## Supported Sites

| Site | Domain(s) | Notes |
|------|-----------|-------|
| Royal Road | `royalroad.com` | Fiction metadata, chapter lists, and chapter content |
| FanFiction.net | `fanfiction.net` | May require a `cf_clearance` cookie to bypass Cloudflare challenges |
| Archive of Our Own | `archiveofourown.org`, `archive.transformativeworks.org` | Uses Python `nodriver` to fetch page HTML |

## Prerequisites

- Java version required by the Maven project
- Apache Maven
- PostgreSQL
- Flutter SDK
- Python 3
- Bash
- Python dependencies required by the AO3 scraper, including `nodriver`
- Browser dependencies required by `nodriver`

## Setup

### 1. Configure PostgreSQL

Create a PostgreSQL database and user:

- Database: `fiction_db`
- Username: `fiction_app`
- JDBC URL: `jdbc:postgresql://localhost:5432/fiction_db`

Configure the database credentials in `scraper/.env`:

```env
DB_URL=jdbc:postgresql://localhost:5432/fiction_db
DB_USERNAME=your_database_username
DB_PASSWORD=your_database_password
FF_COOKIE=your_cf_clearance_cookie
```
### 2. Run Bash Script
(Make sure to navigate to project root before running)
```bash
bash ./start.sh
```