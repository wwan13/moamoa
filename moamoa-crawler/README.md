# moamoa-crawler

Standalone Python crawler for tech blog sources using Scrapling browser crawling.

The MUSINSA crawler opens `https://techblog.musinsa.com/all` through Scrapling's browser fetcher, scrolls the rendered page, and extracts post links from the browser DOM.

## Run

```bash
python3.12 -m venv .venv
.venv/bin/python -m pip install -e ".[fetchers]"
.venv/bin/scrapling install
.venv/bin/moamoa-crawler --once --key musinsa --size 30
```

Open the API server:

```bash
.venv/bin/moamoa-crawler --serve --host 127.0.0.1 --port 8765
```

Run the Redis Stream consumer:

```bash
.venv/bin/moamoa-crawler --redis-stream \
  --stream tech-blog:crawl:requests \
  --group moamoa-crawler \
  --consumer crawler-1
```

Outputs are written to `data/{key}.posts.json` by default.

## API

```bash
curl -X POST 'http://127.0.0.1:8765/crawl?key=musinsa&size=30'
curl -X POST http://127.0.0.1:8765/crawl \
  -H 'Content-Type: application/json' \
  -d '{"key":"musinsa","size":30}'
curl http://127.0.0.1:8765/health
curl 'http://127.0.0.1:8765/posts?key=musinsa'
```

`POST /crawl` starts browser crawling immediately and returns the crawl result in the same response.

## Redis Stream

`--redis-stream` creates the configured consumer group with `MKSTREAM` if it does not already exist, then reads new messages with `XREADGROUP`.

Redis connection defaults are read from the repository `.env`:

```dotenv
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_DATABASE=1
REDIS_PASSWORD=root
```

`REDIS_URL` or `--redis-url` can override those values.

Publish a single-source crawl event:

```bash
redis-cli XADD tech-blog:crawl:requests '*' key toss size 30
```

Publish all supported sources:

```bash
redis-cli XADD tech-blog:crawl:requests '*' key '*' size 1
```

JSON payload events are also accepted:

```bash
redis-cli XADD tech-blog:crawl:requests '*' payload '{"key":"musinsa","size":30}'
```

Messages are acknowledged after successful crawl. Use `--redis-ack-on-failure` only when failed messages should not remain pending.

Supported crawler keys are discovered from `sources/{key}.py`.
The internal `run_crawl_job(request, config)` function is shared by CLI/API and can be reused by an event consumer later.

## Add a source

Add `sources/{key}.py` and expose a callable `crawl(request, config)` function.
No manual registration is needed.

```python
from __future__ import annotations

from datetime import datetime, timezone


def crawl(request, config) -> dict[str, object]:
    posts = []
    return {
        "key": request.key,
        "blog": "Your Tech Blog",
        "baseUrl": "https://example.com",
        "requestedUrl": "https://example.com/posts",
        "crawler": "your crawler name",
        "crawledAt": datetime.now(timezone.utc).isoformat(),
        "requestedSize": request.size,
        "postCount": len(posts),
        "posts": posts,
    }
```

## Options

```bash
.venv/bin/moamoa-crawler --help
```
