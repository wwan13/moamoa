from __future__ import annotations

from _common import html_link_posts


def crawl(request, config) -> dict[str, object]:
    return html_link_posts(
        key="rapportlabs",
        blog="Rapport Labs Tech Blog",
        base_url="https://blog.rapportlabs.kr",
        list_urls=["https://blog.rapportlabs.kr", "https://blog.rapportlabs.kr?page=2"],
        include=r"^https://blog\.rapportlabs\.kr/[^/?#]+/?$",
        exclude=r"/category/|/tag/",
        request=request,
    )
