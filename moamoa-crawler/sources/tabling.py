from __future__ import annotations

from _common import medium_browser_posts


def crawl(request, config) -> dict[str, object]:
    return medium_browser_posts(
        key="tabling",
        blog="Tabling Tech Blog",
        base_url="https://techblog.tabling.co.kr",
        request=request,
        config=config,
    )
