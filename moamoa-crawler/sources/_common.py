from __future__ import annotations

import html
import json
import re
import urllib.parse
import urllib.request
from dataclasses import asdict, dataclass
from datetime import datetime, timedelta, timezone
from email.utils import parsedate_to_datetime
from html.parser import HTMLParser
from typing import Any, Callable, Iterable

from lxml import etree
from lxml import html as lxml_html


USER_AGENT = (
    "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) "
    "AppleWebKit/537.36 (KHTML, like Gecko) "
    "Chrome/120.0.0.0 Safari/537.36"
)


@dataclass(frozen=True)
class Post:
    key: str
    title: str
    description: str
    tags: list[str]
    thumbnail: str
    publishedAt: str
    url: str
    source: str


@dataclass(frozen=True)
class Link:
    url: str
    text: str


class HtmlNode:
    def __init__(self, element: Any, base_url: str):
        self.element = element
        self.base_url = base_url

    def select(self, selector: str) -> list["HtmlNode"]:
        return [HtmlNode(node, self.base_url) for node in self.element.cssselect(selector)]

    def select_first(self, selector: str) -> "HtmlNode | None":
        nodes = self.element.cssselect(selector)
        return HtmlNode(nodes[0], self.base_url) if nodes else None

    def text(self) -> str:
        return normalize_space(self.element.text_content())

    def attr(self, name: str) -> str:
        return normalize_space(self.element.get(name))

    def abs_url(self, name: str) -> str:
        raw = self.element.get(name)
        if not raw:
            return ""
        return normalize_url(self.base_url, raw)


class HtmlDocument(HtmlNode):
    pass


class AnchorParser(HTMLParser):
    def __init__(self, base_url: str):
        super().__init__(convert_charrefs=True)
        self.base_url = base_url
        self.links: list[Link] = []
        self._href_stack: list[str | None] = []
        self._text_stack: list[list[str]] = []

    def handle_starttag(self, tag: str, attrs: list[tuple[str, str | None]]) -> None:
        attrs_map = {name.lower(): value for name, value in attrs}
        if tag.lower() == "a":
            href = attrs_map.get("href")
            self._href_stack.append(href)
            self._text_stack.append([])

    def handle_data(self, data: str) -> None:
        if self._text_stack:
            self._text_stack[-1].append(data)

    def handle_endtag(self, tag: str) -> None:
        if tag.lower() != "a" or not self._href_stack:
            return
        href = self._href_stack.pop()
        text = normalize_space(" ".join(self._text_stack.pop()))
        if href:
            self.links.append(Link(url=normalize_url(self.base_url, href), text=text))


def fetch_text(url: str, *, method: str = "GET", data: bytes | None = None, headers: dict[str, str] | None = None) -> str:
    request_headers = {
        "User-Agent": USER_AGENT,
        "Accept": "text/html,application/xhtml+xml,application/xml,application/json;q=0.9,*/*;q=0.8",
    }
    if headers:
        request_headers.update(headers)
    req = urllib.request.Request(url, data=data, method=method, headers=request_headers)
    with urllib.request.urlopen(req, timeout=20) as response:
        charset = response.headers.get_content_charset() or "utf-8"
        return response.read().decode(charset, errors="replace")


def fetch_json(url: str, *, method: str = "GET", data: bytes | None = None, headers: dict[str, str] | None = None) -> Any:
    text = fetch_text(url, method=method, data=data, headers=headers)
    return json.loads(text)


def parse_html(body: str, base_url: str) -> HtmlDocument:
    root = lxml_html.fromstring(body)
    return HtmlDocument(root, base_url)


def fetch_html(url: str, *, method: str = "GET", data: bytes | None = None, headers: dict[str, str] | None = None) -> HtmlDocument:
    return parse_html(fetch_text(url, method=method, data=data, headers=headers), url)


def parse_xml(body: str) -> etree._Element:
    parser = etree.XMLParser(recover=True)
    return etree.fromstring(body.encode("utf-8"), parser=parser)


def normalize_space(value: str | None) -> str:
    if not value:
        return ""
    return re.sub(r"\s+", " ", html.unescape(value).replace("\u00a0", " ")).strip()


def strip_html(value: str | None) -> str:
    if not value:
        return ""
    return normalize_space(re.sub(r"<[^>]+>", " ", value))


def normalize_tag(value: str | None) -> str:
    return normalize_space(value).lstrip("#")


