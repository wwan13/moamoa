from __future__ import annotations

from _common import html_link_posts


def crawl(request, config) -> dict[str, object]:
    return html_link_posts(
        key="wanted",
        blog="Wanted Tech Blog",
        base_url="https://social.wanted.co.kr/community/team/171",
        list_urls=["https://social.wanted.co.kr/community/team/171"],
        include=r"^https://social\.wanted\.co\.kr/community/article/[^/?#]+/?$",
        request=request,
    )
