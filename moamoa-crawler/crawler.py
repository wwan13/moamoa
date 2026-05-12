from __future__ import annotations

import argparse
import logging
import threading
from dataclasses import dataclass


@dataclass(frozen=True)
class BlogPost:
    title: str
    url: str
    source: str


def positive_int(value: str) -> int:
    parsed = int(value)
    if parsed <= 0:
        raise argparse.ArgumentTypeError("must be a positive integer")
    return parsed


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Run browser crawlers with Scrapling.")
    parser.add_argument("--serve", action="store_true", help="Run an HTTP API server")
    parser.add_argument("--redis-stream", action="store_true", help="Run a Redis Stream event consumer")
    parser.add_argument("--redis-url", help="Redis URL for --redis-stream. Defaults to REDIS_URL or REDIS_HOST/REDIS_PORT/REDIS_DATABASE/REDIS_PASSWORD from .env")
    parser.add_argument("--stream", default="tech-blog:crawl:requests", help="Redis Stream name")
    parser.add_argument("--group", default="moamoa-crawler", help="Redis Stream consumer group")
    parser.add_argument("--consumer", default="moamoa-crawler-1", help="Redis Stream consumer name")
    parser.add_argument("--redis-block-ms", type=positive_int, default=5000, help="Redis XREADGROUP block time in milliseconds")
    parser.add_argument("--redis-count", type=positive_int, default=1, help="Redis XREADGROUP max message count")
    parser.add_argument("--redis-ack-on-failure", action="store_true", help="Acknowledge failed Redis Stream messages")
    parser.add_argument("--host", default="127.0.0.1", help="API server host")
    parser.add_argument("--port", type=positive_int, default=8765, help="API server port")
    parser.add_argument("--headful", action="store_true", help="Show the browser window")
    parser.add_argument("--wait", type=positive_int, default=3000, help="Extra wait time in milliseconds")
    parser.add_argument("--scrolls", type=positive_int, default=8, help="Number of browser scrolls on /all")
    parser.add_argument("--scroll-wait", type=positive_int, default=1200, help="Wait after each scroll in milliseconds")
    return parser.parse_args()


def main() -> None:
    logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(message)s")
    args = parse_args()

    if not args.serve and not args.redis_stream:
        args.serve = True
        args.redis_stream = True

    headless = not args.headful
    from jobs import CrawlJobConfig

    crawl_config = CrawlJobConfig(
        headless=headless,
        wait=args.wait,
        scrolls=args.scrolls,
        scroll_wait=args.scroll_wait,
    )

    if args.serve and args.redis_stream:
        from http import HTTPStatus

        from redis_stream import redis_url_from_env, run_redis_stream
        from service import CrawlerApi, serve_api

        api = CrawlerApi(crawl_config)
        api_thread = threading.Thread(
            target=serve_api,
            kwargs={"host": args.host, "port": args.port, "api": api},
            name="crawler-api-server",
            daemon=True,
        )
        api_thread.start()

        def run_crawl_from_queue(request):
            status, payload = api.crawl_now(request)
            if status != HTTPStatus.OK:
                raise RuntimeError(payload.get("error") or f"crawl failed with status {status}")
            return payload

        run_redis_stream(
            redis_url=args.redis_url or redis_url_from_env(),
            stream=args.stream,
            group=args.group,
            consumer=args.consumer,
            block_ms=args.redis_block_ms,
            count=args.redis_count,
            ack_on_failure=args.redis_ack_on_failure,
            headless=headless,
            wait=args.wait,
            scrolls=args.scrolls,
            scroll_wait=args.scroll_wait,
            run_crawl=run_crawl_from_queue,
        )
        return

    if args.redis_stream:
        from redis_stream import redis_url_from_env, run_redis_stream

        run_redis_stream(
            redis_url=args.redis_url or redis_url_from_env(),
            stream=args.stream,
            group=args.group,
            consumer=args.consumer,
            block_ms=args.redis_block_ms,
            count=args.redis_count,
            ack_on_failure=args.redis_ack_on_failure,
            headless=headless,
            wait=args.wait,
            scrolls=args.scrolls,
            scroll_wait=args.scroll_wait,
        )
        return

    if args.serve:
        from service import serve

        serve(
            host=args.host,
            port=args.port,
            config=crawl_config,
        )


if __name__ == "__main__":
    main()
