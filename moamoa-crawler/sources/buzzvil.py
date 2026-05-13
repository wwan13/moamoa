from __future__ import annotations

import json
import re

from _common import (
    Post,
    extract_json_ld_objects,
    extract_meta,
    extract_title,
    fetch_text,
    key_from_url,
    make_payload,
    normalize_published_at,
    normalize_space,
    normalize_url,
    strip_html,
    unique_posts,
)


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

    posts = unique_posts(posts, request.size)
    posts = [_enrich_post(post) for post in posts]
    if not posts:
        raise RuntimeError("buzzvil crawl finished but no post links were extracted")
    return make_payload(key=KEY, blog="Buzzvil Tech Blog", base_url=BASE_URL, requested_url=LIST_URL, crawler="html.urllib", requested_size=request.size, posts=posts)


def _decode_json_string(value: str) -> str:
    try:
        return json.loads(f'"{value}"')
    except json.JSONDecodeError:
        return value


def _enrich_post(post: Post) -> Post:
    body = fetch_text(post.url)
    json_ld = extract_json_ld_objects(body)
    title = (
        _first_json_ld_value(json_ld, "headline", "name")
        or extract_meta(body, "og:title")
        or extract_meta(body, "twitter:title")
        or extract_title(body)
    )
    description = (
        _first_json_ld_value(json_ld, "description")
        or extract_meta(body, "description")
        or extract_meta(body, "og:description")
        or extract_meta(body, "twitter:description")
    )
    image = _first_json_ld_image(json_ld) or extract_meta(body, "og:image") or extract_meta(body, "twitter:image")
    published_at = (
        _first_json_ld_value(json_ld, "datePublished", "dateCreated", "dateModified")
        or extract_meta(body, "article:published_time")
        or extract_meta(body, "publishdate")
        or extract_meta(body, "date")
    )
    return Post(
        key=post.key,
        title=normalize_space(title) or post.title,
        description=strip_html(description),
        tags=post.tags,
        thumbnail=normalize_url(post.url, image) if image else post.thumbnail,
        publishedAt=normalize_published_at(published_at),
        url=post.url,
        source=post.source,
    )


def _first_json_ld_value(objects: list[dict[str, object]], *names: str) -> str:
    for obj in objects:
        obj_type = normalize_space(str(obj.get("@type") or "")).lower()
        if obj_type not in {"article", "blogposting"}:
            continue
        for name in names:
            value = obj.get(name)
            if isinstance(value, str) and normalize_space(value):
                return normalize_space(value)
    return ""


def _first_json_ld_image(objects: list[dict[str, object]]) -> str:
    for obj in objects:
        obj_type = normalize_space(str(obj.get("@type") or "")).lower()
        if obj_type not in {"article", "blogposting"}:
            continue
        image = obj.get("image") or obj.get("thumbnailUrl")
        if isinstance(image, str) and normalize_space(image):
            return normalize_space(image)
        if isinstance(image, dict):
            url = image.get("url")
            if isinstance(url, str) and normalize_space(url):
                return normalize_space(url)
    return ""
