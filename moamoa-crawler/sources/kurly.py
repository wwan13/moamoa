from __future__ import annotations

from _common import html_link_posts


def crawl(request, config) -> dict[str, object]:
    return html_link_posts(
        key="kurly",
        blog="Kurly Tech Blog",
        base_url="https://helloworld.kurly.com",
        list_urls=["https://helloworld.kurly.com", "https://helloworld.kurly.com/page/2/"],
        include=r"^https://helloworld\.kurly\.com/blog/[^/?#]+/?$",
        exclude=r"/page/|/tags/|/categories/|/about",
        request=request,
    )