def normalize_url(base_url: str, value: str) -> str:
    absolute = urllib.parse.urljoin(base_url, html.unescape(value).strip())
    parsed = urllib.parse.urlsplit(absolute)
    return urllib.parse.urlunsplit((parsed.scheme, parsed.netloc, parsed.path, parsed.query, ""))


def url_without_query(url: str) -> str:
    parsed = urllib.parse.urlsplit(url)
    return urllib.parse.urlunsplit((parsed.scheme, parsed.netloc, parsed.path, "", ""))


def key_from_url(url: str) -> str:
    parsed = urllib.parse.urlsplit(url_without_query(url))
    path = parsed.path.strip("/")
    if not path:
        return parsed.netloc
    return path.removesuffix(".html").split("/")[-1] or path


def title_from_url(url: str) -> str:
    key = urllib.parse.unquote(key_from_url(url))
    key = re.sub(r"[-_]+", " ", key)
    return normalize_space(key)


def extract_links(body: str, base_url: str) -> list[Link]:
    parser = AnchorParser(base_url)
    parser.feed(body)
    return parser.links


def extract_meta(body: str, name: str) -> str:
    patterns = [
        rf'<meta[^>]+property=["\']{re.escape(name)}["\'][^>]+content=["\']([^"\']+)["\']',
        rf'<meta[^>]+name=["\']{re.escape(name)}["\'][^>]+content=["\']([^"\']+)["\']',
        rf'<meta[^>]+content=["\']([^"\']+)["\'][^>]+property=["\']{re.escape(name)}["\']',
        rf'<meta[^>]+content=["\']([^"\']+)["\'][^>]+name=["\']{re.escape(name)}["\']',
    ]
    for pattern in patterns:
        match = re.search(pattern, body, flags=re.IGNORECASE | re.DOTALL)
        if match:
            return normalize_space(match.group(1))
    return ""


def extract_json_ld_objects(body: str) -> list[dict[str, Any]]:
    objects: list[dict[str, Any]] = []
    for match in re.finditer(
        r"<script[^>]+type=[\"']application/ld\+json[\"'][^>]*>(.*?)</script>",
        body,
        flags=re.IGNORECASE | re.DOTALL,
    ):
        raw = html.unescape(match.group(1)).strip()
        if not raw:
            continue
        try:
            parsed = json.loads(raw)
        except json.JSONDecodeError:
            continue
        objects.extend(_flatten_json_ld(parsed))
    return objects


def _flatten_json_ld(value: Any) -> list[dict[str, Any]]:
    if isinstance(value, dict):
        result = [value]
        graph = value.get("@graph")
        if isinstance(graph, list):
            for item in graph:
                result.extend(_flatten_json_ld(item))
        return result
    if isinstance(value, list):
        result: list[dict[str, Any]] = []
        for item in value:
            result.extend(_flatten_json_ld(item))
        return result
    return []


def extract_title(body: str) -> str:
    og_title = extract_meta(body, "og:title")
    if og_title:
        return og_title
    match = re.search(r"<title[^>]*>(.*?)</title>", body, flags=re.IGNORECASE | re.DOTALL)
    return strip_html(match.group(1)) if match else ""


def normalize_post_title(value: str | None) -> str:
    title = normalize_space(value)
    if not title:
        return ""
    title = re.sub(r"^Samsung Tech Blog\s*-\s*", "", title, flags=re.IGNORECASE)
    title = re.sub(r"\s*[-|]\s*(?:Medium|MUSINSA techblog|TECH\.KAKAO\.COM|Kakao Tech|LINE ENGINEERING|DEVOCEAN|NAVER D2|뱅크샐러드|카카오페이 기술 블로그|교보DTS 기술 블로그)\s*$", "", title, flags=re.IGNORECASE)
    title = re.sub(r"\s*\|\s*[^|]{1,40}(?:Tech|Blog|Engineering|Developers?)\s*$", "", title, flags=re.IGNORECASE)
    title = re.sub(r"\s*[–-]\s*교보DTS 기술 블로그\s*$", "", title)
    return normalize_space(title)


