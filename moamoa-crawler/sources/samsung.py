from __future__ import annotations

from _common import html_link_posts


def crawl(request, config) -> dict[str, object]:
    return html_link_posts(
        key="samsung",
        blog="Samsung Tech Blog",
        base_url="https://techblog.samsung.com",
        list_urls=["https://techblog.samsung.com/?page=1&"],
        include=r"^https://techblog\.samsung\.com/blog/article/[^/?#]+/?$",
        request=request,
    )
