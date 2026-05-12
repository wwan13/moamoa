from __future__ import annotations

from _common import html_link_posts


def crawl(request, config) -> dict[str, object]:
    return html_link_posts(
        key="danawa",
        blog="Danawa Lab",
        base_url="https://danawalab.github.io",
        list_urls=["https://danawalab.github.io"],
        include=r"^https://danawalab\.github\.io/[^/?#]+/\d{4}/\d{2}/\d{2}/[^/?#]+\.html$",
        exclude=r"/category/|/tags/|/about|/archive|/page/",
        request=request,
    )