def normalize_published_at(value: Any) -> str:
    if value is None:
        return ""
    if isinstance(value, (int, float)):
        number = float(value)
        if number <= 0:
            return ""
        if number > 10_000_000_000:
            number = number / 1000
        return _format_local_datetime(datetime.fromtimestamp(number, tz=timezone.utc))

    text = normalize_space(str(value))
    if not text:
        return ""
    text = text.replace("Z", "+00:00")
    text = re.sub(r"([+-]\d{2}:\d{2})(?:\1)+$", r"\1", text)

    if re.fullmatch(r"\d{13}", text):
        return datetime.fromtimestamp(int(text) / 1000, tz=timezone.utc).isoformat()
    if re.fullmatch(r"\d{10}", text):
        return datetime.fromtimestamp(int(text), tz=timezone.utc).isoformat()

    candidates = [text]
    if re.fullmatch(r"\d{4}\.\d{1,2}\.\d{1,2}\.?", text):
        candidates.append(text.rstrip(".").replace(".", "-"))
    if re.fullmatch(r"\d{2}\.\d{1,2}\.\d{1,2}\.?", text):
        parts = [int(part) for part in text.rstrip(".").split(".")]
        candidates.append(f"20{parts[0]:02d}-{parts[1]:02d}-{parts[2]:02d}")
    if re.fullmatch(r"\d{4}/\d{1,2}/\d{1,2}", text):
        candidates.append(text.replace("/", "-"))
    match = re.search(r"(\d{4})년\s*(\d{1,2})월\s*(\d{1,2})일", text)
    if match:
        candidates.append(f"{match.group(1)}-{int(match.group(2)):02d}-{int(match.group(3)):02d}")

    for candidate in candidates:
        try:
            return _format_local_datetime(datetime.fromisoformat(candidate))
        except ValueError:
            pass
        for pattern in ("%Y-%m-%d", "%Y-%m-%d %H:%M:%S", "%Y.%m.%d", "%Y/%m/%d", "%b %d, %Y", "%B %d, %Y"):
            try:
                parsed = datetime.strptime(candidate, pattern)
            except ValueError:
                continue
            return _format_local_datetime(parsed)

    try:
        parsed_email_date = parsedate_to_datetime(text)
    except (TypeError, ValueError):
        return ""
    return _format_local_datetime(parsed_email_date)


def _format_local_datetime(value: datetime) -> str:
    if value.tzinfo is not None:
        value = value.astimezone(timezone.utc).replace(tzinfo=None)
    return value.replace(microsecond=0).isoformat(timespec="seconds")


def enrich_post_details(posts: Iterable[Post], *, limit: int | None = None) -> list[Post]:
    result: list[Post] = []
    for post in posts:
        if limit is not None and len(result) >= limit:
            break
        result.append(enrich_post_detail(post))
    return result


def enrich_post_detail(post: Post) -> Post:
    try:
        body = fetch_text(post.url)
    except Exception:
        return _normalized_post(post)

    json_ld = extract_json_ld_objects(body)
    title = _first_json_ld_value(json_ld, "headline", "name") or extract_meta(body, "og:title") or extract_meta(body, "twitter:title") or extract_title(body)
    title = "" if _is_generic_detail_title(title) else title
    description = (
        _first_json_ld_value(json_ld, "description")
        or extract_meta(body, "description")
        or extract_meta(body, "og:description")
        or extract_meta(body, "twitter:description")
    )
    if _is_generic_description(description):
        description = ""
    image = _first_json_ld_image(json_ld) or extract_meta(body, "og:image") or extract_meta(body, "twitter:image")
    published_at = (
        _first_json_ld_value(json_ld, "datePublished", "dateCreated", "dateModified")
        or extract_meta(body, "article:published_time")
        or extract_meta(body, "pubdate")
        or extract_meta(body, "publishdate")
        or extract_meta(body, "date")
        or _extract_inline_json_value(body, "datePublished", "publishedAt", "createdAt")
        or _extract_html_date(body)
    )
    tags = post.tags or _json_ld_keywords(json_ld)

    return Post(
        key=post.key,
        title=normalize_post_title(title) or normalize_post_title(post.title),
        description=strip_html(description) or post.description,
        tags=tags,
        thumbnail=normalize_url(post.url, image) if image else post.thumbnail,
        publishedAt=normalize_published_at(published_at) or normalize_published_at(post.publishedAt) or _published_at_from_url(post.url),
        url=post.url,
        source=post.source,
    )


def _normalized_post(post: Post) -> Post:
    return Post(
        key=post.key,
        title=normalize_post_title(post.title),
        description=strip_html(post.description),
        tags=[normalize_tag(tag) for tag in post.tags if normalize_tag(tag)],
        thumbnail=post.thumbnail,
        publishedAt=normalize_published_at(post.publishedAt) or _published_at_from_url(post.url),
        url=post.url,
        source=post.source,
    )


