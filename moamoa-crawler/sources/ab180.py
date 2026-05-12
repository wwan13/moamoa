from __future__ import annotations

import re

from _common import Post, fetch_text, key_from_url, make_payload, normalize_space, strip_html, unique_posts


KEY = "ab180"
BASE_URL = "https://engineering.ab180.co"
RSS_URL = "https://raw.githubusercontent.com/ab180/engineering-blog-rss-scheduler/main/rss.xml"


def crawl(request, config) -> dict[str, object]:
    body = fetch_text(RSS_URL)
    posts = []
    for item in re.findall(r"<item\b.*?</item>", body, flags=re.IGNORECASE | re.DOTALL):
        link = normalize_space(_tag(item, "link"))
        title = normalize_space(_tag(item, "title"))
        if not link or not title:
            continue
        description = strip_html(_tag(item, "description"))
        tags = [normalize_space(value) for value in re.findall(r"<category[^>]*><!\[CDATA\[(.*?)\]\]></category>|<category[^>]*>(.*?)</category>", item, flags=re.IGNORECASE | re.DOTALL) for value in value if normalize_space(value)]
        posts.append(Post(key_from_url(link), title, description, tags, "", normalize_space(_tag(item, "pubDate")), link, "rss"))
    posts = unique_posts(posts, request.size)
    if not posts:
        raise RuntimeError("ab180 RSS returned no posts")
    return make_payload(key=KEY, blog="AB180 Engineering", base_url=BASE_URL, requested_url=RSS_URL, crawler="rss.urllib", requested_size=request.size, posts=posts)


def _tag(body: str, tag: str) -> str:
    match = re.search(rf"<{tag}[^>]*>(?:<!\[CDATA\[)?(.*?)(?:\]\]>)?</{tag}>", body, flags=re.IGNORECASE | re.DOTALL)
    return match.group(1) if match else ""
