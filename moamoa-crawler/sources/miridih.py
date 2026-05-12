from __future__ import annotations

from _common import medium_browser_posts


def crawl(request, config) -> dict[str, object]:
    return medium_browser_posts(
        key="miridih",
        blog="Miridih Blog",
        base_url="https://medium.com/miridih",
        request=request,
        config=config,
        post_path_prefix="/miridih/",
    )
