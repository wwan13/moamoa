from __future__ import annotations

from _common import medium_browser_posts


def crawl(request, config) -> dict[str, object]:
    return medium_browser_posts(
        key="kream",
        blog="KREAM Tech Blog",
        base_url="https://medium.com/kream-%EA%B8%B0%EC%88%A0-%EB%B8%94%EB%A1%9C%EA%B7%B8",
        request=request,
        config=config,
        post_path_prefix="/kream-기술-블로그/",
    )
