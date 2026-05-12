from __future__ import annotations

from _common import medium_browser_posts


def crawl(request, config) -> dict[str, object]:
    return medium_browser_posts(
        key="gccompany",
        blog="GC Company Tech Blog",
        base_url="https://techblog.gccompany.co.kr",
        request=request,
        config=config,
    )
