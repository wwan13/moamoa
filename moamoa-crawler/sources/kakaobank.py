from __future__ import annotations

from _common import html_link_posts


def crawl(request, config) -> dict[str, object]:
    return html_link_posts(
        key="kakaobank",
        blog="KakaoBank Tech Blog",
        base_url="https://tech.kakaobank.com",
        list_urls=["https://tech.kakaobank.com", "https://tech.kakaobank.com/page/2/"],
        include=r"^https://tech\.kakaobank\.com/posts/[^/?#]+/?$|^https://tech\.kakaobank\.com/[^/?#]+/?$",
        exclude=r"/page/|/tags/|/categories/",
        request=request,
    )
