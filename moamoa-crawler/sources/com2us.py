from __future__ import annotations

from _common import html_link_posts


def crawl(request, config) -> dict[str, object]:
    base = "https://on.com2us.com/tag/%EA%B8%B0%EC%88%A0%EB%B8%94%EB%A1%9C%EA%B7%B8/"
    return html_link_posts(
        key="com2us",
        blog="Com2uS Tech Blog",
        base_url=base,
        list_urls=[base, f"{base}page/2/"],
        include=r"^https://on\.com2us\.com/[^/?#]+/?$",
        exclude=r"/tag/|/category/|/author/|/page/",
        request=request,
    )