def _published_at_from_url(url: str) -> str:
    decoded = urllib.parse.unquote(urllib.parse.urlsplit(url).path)
    patterns = [
        r"/(\d{4})/(\d{1,2})/(\d{1,2})(?:/|$)",
        r"/(\d{4})-(\d{1,2})-(\d{1,2})[-/]",
    ]
    for pattern in patterns:
        match = re.search(pattern, decoded)
        if match:
            return f"{int(match.group(1)):04d}-{int(match.group(2)):02d}-{int(match.group(3)):02d}T00:00:00"
    return ""


def _is_generic_detail_title(value: str | None) -> bool:
    title = normalize_space(value)
    if not title:
        return False
    generic_titles = {
        "goorm TechBlog",
        "원티드 - 일하는 사람들의 모든 가능성",
        "SOCAR Tech Blog",
    }
    return title in generic_titles


def _is_generic_description(value: str | None) -> bool:
    description = normalize_space(value)
    if not description:
        return False
    generic_descriptions = {
        "데보션 (DEVOCEAN) 기술 블로그 , 개발자 커뮤니티이자 내/외부 소통과 성장 플랫폼",
    }
    return description in generic_descriptions


def _extract_inline_json_value(body: str, *names: str) -> str:
    for name in names:
        patterns = [
            rf'"{re.escape(name)}"\s*:\s*"([^"]+)"',
            rf'\\"{re.escape(name)}\\"\s*:\s*\\"([^"\\]+)',
        ]
        for pattern in patterns:
            match = re.search(pattern, body, flags=re.IGNORECASE)
            if match:
                return html.unescape(match.group(1)).replace("\\/", "/")
    return ""


def _extract_html_date(body: str) -> str:
    patterns = [
        r"<time[^>]+datetime=[\"']([^\"']+)[\"']",
        r"itemprop=[\"']datePublished[\"'][^>]+content=[\"']([^\"']+)[\"']",
        r"content=[\"']([^\"']+)[\"'][^>]+itemprop=[\"']datePublished[\"']",
        r"class=[\"'][^\"']*(?:upload-date|published|post-date|date)[^\"']*[\"'][^>]*>([^<]+)<",
        r"id=[\"'][^\"']*(?:Regdate|regdate|date)[^\"']*[\"'][^>]*>([^<]+)<",
    ]
    for pattern in patterns:
        match = re.search(pattern, body, flags=re.IGNORECASE | re.DOTALL)
        if match:
            return strip_html(match.group(1))
    return ""


def _first_json_ld_value(objects: list[dict[str, Any]], *names: str) -> str:
    for obj in _preferred_json_ld_objects(objects):
        for name in names:
            value = obj.get(name)
            if isinstance(value, str) and normalize_space(value):
                return normalize_space(value)
    return ""


def _first_json_ld_image(objects: list[dict[str, Any]]) -> str:
    for obj in _preferred_json_ld_objects(objects):
        image = obj.get("image") or obj.get("thumbnailUrl")
        if isinstance(image, str) and image:
            return image
        if isinstance(image, list):
            for item in image:
                if isinstance(item, str) and item:
                    return item
                if isinstance(item, dict) and item.get("url"):
                    return str(item["url"])
        if isinstance(image, dict) and image.get("url"):
            return str(image["url"])
    return ""


def _json_ld_keywords(objects: list[dict[str, Any]]) -> list[str]:
    for obj in _preferred_json_ld_objects(objects):
        keywords = obj.get("keywords")
        if isinstance(keywords, str):
            return [normalize_tag(tag) for tag in re.split(r"[,#]", keywords) if normalize_tag(tag)]
        if isinstance(keywords, list):
            return [normalize_tag(str(tag)) for tag in keywords if normalize_tag(str(tag))]
    return []


def _preferred_json_ld_objects(objects: list[dict[str, Any]]) -> list[dict[str, Any]]:
    preferred_types = {
        "article",
        "blogposting",
        "newsarticle",
        "techarticle",
        "report",
        "webpage",
    }
    preferred = [obj for obj in objects if _json_ld_type_name(obj) in preferred_types]
    return preferred or objects


def _json_ld_type_name(obj: dict[str, Any]) -> str:
    value = obj.get("@type")
    if isinstance(value, list):
        for item in value:
            normalized = normalize_space(str(item)).lower()
            if normalized:
                return normalized
        return ""
    return normalize_space(str(value)).lower()


def iso_now() -> str:
    return datetime.now(timezone.utc).isoformat()


