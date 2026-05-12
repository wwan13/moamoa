from __future__ import annotations

from _common import medium_browser_posts


def crawl(request, config) -> dict[str, object]:
    return medium_browser_posts(
        key="yogiyo",
        blog="YOGIYO Tech Blog",
        base_url="https://techblog.yogiyo.co.kr",
        request=request,
        config=config,
    )
