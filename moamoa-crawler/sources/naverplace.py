from __future__ import annotations

from _common import medium_browser_posts


def crawl(request, config) -> dict[str, object]:
    return medium_browser_posts(
        key="naverplace",
        blog="NAVER Place Dev Blog",
        base_url="https://medium.com/naver-place-dev",
        request=request,
        config=config,
        post_path_prefix="/naver-place-dev/",
    )
