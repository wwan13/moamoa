from __future__ import annotations

from _common import html_link_posts


def crawl(request, config) -> dict[str, object]:
    return html_link_posts(
        key="oliveyoung",
        blog="Olive Young Tech",
        base_url="https://oliveyoung.tech",
        list_urls=["https://oliveyoung.tech", "https://oliveyoung.tech/page/2/"],
        include=r"^https://oliveyoung\.tech/\d{4}-\d{2}-\d{2}/[^/?#]+/?$",
        request=request,
    )
