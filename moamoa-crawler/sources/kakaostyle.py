from __future__ import annotations

from _common import html_link_posts


def crawl(request, config) -> dict[str, object]:
    return html_link_posts(
        key="kakaostyle",
        blog="Kakao Style Dev Blog",
        base_url="https://devblog.kakaostyle.com/ko",
        list_urls=["https://devblog.kakaostyle.com/ko/", "https://devblog.kakaostyle.com/ko/page/2/"],
        include=r"^https://devblog\.kakaostyle\.com/ko/[^/?#]+/?$",
        exclude=r"/page/|/tags/|/authors/",
        request=request,
    )