def make_payload(
    *,
    key: str,
    blog: str,
    base_url: str,
    requested_url: str,
    crawler: str,
    requested_size: int | None,
    posts: list[Post],
) -> dict[str, object]:
    return {
        "key": key,
        "blog": blog,
        "baseUrl": base_url,
        "requestedUrl": requested_url,
        "crawler": crawler,
        "crawledAt": iso_now(),
        "requestedSize": requested_size,
        "postCount": len(posts),
        "posts": [asdict(_normalized_post(post)) for post in posts],
    }


def make_payload_raw(
    *,
    key: str,
    blog: str,
    base_url: str,
    requested_url: str,
    crawler: str,
    requested_size: int | None,
    posts: list[Post],
) -> dict[str, object]:
    return {
        "key": key,
        "blog": blog,
        "baseUrl": base_url,
        "requestedUrl": requested_url,
        "crawler": crawler,
        "crawledAt": iso_now(),
        "requestedSize": requested_size,
        "postCount": len(posts),
        "posts": [asdict(post) for post in posts],
    }


def unique_posts(posts: Iterable[Post], limit: int | None) -> list[Post]:
    result: list[Post] = []
    seen: set[str] = set()
    for post in posts:
        if not post.url or post.url in seen:
            continue
        seen.add(post.url)
        result.append(post)
        if limit is not None and len(result) >= limit:
            break
    return result


def html_link_posts(
    *,
    key: str,
    blog: str,
    base_url: str,
    list_urls: list[str],
    include: str,
    request: Any,
    crawler: str = "html.urllib",
    exclude: str | None = None,
) -> dict[str, object]:
    include_re = re.compile(include)
    exclude_re = re.compile(exclude) if exclude else None
    posts: list[Post] = []
    first_url = list_urls[0]

    for list_url in list_urls:
        body = fetch_text(list_url)
        for link in extract_links(body, list_url):
            url = url_without_query(link.url)
            if not include_re.search(url):
                continue
            if exclude_re and exclude_re.search(url):
                continue
            title = link.text or title_from_url(url)
            if not title:
                continue
            posts.append(
                Post(
                    key=key_from_url(url),
                    title=title,
                    description="",
                    tags=[],
                    thumbnail="",
                    publishedAt="",
                    url=url,
                    source="html",
                )
            )
            posts = unique_posts(posts, request.size)
            if request.size is not None and len(posts) >= request.size:
                break
        if request.size is not None and len(posts) >= request.size:
            break

    posts = unique_posts(posts, request.size)
    if not posts:
        raise RuntimeError(f"{key} crawl finished but no post links were extracted from {first_url}")

    return make_payload(
        key=key,
        blog=blog,
        base_url=base_url,
        requested_url=first_url,
        crawler=crawler,
        requested_size=request.size,
        posts=posts,
    )


def medium_browser_posts(
    *,
    key: str,
    blog: str,
    base_url: str,
    request: Any,
    config: Any,
    post_path_prefix: str | None = None,
    publication_host: str | None = None,
) -> dict[str, object]:
    from scrapling.fetchers import StealthyFetcher

    parsed_base = urllib.parse.urlsplit(base_url)
    allowed_hosts = {parsed_base.netloc}
    if publication_host:
        allowed_hosts.add(publication_host)
    list_url = base_url.rstrip("/") + "/all"

    def fetch_page(url: str) -> Any:
        return StealthyFetcher.fetch(
            url,
            headless=config.headless,
            network_idle=True,
            wait=config.wait,
            page_action=_scroll_until_stable(config.scroll_wait),
        )

    posts: list[Post] = []
    page = fetch_page(list_url)
    posts.extend(_medium_posts_from_page(page, base_url, allowed_hosts, post_path_prefix))
    posts = unique_posts(posts, len(posts))
    if not posts:
        raise RuntimeError(f"{key} Medium browser crawl finished but no post links were extracted from {list_url}")

    return make_payload(
        key=key,
        blog=blog,
        base_url=base_url,
        requested_url=list_url,
        crawler="scrapling.StealthyFetcher",
        requested_size=len(posts),
        posts=posts,
    )


