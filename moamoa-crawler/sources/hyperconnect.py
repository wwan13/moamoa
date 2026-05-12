from __future__ import annotations

from _common import html_link_posts


def crawl(request, config) -> dict[str, object]:
    return html_link_posts(
        key="hyperconnect",
        blog="Hyperconnect Tech Blog",
        base_url="https://hyperconnect.github.io",
        list_urls=["https://hyperconnect.github.io"],
        include=r"^https://hyperconnect\.github\.io/\d{4}/\d{2}/\d{2}/[^/?#]+/?$",
        exclude=r"/tag/|/tags/|/category/|/categories/|/about|/archive|\.xml$|/page/",
        request=request,
    )
