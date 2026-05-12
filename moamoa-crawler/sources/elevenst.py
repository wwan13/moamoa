from __future__ import annotations

from _common import html_link_posts


def crawl(request, config) -> dict[str, object]:
    return html_link_posts(
        key="elevenst",
        blog="11st Tech Blog",
        base_url="https://11st-tech.github.io",
        list_urls=["https://11st-tech.github.io", "https://11st-tech.github.io/page/2/"],
        include=r"^https://11st-tech\.github\.io/[^/?#]+/?$",
        exclude=r"/tags/|/categories/|/about|/page/",
        request=request,
    )
