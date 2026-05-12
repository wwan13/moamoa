from __future__ import annotations

from _common import medium_browser_posts


def crawl(request, config) -> dict[str, object]:
    return medium_browser_posts(
        key="delightroom",
        blog="DelightRoom",
        base_url="https://medium.com/delightroom",
        request=request,
        config=config,
        post_path_prefix="/delightroom/",
    )
