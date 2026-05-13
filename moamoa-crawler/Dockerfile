FROM ghcr.io/d4vinci/scrapling:latest

WORKDIR /app

ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    CRAWLER_HOST=0.0.0.0 \
    CRAWLER_PORT=8765 \
    CRAWLER_SERVE=true \
    CRAWLER_REDIS_STREAM=true \
    CRAWLER_STREAM=tech-blog:crawl:requests \
    CRAWLER_GROUP=moamoa-crawler \
    CRAWLER_REDIS_BLOCK_MS=5000 \
    CRAWLER_REDIS_COUNT=1 \
    CRAWLER_WAIT=3000 \
    CRAWLER_SCROLLS=8 \
    CRAWLER_SCROLL_WAIT=1200

COPY pyproject.toml README.md ./
COPY __main__.py crawler.py jobs.py redis_stream.py service.py ./
COPY sources ./sources

RUN python -m pip install --no-cache-dir ".[fetchers]"

EXPOSE 8765

HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
  CMD sh -c 'if [ "${CRAWLER_SERVE:-true}" != "true" ]; then exit 0; fi; python -c "import urllib.request; urllib.request.urlopen(\"http://127.0.0.1:8765/health\", timeout=3)"'

CMD sh -c 'set -eu; args=""; if [ "${CRAWLER_SERVE:-true}" = "true" ]; then args="$args --serve"; fi; if [ "${CRAWLER_REDIS_STREAM:-true}" = "true" ]; then args="$args --redis-stream"; fi; args="$args --host ${CRAWLER_HOST:-0.0.0.0} --port ${CRAWLER_PORT:-8765} --stream ${CRAWLER_STREAM:-tech-blog:crawl:requests} --group ${CRAWLER_GROUP:-moamoa-crawler} --consumer ${CRAWLER_CONSUMER:-moamoa-crawler-$(hostname)} --redis-block-ms ${CRAWLER_REDIS_BLOCK_MS:-5000} --redis-count ${CRAWLER_REDIS_COUNT:-1} --wait ${CRAWLER_WAIT:-3000} --scrolls ${CRAWLER_SCROLLS:-8} --scroll-wait ${CRAWLER_SCROLL_WAIT:-1200}"; if [ -n "${CRAWLER_REDIS_URL:-}" ]; then args="$args --redis-url ${CRAWLER_REDIS_URL}"; fi; if [ "${CRAWLER_HEADFUL:-false}" = "true" ]; then args="$args --headful"; fi; if [ "${CRAWLER_REDIS_ACK_ON_FAILURE:-false}" = "true" ]; then args="$args --redis-ack-on-failure"; fi; exec python -m crawler $args'
