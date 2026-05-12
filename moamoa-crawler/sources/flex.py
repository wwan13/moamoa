from __future__ import annotations

from _common import html_link_posts


def crawl(request, config) -> dict[str, object]:
    return html_link_posts(
        key="flex",
        blog="flex Tech Blog",
        base_url="https://flex.team/blog/category/flexteam",
        list_urls=["https://flex.team/blog/category/flexteam"],
        include=r"^https://flex\.team/blog/\d{4}/\d{2}/\d{2}/[^/?#]+/?$",
        exclude=r"/blog/category/|/blog/tag/|/blog/search|/blog/$",
        request=request,
    )
