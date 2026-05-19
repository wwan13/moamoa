from __future__ import annotations

from datetime import datetime

from _common import Post, fetch_json, key_from_url, make_payload, normalize_published_at, normalize_space, unique_posts


KEY = "kakao"
BLOG = "Kakao Tech"
BASE_URL = "https://tech.kakao.com"
LIST_URL = f"{BASE_URL}/blog"


def crawl(request, config) -> dict[str, object]:
    del config

    requested_url = _build_api_url(page=1)
    posts: list[Post] = []
    page = 1
    total_pages: int | None = None

    while total_pages is None or page <= total_pages:
        data = fetch_json(
            _build_api_url(page=page),
            headers={"Accept": "application/json", "Referer": LIST_URL},
        )
        total_pages = _parse_total_pages(data)
        posts.extend(_parse_posts(data))
        posts = unique_posts(posts, request.size)

        if request.size is not None and len(posts) >= request.size:
            break

        page += 1

    if not posts:
        raise RuntimeError(f"{KEY} crawl finished but API returned no posts: {requested_url}")

    return make_payload(
        key=KEY,
        blog=BLOG,
        base_url=BASE_URL,
        requested_url=requested_url,
        crawler="api.urllib",
        requested_size=request.size,
        posts=posts,
    )


def _build_api_url(*, page: int) -> str:
    return f"{BASE_URL}/api/v2/posts?page={page}&code=blog"


def _parse_total_pages(data: object) -> int:
    if not isinstance(data, dict):
        return 0

    value = data.get("totalPageCount")
    try:
        return max(int(value), 0)
    except (TypeError, ValueError):
        return 0


def _parse_posts(data: object) -> list[Post]:
    if not isinstance(data, dict):
        return []

    parsed_posts: list[Post] = []
    for item in data.get("postList") or []:
        if not isinstance(item, dict):
            continue

        post_id = item.get("id")
        title = normalize_space(item.get("title"))
        if post_id is None or not title:
            continue

        post_url = f"{BASE_URL}/posts/{post_id}"
        parsed_posts.append(
            Post(
                key=key_from_url(post_url),
                title=title,
                description="",
                tags=[name for name in (_category_name(category) for category in item.get("categories") or []) if name],
                thumbnail=normalize_space(item.get("thumbnailUri") or item.get("thumb")),
                publishedAt=_parse_published_at(item.get("releaseDateTime") or item.get("releaseDate")),
                url=post_url,
                source="api",
            )
        )

    return parsed_posts


def _category_name(category: object) -> str:
    if not isinstance(category, dict):
        return ""
    return normalize_space(category.get("name"))


def _parse_published_at(value: object) -> str:
    text = normalize_space(str(value or ""))
    if not text:
        return ""

    try:
        return datetime.strptime(text, "%Y.%m.%d %H:%M:%S").strftime("%Y-%m-%dT%H:%M:%S")
    except ValueError:
        return normalize_published_at(text)
