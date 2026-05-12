from __future__ import annotations

import argparse
import json
import logging
import time
from dataclasses import dataclass
from pathlib import Path

DEFAULT_OUTPUT_DIR = Path("data")


@dataclass(frozen=True)
class BlogPost:
    title: str
    url: str
    source: str


def write_json(payload: dict[str, object], output: Path) -> None:
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(json.dumps(payload, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def run_once(
    key: str,
    size: int,
    output_dir: Path,
    headless: bool,
    wait: int,
    scrolls: int,
    scroll_wait: int,
) -> None:
    from jobs import CrawlJobConfig, CrawlJobRequest, output_path, run_crawl_job

    request = CrawlJobRequest(key=key, size=size)
    config = CrawlJobConfig(
        output_dir=output_dir,
        headless=headless,
        wait=wait,
        scrolls=scrolls,
        scroll_wait=scroll_wait,
    )
    result = run_crawl_job(request, config)
    logging.info("wrote %s posts to %s", result["postCount"], output_path(output_dir, key))


def positive_int(value: str) -> int:
    parsed = int(value)
    if parsed <= 0:
        raise argparse.ArgumentTypeError("must be a positive integer")
    return parsed


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Run browser crawlers with Scrapling.")
    parser.add_argument("--key", default="musinsa", help="Crawler key")
    parser.add_argument("--size", type=positive_int, default=30, help="Maximum number of posts to return")
    parser.add_argument("--output-dir", type=Path, default=DEFAULT_OUTPUT_DIR, help="JSON output directory")
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
    parser.add_argument("--once", action="store_true", help="Run once and exit")
    parser.add_argument("--interval", type=positive_int, help="Run continuously every N seconds")
    parser.add_argument("--headful", action="store_true", help="Show the browser window")
    parser.add_argument("--wait", type=positive_int, default=3000, help="Extra wait time in milliseconds")
    parser.add_argument("--scrolls", type=positive_int, default=8, help="Number of browser scrolls on /all")
    parser.add_argument("--scroll-wait", type=positive_int, default=1200, help="Wait after each scroll in milliseconds")
    return parser.parse_args()


def main() -> None:
    logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(message)s")
    args = parse_args()

    if not args.once and not args.interval and not args.serve and not args.redis_stream:
        args.once = True

    headless = not args.headful
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
            output_dir=args.output_dir,
            headless=headless,
            wait=args.wait,
            scrolls=args.scrolls,
            scroll_wait=args.scroll_wait,
        )
        return

    if args.serve:
        from jobs import CrawlJobConfig
        from service import serve

        serve(
            host=args.host,
            port=args.port,
            config=CrawlJobConfig(
                output_dir=args.output_dir,
                headless=headless,
                wait=args.wait,
                scrolls=args.scrolls,
                scroll_wait=args.scroll_wait,
            ),
        )
        return

    if args.once:
        run_once(
            key=args.key,
            size=args.size,
            output_dir=args.output_dir,
            headless=headless,
            wait=args.wait,
            scrolls=args.scrolls,
            scroll_wait=args.scroll_wait,
        )
        return

    while True:
        try:
            run_once(
                key=args.key,
                size=args.size,
                output_dir=args.output_dir,
                headless=headless,
                wait=args.wait,
                scrolls=args.scrolls,
                scroll_wait=args.scroll_wait,
            )
        except Exception:
            logging.exception("crawl failed")
        time.sleep(args.interval)


if __name__ == "__main__":
    main()
