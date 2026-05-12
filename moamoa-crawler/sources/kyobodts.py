from __future__ import annotations

from _common import html_link_posts


def crawl(request, config) -> dict[str, object]:
    return html_link_posts(
        key="kyobodts",
        blog="Kyobo DTS Tech Blog",
        base_url="https://blog.kyobodts.co.kr",
        list_urls=["https://blog.kyobodts.co.kr"],
        include=r"^https?://blog\.kyobodts\.co\.kr/\d{4}/\d{2}/\d{2}/[^?#]+/?$",
        request=request,
    )
