from __future__ import annotations

from _common import html_link_posts


def crawl(request, config) -> dict[str, object]:
    return html_link_posts(
        key="samosam",
        blog="3o3 Tech Blog",
        base_url="https://blog.3o3.co.kr/tag/tech",
        list_urls=["https://blog.3o3.co.kr/tag/tech/", "https://blog.3o3.co.kr/tag/tech/page/2/"],
        include=r"^https://blog\.3o3\.co\.kr/[^/?#]+/?$",
        exclude=r"/tag/|/page/|/author/",
        request=request,
    )
