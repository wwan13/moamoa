from __future__ import annotations

from _common import html_link_posts


def crawl(request, config) -> dict[str, object]:
    return html_link_posts(
        key="ktcloud",
        blog="kt cloud Tech Blog",
        base_url="https://tech.ktcloud.com",
        list_urls=["https://tech.ktcloud.com"],
        include=r"^https://tech\.ktcloud\.com/entry/[^?#]+/?$",
        request=request,
    )
