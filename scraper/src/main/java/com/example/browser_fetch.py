
import asyncio
from urllib.parse import urlparse

from aiohttp import web
import nodriver as uc


browser = None
browser_lock = asyncio.Lock()

ALLOWED_HOSTS = {
    "archive.transformativeworks.org",
    "archiveofourown.org",
    "royalroad.com",
    "www.royalroad.com",
    "fanfiction.net",
    "www.fanfiction.net",
}


def is_allowed_url(url):
    parsed = urlparse(url)

    if parsed.scheme != "https":
        return False

    if parsed.hostname not in ALLOWED_HOSTS:
        return False

    if parsed.hostname in {
        "archive.transformativeworks.org",
        "archiveofourown.org",
    }:
        return parsed.path.startswith("/works/")

    if parsed.hostname in {
        "royalroad.com",
        "www.royalroad.com",
    }:
        return parsed.path.startswith("/fiction/")

    if parsed.hostname in {
        "fanfiction.net",
        "www.fanfiction.net",
    }:
        return parsed.path.startswith("/s/")

    return False


async def fetch_html(request):
    global browser

    url = request.query.get("url", "")

    if not is_allowed_url(url):
        return web.Response(
            status=400,
            text="Invalid or unsupported URL",
        )

    async with browser_lock:
        try:
            tab = await browser.get(url)

            await tab.sleep(5)

            html = await tab.get_content()

            lower_html = html.lower()

            if (
                "making sure you're not a bot" in lower_html
                or "id=\"anubis_challenge\"" in lower_html
            ):
                return web.Response(
                    status=403,
                    text="Bot-protection challenge returned",
                )

            return web.Response(
                text=html,
                content_type="text/html",
            )

        except Exception as exc:
            return web.Response(
                status=502,
                text=str(exc),
            )


async def main():
    global browser

    browser = await uc.start(
        headless=False,
        browser_executable_path="/usr/bin/chromium",
    )

    app = web.Application()
    app.router.add_get("/fetch", fetch_html)

    runner = web.AppRunner(app)
    await runner.setup()

    site = web.TCPSite(
        runner,
        "127.0.0.1",
        8765,
    )
    await site.start()

    print(
        "Browser fetch service running at "
        "http://127.0.0.1:8765"
    )

    try:
        await asyncio.Event().wait()
    finally:
        await runner.cleanup()
        browser.stop()


if __name__ == "__main__":
    asyncio.run(main())
