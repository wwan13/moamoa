from __future__ import annotations

from _common import html_link_posts


def crawl(request, config) -> dict[str, object]:
    return html_link_posts(
        key="skplanet",
        blog="SK planet Tech Topic",
        base_url="https://techtopic.skplanet.com",
        list_urls=["https://techtopic.skplanet.com"],
        include=r"^https://techtopic\.skplanet\.com/[^/?#]+/?$",
        exclude=r"/tag/|/category/|/author/|/page/",
        request=request,
    )
