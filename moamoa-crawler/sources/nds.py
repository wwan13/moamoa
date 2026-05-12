from __future__ import annotations

from _common import html_link_posts


def crawl(request, config) -> dict[str, object]:
    return html_link_posts(
        key="nds",
        blog="NDS Tech Blog",
        base_url="https://tech.cloud.nongshim.co.kr",
        list_urls=["https://tech.cloud.nongshim.co.kr/post/", "https://tech.cloud.nongshim.co.kr/post/page/2/"],
        include=r"^https://tech\.cloud\.nongshim\.co\.kr/blog/[^/?#]+(?:/[^/?#]+)*/\d+/?$",
        exclude=r"/blog/category/|/post/page/",
        request=request,
    )
