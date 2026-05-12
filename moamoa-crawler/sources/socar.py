from __future__ import annotations

from _common import html_link_posts


def crawl(request, config) -> dict[str, object]:
    return html_link_posts(
        key="socar",
        blog="Socar Tech Blog",
        base_url="https://tech.socarcorp.kr",
        list_urls=["https://tech.socarcorp.kr/posts/", "https://tech.socarcorp.kr/posts/page2/"],
        include=r"^https://tech\.socarcorp\.kr/[^/?#]+/?$|^https://tech\.socarcorp\.kr/posts/[^/?#]+/?$",
        exclude=r"/posts/page|/tags/|/categories/",
        request=request,
    )
