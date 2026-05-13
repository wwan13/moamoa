---
name: moamoa-crawler-source
description: Use when adding or updating a Python source crawler in moamoa-crawler, especially when the user provides a source key, blog URL, or asks that a new crawler be picked up automatically from sources/{key}.py.
---

# Moamoa Crawler Source

## Scope

Lock `moamoa-crawler` for application changes. Keep edits inside `moamoa-crawler/**`.

Use this skill for the standalone Python crawler package, not the Kotlin `infra/tech-blog-*` modules. For Kotlin tech-blog source work, use the existing backend/tech-blog agent rules instead.

## Source Layout

Crawler sources live in:

```text
moamoa-crawler/sources/{key}.py
```

`{key}` is the public crawler key used by CLI/API calls:

```bash
moamoa-crawler --once --key {key}
curl -X POST 'http://127.0.0.1:8765/crawl?key={key}&size=30'
```

Do not add a manual registry entry for new sources. `jobs.py` discovers supported keys from non-private `.py` files under the top-level `sources/` directory and imports only the requested key at execution time.

## Required Module Contract

Each source module must expose:

```python
def crawl(request, config) -> dict[str, object]:
    ...
```

Use `request.size` for the requested maximum post count. Use `config.headless`, `config.wait`, `config.scrolls`, and `config.scroll_wait` when the source needs browser crawling.

Return a JSON-serializable dict with the existing payload shape:

```python
{
    "key": request.key,
    "blog": "Blog display name",
    "baseUrl": "https://example.com",
    "requestedUrl": "https://example.com/posts",
    "crawler": "crawler implementation name",
    "crawledAt": datetime.now(timezone.utc).isoformat(),
    "requestedSize": request.size,
    "postCount": len(posts),
    "posts": posts,
}
```

Posts should include at least `title`, `url`, and `source`, matching the existing `BlogPost` dataclass when that shape is sufficient.

## Implementation Rules

Prefer stable APIs or static HTML parsing when available. Use Scrapling browser crawling only when the source needs rendered DOM behavior.

For Medium-family publications or Medium-hosted custom domains, prefer Scrapling browser crawling over RSS when the task is to match what is visibly rendered on the publication list page.

Keep source-specific URL rules, selectors, parsing, paging, and stop conditions inside the `{key}.py` module. Do not broaden shared crawler abstractions unless the user explicitly asks for it.

Avoid generated or hash-like CSS selectors. Prefer href patterns, semantic tags, stable attributes, and URL-derived keys.

When the list page already shows the needed fields, do not fetch detail pages just to re-derive them. Extract `title`, `description`, `thumbnail`, and `url` directly from the list card and keep the crawler single-page when possible.

Prefer element-role analysis over broad text heuristics. Identify the post card first, then pull fields from stable semantic elements inside that card such as:

- main post link for `url`
- primary heading/link text for `title`
- secondary heading such as `h3` for `description` when the publication uses a subtitle/deck line
- `img` inside the same card for `thumbnail`

Only fall back to broader text scanning when a field cannot be obtained from stable elements.

Raise a clear error if the crawl succeeds technically but extracts no post links. Include the source key or requested URL in source-specific failure messages when helpful.

## Verification

Run at least:

```bash
python3 -m compileall -q moamoa-crawler/crawler.py moamoa-crawler/jobs.py moamoa-crawler/service.py moamoa-crawler/sources
python3 - <<'PY'
from pathlib import Path
import sys
sys.path.insert(0, str(Path("moamoa-crawler").resolve()))
from jobs import supported_keys
print(supported_keys())
PY
```

Run a real crawl only when the environment has the needed browser/fetcher dependencies installed or when the user asks for runtime verification.
