from __future__ import annotations

from _common import html_link_posts


def crawl(request, config) -> dict[str, object]:
    return html_link_posts(
        key="banksalad",
        blog="Banksalad Tech Blog",
        base_url="https://blog.banksalad.com",
        list_urls=["https://blog.banksalad.com/tech/page/1/", "https://blog.banksalad.com/tech/"],
        include=r"^https://blog\.banksalad\.com/tech/[^/?#]+/?$",
        request=request,
    )
