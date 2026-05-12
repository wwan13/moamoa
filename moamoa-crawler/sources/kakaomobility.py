from __future__ import annotations

from _common import html_link_posts


def crawl(request, config) -> dict[str, object]:
    return html_link_posts(
        key="kakaomobility",
        blog="Kakao Mobility Developers",
        base_url="https://developers.kakaomobility.com",
        list_urls=["https://developers.kakaomobility.com/techblogs/"],
        include=r"^https://developers\.kakaomobility\.com/techblogs/[^/?#]+/?$",
        request=request,
    )
