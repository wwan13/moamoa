from __future__ import annotations

from _common import html_link_posts


def crawl(request, config) -> dict[str, object]:
    return html_link_posts(
        key="kakaopay",
        blog="KakaoPay Tech Blog",
        base_url="https://tech.kakaopay.com",
        list_urls=["https://tech.kakaopay.com", "https://tech.kakaopay.com/page/2/"],
        include=r"^https://tech\.kakaopay\.com/post/[^/?#]+/?$",
        request=request,
    )
