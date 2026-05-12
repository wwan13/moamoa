from __future__ import annotations

import json
import re

from _common import Post, extract_links, fetch_text, key_from_url, make_payload, normalize_space, title_from_url, unique_posts, url_without_query


KEY = "buzzvil"
BASE_URL = "https://tech.buzzvil.com"
LIST_URL = f"{BASE_URL}/blog"


def crawl(request, config) -> dict[str, object]:
    body = fetch_text(LIST_URL)
    posts: list[Post] = []

    for match in re.finditer(r'"slug"\s*:\s*"([^"]+)".{0,800}?"title"\s*:\s*"([^"]+)"', body, flags=re.DOTALL):
        slug = normalize_space(match.group(1))
        title = normalize_space(_decode_json_string(match.group(2)))
        if slug and title:
            url = f"{BASE_URL}/blog/{slug}"
            posts.append(Post(key_from_url(url), title, "", [], "", "", url, "html-json"))

    if not posts:
        for link in extract_links(body, LIST_URL):
            url = url_without_query(link.url)
            if not re.match(r"^https://tech\.buzzvil\.com/blog/[^/?#]+/?$", url):
                continue
            if "/blog/page-" in url:
                continue
            title = link.text or title_from_url(url)
            posts.append(Post(key_from_url(url), title, "", [], "", "", url, "html"))

    if not posts:
        for url in sorted(set(BASE_URL + path for path in re.findall(r"/blog/(?!page-)[a-zA-Z0-9_-]+", body))):
            posts.append(Post(key_from_url(url), title_from_url(url), "", [], "", "", url, "html-regex"))

    posts = unique_posts(posts, request.size)
    if not posts:
        raise RuntimeError("buzzvil crawl finished but no post links were extracted")
    return make_payload(key=KEY, blog="Buzzvil Tech Blog", base_url=BASE_URL, requested_url=LIST_URL, crawler="html.urllib", requested_size=request.size, posts=posts)


def _decode_json_string(value: str) -> str:
    try:
        return json.loads(f'"{value}"')
    except json.JSONDecodeError:
        return value
