from __future__ import annotations

from _common import html_link_posts


def crawl(request, config) -> dict[str, object]:
    return html_link_posts(
        key="goorm",
        blog="goorm TechBlog",
        base_url="https://tech.goorm.io",
        list_urls=["https://tech.goorm.io", "https://tech.goorm.io/page/2/"],
        include=r"^https://tech\.goorm\.io/[^?#]+/?$",
        exclude=r"/(category|comments|feed|page|wp-admin)/|[#?]",
        request=request,
    )
