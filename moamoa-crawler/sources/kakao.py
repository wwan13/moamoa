from __future__ import annotations

from _common import Post, fetch_json, key_from_url, make_payload, normalize_space, strip_html, unique_posts


BASE_URL = "https://tech.kakao.com"


def crawl(request, config) -> dict[str, object]:
    api_url = f"{BASE_URL}/api/v2/posts?page=1&code=blog"
    data = fetch_json(api_url, headers={"Accept": "application/json", "Referer": f"{BASE_URL}/blog"})
    posts = []
    for item in data.get("postList") or []:
        post_id = item.get("id")
        title = normalize_space(item.get("title"))
        if post_id is None or not title:
            continue
        url = f"{BASE_URL}/posts/{post_id}"
        posts.append(
            Post(
                key=key_from_url(url),
                title=title,
                description=strip_html(item.get("description")),
                tags=[normalize_space(category.get("name")) for category in item.get("categories") or [] if normalize_space(category.get("name"))],
                thumbnail=normalize_space(item.get("thumb")),
                publishedAt=normalize_space(item.get("releaseDateTime") or item.get("releaseDate")),
                url=url,
                source="api",
            )
        )

    posts = unique_posts(posts, request.size)
    if not posts:
        raise RuntimeError("kakao API returned no posts")

    return make_payload(
        key="kakao",
        blog="Kakao Tech",
        base_url=BASE_URL,
        requested_url=api_url,
        crawler="api.urllib",
        requested_size=request.size,
        posts=posts,
    )
