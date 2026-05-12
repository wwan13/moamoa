from __future__ import annotations

from _common import medium_browser_posts


def crawl(request, config) -> dict[str, object]:
    return medium_browser_posts(
        key="daangn",
        blog="Daangn Tech Blog",
        base_url="https://medium.com/daangn",
        request=request,
        config=config,
        post_path_prefix="/daangn/",
    )
