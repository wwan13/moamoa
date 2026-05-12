from __future__ import annotations

from _common import medium_browser_posts


def crawl(request, config) -> dict[str, object]:
    return medium_browser_posts(
        key="watcha",
        blog="WATCHA Tech Blog",
        base_url="https://medium.com/watcha",
        request=request,
        config=config,
        post_path_prefix="/watcha/",
    )
