from __future__ import annotations

from _common import html_link_posts


def crawl(request, config) -> dict[str, object]:
    return html_link_posts(
        key="saramin",
        blog="Saramin Tech Blog",
        base_url="https://saramin.github.io",
        list_urls=["https://saramin.github.io/", "https://saramin.github.io/page2/"],
        include=r"^https://saramin\.github\.io/\d{4}-\d{2}-\d{2}-[^/?#]+/?$",
        request=request,
    )