def card_browser_posts(
    *,
    key: str,
    blog: str,
    base_url: str,
    request: Any,
    config: Any,
    is_post_url: Callable[[str], bool],
    no_posts_message: str | None = None,
) -> dict[str, object]:
    from scrapling.fetchers import StealthyFetcher

    list_url = base_url.rstrip("/") + "/all"
    page = StealthyFetcher.fetch(
        list_url,
        headless=config.headless,
        network_idle=True,
        wait=config.wait,
        page_action=_scroll_page(config.scrolls, config.scroll_wait),
    )
    posts = _card_posts_from_page(page, base_url, is_post_url)
    posts = unique_posts(posts, request.size)
    if not posts:
        raise RuntimeError(no_posts_message or f"{key} browser crawl finished but no post links were extracted from /all")

    return make_payload(
        key=key,
        blog=blog,
        base_url=base_url,
        requested_url=list_url,
        crawler="scrapling.StealthyFetcher",
        requested_size=request.size,
        posts=posts,
    )


def _scroll_page(scrolls: int, scroll_wait: int):
    def action(page: Any) -> None:
        for _ in range(scrolls):
            page.mouse.wheel(0, 2400)
            page.wait_for_timeout(scroll_wait)

    return action


def _scroll_until_stable(scroll_wait: int):
    def action(page: Any) -> None:
        stable_count = 0
        previous_height = 0
        for _ in range(200):
            current_height = page.evaluate("document.body.scrollHeight")
            if current_height == previous_height:
                stable_count += 1
            else:
                stable_count = 0
                previous_height = current_height
            if stable_count >= 3:
                break
            page.mouse.wheel(0, 2400)
            page.wait_for_timeout(scroll_wait)

    return action


def _medium_posts_from_page(page: Any, base_url: str, allowed_hosts: set[str], post_path_prefix: str | None) -> list[Post]:
    posts: list[Post] = []
    for anchor in page.css("a[href]"):
        href = _selector_attr(anchor, "href")
        if not href:
            continue
        url = url_without_query(normalize_url(base_url, href))
        if not _is_medium_post_url(url, allowed_hosts, post_path_prefix):
            continue
        title = _selector_text(anchor) or _selector_attr(anchor, "aria-label") or _selector_attr(anchor, "title") or title_from_url(url)
        title = re.sub(r"-[0-9a-f]{12,}$", "", title).replace("-", " ")
        title = normalize_space(title)
        if title:
            posts.append(Post(key_from_url(url), normalize_post_title(title), "", [], "", "", url, "browser"))
    return posts


def _is_medium_post_url(url: str, allowed_hosts: set[str], post_path_prefix: str | None) -> bool:
    parsed = urllib.parse.urlsplit(url)
    path = urllib.parse.unquote(parsed.path)
    if parsed.netloc not in allowed_hosts:
        return False
    if post_path_prefix and not path.startswith(post_path_prefix):
        return False
    excluded = ("/about", "/archive", "/followers", "/latest", "/lists", "/membership", "/search", "/subpage", "/tagged")
    path_after_prefix = path.removeprefix(post_path_prefix or "")
    if path_after_prefix.startswith(excluded):
        return False
    return re.search(r"-[0-9a-f]{12}/?$", path) is not None




def _selector_attr(selector: Any, name: str) -> str:
    if hasattr(selector, "attrib"):
        value = selector.attrib.get(name)
        if value:
            return str(value)
    if hasattr(selector, "get"):
        try:
            value = selector.get(name)
            if value:
                return str(value)
        except TypeError:
            return ""
    return ""


def _selector_text(selector: Any) -> str:
    value = getattr(selector, "text", "")
    if callable(value):
        value = value()
    return normalize_space(str(value)) if value else ""


def _card_posts_from_page(page: Any, base_url: str, is_post_url: Callable[[str], bool]) -> list[Post]:
    posts: list[Post] = []
    for anchor in page.css("a[href]"):
        if not _is_primary_post_anchor(anchor):
            continue
        href = _selector_attr(anchor, "href")
        if not href:
            continue
        url = url_without_query(normalize_url(base_url, href))
        if not is_post_url(url):
            continue
        title = _extract_card_title(anchor, url)
        if not title:
            continue
        description = _extract_card_description(anchor, title)
        thumbnail = _extract_card_thumbnail(anchor, title, base_url)
        published_at = _extract_card_published_at(anchor, url, base_url)
        posts.append(Post(key_from_url(url), title, description, [], thumbnail, published_at, url, "browser"))
    return unique_posts(posts, 10_000)


def _is_primary_post_anchor(anchor: Any) -> bool:
    if _first_heading_text(anchor, "h2"):
        return True
    if _first_heading_text(anchor, "h3"):
        return True
    return False


