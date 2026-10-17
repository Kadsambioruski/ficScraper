import asyncio
from aiohttp import web
import nodriver as uc

browser = None
browser_lock = asyncio.Lock()


async def fetch_html(request):
    global browser

    url = request.query.get("url", "")

    # Only allow AO3 work and chapter URLs.
    from urllib.parse import urlparse
    parsed = urlparse(url)

    if (
        parsed.scheme != "https"
        or parsed.hostname != "archive.transformativeworks.org"
        or not parsed.path.startswith("/works/")
    ):
        return web.Response(status=400, text="Invalid AO3 URL")

    async with browser_lock:
        try:
            tab = await browser.get(url)
            await tab.sleep(3)

            html = await tab.get_content()
            return web.Response(text=html, content_type="text/html")

        except Exception as exc:
            return web.Response(status=502, text=str(exc))


async def main():
    global browser

    browser = await uc.start(
        headless=True,
        browser_executable_path="/usr/bin/chromium",
    )

    app = web.Application()
    app.router.add_get("/fetch", fetch_html)

    runner = web.AppRunner(app)
    await runner.setup()
    site = web.TCPSite(runner, "127.0.0.1", 8765)
    await site.start()

    print("AO3 browser service running at http://127.0.0.1:8765")

    try:
        await asyncio.Event().wait()
    finally:
        await runner.cleanup()
        browser.stop()


if __name__ == "__main__":
    asyncio.run(main())
