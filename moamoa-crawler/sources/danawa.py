from __future__ import annotations

from _common import Post, fetch_html, make_payload_raw as make_payload


KEY = "danawa"
BLOG = "Danawa Lab"
BASE_URL = "https://danawalab.github.io"


def crawl(request, config) -> dict[str, object]:
    del config
    tags = _fetch_tags()
    posts: list[Post] = []
    for tag_name, tag_url in tags:
        tag_doc = fetch_html(tag_url)
        for post in tag_doc.select("div.content__post"):
            link = post.select_first("a.content__link[href]")
            title = post.select_first("h3.content__h3")
            date_el = post.select_first("span.date")
            if link is None or title is None or date_el is None:
                continue

            url = BASE_URL + link.attr("href")
            posts.append(
                Post(
                    key=_extract_key(url),
                    title=title.text(),
                    description=post.select_first("p.content__p").text() if post.select_first("p.content__p") else "",
                    tags=[tag_name],
                    thumbnail="",
                    publishedAt=_parse_date(date_el.text().rstrip(".")),
                    url=url,
                    source="html",
                )
            )
            if request.size and len(posts) >= request.size:
                break
        if request.size and len(posts) >= request.size:
            break

    if not posts:
        raise RuntimeError(f"{KEY} crawl finished but no article links were extracted from {BASE_URL}")

    return make_payload(
        key=KEY,
        blog=BLOG,
        base_url=BASE_URL,
        requested_url=BASE_URL,
        crawler="html.urllib",
        requested_size=request.size,
        posts=posts,
    )


def _fetch_tags() -> list[tuple[str, str]]:
    doc = fetch_html(BASE_URL)
    tags: list[tuple[str, str]] = []
    seen_urls: set[str] = set()
    for anchor in doc.select('ul li a[href^="/category/"]'):
        href = anchor.attr("href")
        name = anchor.text().split("(", 1)[0].strip().lower()
        if not href or not name:
            continue
        url = BASE_URL + href
        if url in seen_urls:
            continue
        seen_urls.add(url)
        tags.append((name, url))
    return tags


def _extract_key(url: str) -> str:
    path = url.split("://", 1)[-1].split("/", 1)[-1]
    return "/" + path.rstrip("/")


def _parse_date(raw: str) -> str:
    year, month, day = raw.split(".")
    return f"{int(year):04d}-{int(month):02d}-{int(day):02d}T00:00:00"