def _extract_card_title(anchor: Any, url: str) -> str:
    title = _first_heading_text(anchor, "h2")
    if not title:
        title = _first_heading_text(anchor, "h3")
    if not title:
        title = _selector_attr(anchor, "aria-label") or _selector_attr(anchor, "title") or _selector_text(anchor)
    if not title:
        title = title_from_url(url)
    title = re.sub(r"-[0-9a-f]{12,}$", "", title).replace("-", " ")
    return normalize_space(title)


def _extract_card_description(anchor: Any, title: str) -> str:
    description = _first_h3_text(anchor, exclude=title)
    if description:
        return description
    for node in _candidate_nodes(anchor, depth=4):
        description = _first_h3_text(node, exclude=title)
        if description:
            return description
        lines = _meaningful_lines(node)
        description = _description_after_title(lines, title)
        if description:
            return description
    return ""


def _extract_card_thumbnail(anchor: Any, title: str, base_url: str) -> str:
    for node in _candidate_nodes(anchor, depth=8):
        image = _first_content_image_from_node(node, title, base_url)
        if image:
            return image
    return ""


def _extract_card_published_at(anchor: Any, url: str, base_url: str) -> str:
    for node in _candidate_nodes(anchor, depth=8):
        published_at = _published_at_from_same_card(node, url, base_url)
        if published_at:
            return published_at
    return ""


def _candidate_nodes(node: Any, *, depth: int) -> list[Any]:
    nodes: list[Any] = []
    current = node
    for _ in range(depth):
        if current is None:
            break
        nodes.append(current)
        current = getattr(current, "parent", None)
    return nodes


def _meaningful_lines(node: Any) -> list[str]:
    raw = _raw_text_of(node)
    if not raw:
        return []
    lines: list[str] = []
    for line in re.split(r"[\r\n]+", raw):
        normalized = normalize_space(line)
        if normalized:
            lines.append(normalized)
    return lines


def _description_after_title(lines: list[str], title: str) -> str:
    normalized_title = normalize_space(title)
    if not normalized_title:
        return ""
    for index, line in enumerate(lines):
        if line != normalized_title:
            continue
        for candidate in lines[index + 1 :]:
            if _is_boilerplate_line(candidate):
                continue
            if candidate == normalized_title:
                continue
            return candidate
    return ""


def _is_boilerplate_line(value: str) -> bool:
    if re.search(r"\bAdded\b", value, flags=re.IGNORECASE):
        return True
    if re.search(r"\bmin read\b", value, flags=re.IGNORECASE):
        return True
    if "·" in value:
        return True
    return False


def _raw_text_of(selector: Any) -> str:
    value = getattr(selector, "text", "")
    if callable(value):
        value = value()
    return str(value).strip() if value else ""


def _first_content_image_from_node(node: Any, title: str, base_url: str) -> str:
    try:
        images = node.css("img")
    except Exception:
        return ""
    normalized_title = normalize_space(title)
    fallback = ""
    for image in images:
        alt = normalize_space(_selector_attr(image, "alt"))
        if _is_avatar_image(image, alt):
            continue
        image_url = ""
        for name in ("src", "data-src", "srcset", "data-srcset"):
            value = _selector_attr(image, name)
            if value:
                image_url = _normalize_card_image_url(value, base_url)
                break
        if not image_url:
            continue
        if alt and normalized_title and alt == normalized_title:
            return image_url
        if not fallback:
            fallback = image_url
    return fallback


def _normalize_card_image_url(value: str, base_url: str) -> str:
    first = value.split(",")[0].strip()
    first = first.split(" ")[0].strip()
    return normalize_url(base_url, first) if first else ""


def _first_h3_text(node: Any, *, exclude: str) -> str:
    try:
        headings = node.css("h3")
    except Exception:
        return ""
    excluded = normalize_space(exclude)
    for heading in headings:
        text = _selector_text(heading)
        if text and text != excluded:
            return text
    return ""


def _first_heading_text(node: Any, selector: str) -> str:
    try:
        headings = node.css(selector)
    except Exception:
        return ""
    for heading in headings:
        text = _selector_text(heading)
        if text:
            return text
    return ""


def _published_at_from_same_card(node: Any, url: str, base_url: str) -> str:
    try:
        anchors = node.css("a[href]")
    except Exception:
        return ""
    for candidate in anchors:
        href = _selector_attr(candidate, "href")
        if not href:
            continue
        if url_without_query(normalize_url(base_url, href)) != url:
            continue
        candidate_texts = _candidate_texts(candidate)
        combined_text = normalize_space(" ".join(candidate_texts))
        if combined_text and combined_text not in candidate_texts:
            candidate_texts.append(combined_text)
        for text in candidate_texts:
            published_at = _published_at_from_added_text(text)
            if published_at:
                return published_at
    return ""


