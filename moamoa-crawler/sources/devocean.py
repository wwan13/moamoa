from __future__ import annotations

import re

from _common import Post, fetch_text, make_payload, strip_html, unique_posts


BASE_URL = "https://devocean.sk.com"
LIST_URL = f"{BASE_URL}/blog/index.do?p=BLOG"


def crawl(request, config) -> dict[str, object]:
    body = fetch_text(LIST_URL)
    posts = []
    for match in re.finditer(
        r"<h3[^>]+onclick=[\"']goDetail\(this,[\"'](\d+)[\"'][^>]*>(.*?)</h3>",
        body,
        flags=re.IGNORECASE | re.DOTALL,
    ):
        post_id = match.group(1)
        title = strip_html(match.group(2))
        if not title:
            continue
        url = f"{BASE_URL}/blog/techBoardDetail.do?ID={post_id}&boardType=techBlog"
        posts.append(Post(post_id, title, "", [], "", "", url, "html"))

    posts = unique_posts(posts, request.size)
    if not posts:
        raise RuntimeError(f"devocean crawl finished but no post links were extracted from {LIST_URL}")

    return make_payload(
        key="devocean",
        blog="DEVOCEAN",
        base_url=BASE_URL,
        requested_url=LIST_URL,
        crawler="html.urllib",
        requested_size=request.size,
        posts=posts,
    )
