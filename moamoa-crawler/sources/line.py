from __future__ import annotations

from _common import html_link_posts


def crawl(request, config) -> dict[str, object]:
    return html_link_posts(
        key="line",
        blog="LINE ENGINEERING",
        base_url="https://engineering.linecorp.com",
        list_urls=["https://engineering.linecorp.com/ko/blog"],
        include=r"^https://engineering\.linecorp\.com/ko/blog/[^/?#]+/?$",
        exclude=r"/author/|/tag/",
        request=request,
    )