def _published_at_from_added_text(value: str) -> str:
    text = normalize_space(value)
    if not text:
        return ""

    absolute_with_year_match = re.search(r"\b([A-Za-z]{3,9}\s+\d{1,2},\s+\d{4})\b", text, flags=re.IGNORECASE)
    if absolute_with_year_match:
        published_at = normalize_published_at(absolute_with_year_match.group(1))
        if published_at:
            return published_at

    month_day_match = re.search(r"\b([A-Za-z]{3,9}\s+\d{1,2})\b", text, flags=re.IGNORECASE)
    if month_day_match:
        published_at = _published_at_from_month_day(month_day_match.group(1))
        if published_at:
            return published_at

    relative_match = re.search(
        r"\b(\d+\s*(?:s|sec|secs|second|seconds|m|min|mins|minute|minutes|h|hr|hrs|hour|hours|d|day|days|w|week|weeks)\s+ago)\b",
        text,
        flags=re.IGNORECASE,
    )
    if not relative_match:
        relative_match = re.search(
            r"\b(\d+(?:s|sec|secs|second|seconds|m|min|mins|minute|minutes|h|hr|hrs|hour|hours|d|day|days|w|week|weeks)\s+ago)\b",
            text,
            flags=re.IGNORECASE,
        )
    if not relative_match:
        return ""

    published_at = _published_at_from_relative_token(relative_match.group(1))
    if published_at:
        return published_at
    return ""


def _published_at_from_month_day(value: str) -> str:
    now = datetime.now(timezone.utc)
    for pattern in ("%b %d", "%B %d"):
        try:
            parsed = datetime.strptime(value, pattern)
        except ValueError:
            continue
        candidate = parsed.replace(year=now.year)
        candidate_utc = candidate.replace(tzinfo=timezone.utc)
        if candidate_utc > now + timedelta(days=1):
            candidate = candidate.replace(year=now.year - 1)
        return candidate.replace(microsecond=0).isoformat(timespec="seconds")
    return ""


def _published_at_from_relative_token(value: str) -> str:
    match = re.fullmatch(
        r"(\d+)\s*(s|sec|secs|second|seconds|m|min|mins|minute|minutes|h|hr|hrs|hour|hours|d|day|days|w|week|weeks)\s+ago",
        value,
        flags=re.IGNORECASE,
    )
    if not match:
        return ""

    amount = int(match.group(1))
    unit = match.group(2).lower()
    if unit in {"s", "sec", "secs", "second", "seconds"}:
        delta = timedelta(seconds=amount)
    elif unit in {"m", "min", "mins", "minute", "minutes"}:
        delta = timedelta(minutes=amount)
    elif unit in {"h", "hr", "hrs", "hour", "hours"}:
        delta = timedelta(hours=amount)
    elif unit in {"d", "day", "days"}:
        delta = timedelta(days=amount)
    elif unit in {"w", "week", "weeks"}:
        delta = timedelta(weeks=amount)
    else:
        return ""

    return (datetime.now(timezone.utc) - delta).replace(microsecond=0, tzinfo=None).isoformat(timespec="seconds")


def _candidate_texts(node: Any) -> list[str]:
    texts: list[str] = []
    for value in (_selector_text(node), _raw_text_of(node), _selector_attr(node, "aria-label"), _selector_attr(node, "title")):
        normalized = normalize_space(value)
        if normalized and normalized not in texts:
            texts.append(normalized)
    for selector in ("span", "div", "p"):
        try:
            children = node.css(selector)
        except Exception:
            continue
        for child in children:
            normalized = normalize_space(_selector_text(child) or _raw_text_of(child))
            if normalized and normalized not in texts:
                texts.append(normalized)
    return texts


def _is_avatar_image(image: Any, alt: str) -> bool:
    if alt and len(alt) <= 3:
        return True
    width = _int_selector_attr(image, "width")
    height = _int_selector_attr(image, "height")
    if 0 < width <= 40 and 0 < height <= 40:
        return True
    src = _selector_attr(image, "src") or _selector_attr(image, "data-src")
    if "resize:fill:40:40" in src:
        return True
    return False


def _int_selector_attr(selector: Any, name: str) -> int:
    value = _selector_attr(selector, name)
    if not value or not value.isdigit():
        return 0
    return int(value)
