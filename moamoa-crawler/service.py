from __future__ import annotations

import json
import logging
import queue
import threading
import urllib.parse
from dataclasses import dataclass, field
from http import HTTPStatus
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

from jobs import DEFAULT_KEY, DEFAULT_SIZE, CrawlJobConfig, CrawlJobRequest, run_crawl_job, supported_keys


@dataclass
class QueuedCrawl:
    request: CrawlJobRequest
    done: threading.Event = field(default_factory=threading.Event)
    status: HTTPStatus | None = None
    payload: dict[str, object] | None = None


class CrawlerApi:
    def __init__(self, config: CrawlJobConfig) -> None:
        self.config = config
        self._lock = threading.RLock()
        self._crawl_queue: queue.Queue[QueuedCrawl] = queue.Queue()
        self._worker = threading.Thread(target=self._consume_crawl_queue, name="crawler-api-worker", daemon=True)
        self._latest_results: dict[str, dict[str, object]] = {}
        self._latest_error: str | None = None
        self._running = False
        self._worker.start()

    def health(self) -> dict[str, object]:
        with self._lock:
            return {
                "status": "ok",
                "running": self._running,
                "queueSize": self._crawl_queue.qsize(),
                "latestError": self._latest_error,
                "supportedKeys": supported_keys(),
            }

    def crawl_now(self, request: CrawlJobRequest) -> tuple[HTTPStatus, dict[str, object]]:
        job = QueuedCrawl(request=request)
        self._crawl_queue.put(job)
        logging.info("crawl queued: key=%s queueSize=%s", request.key, self._crawl_queue.qsize())
        job.done.wait()
        return job.status or HTTPStatus.INTERNAL_SERVER_ERROR, job.payload or {"error": "crawl queue failed"}

    def _consume_crawl_queue(self) -> None:
        while True:
            job = self._crawl_queue.get()
            try:
                job.status, job.payload = self._run_crawl(job.request)
            finally:
                job.done.set()
                self._crawl_queue.task_done()

    def _run_crawl(self, request: CrawlJobRequest) -> tuple[HTTPStatus, dict[str, object]]:
        try:
            with self._lock:
                self._running = True
                self._latest_error = None

            result = run_crawl_job(request, self.config)

            with self._lock:
                self._latest_results[request.key] = result
                self._latest_error = None

            logging.info("crawl completed: key=%s posts=%s queueSize=%s", request.key, result["postCount"], self._crawl_queue.qsize())
            return HTTPStatus.OK, result
        except ValueError as exc:
            with self._lock:
                self._latest_error = str(exc)
            return HTTPStatus.BAD_REQUEST, {"error": str(exc)}
        except Exception as exc:
            with self._lock:
                self._latest_error = str(exc)
            logging.exception("crawl failed")
            return HTTPStatus.INTERNAL_SERVER_ERROR, {"error": str(exc)}
        finally:
            with self._lock:
                self._running = False


def json_response(handler: BaseHTTPRequestHandler, status: HTTPStatus, payload: dict[str, object]) -> None:
    body = json.dumps(payload, ensure_ascii=False, indent=2).encode("utf-8")
    handler.send_response(status)
    handler.send_header("Content-Type", "application/json; charset=utf-8")
    handler.send_header("Content-Length", str(len(body)))
    handler.end_headers()
    handler.wfile.write(body)


def positive_int(value: object, default: int) -> int:
    if value is None or value == "":
        return default
    try:
        parsed = int(value)
    except (TypeError, ValueError) as exc:
        raise ValueError("size must be a positive integer") from exc
    if parsed <= 0:
        raise ValueError("size must be a positive integer")
    return parsed


def parse_json_body(handler: BaseHTTPRequestHandler) -> dict[str, object]:
    content_length = int(handler.headers.get("Content-Length", "0"))
    if content_length <= 0:
        return {}
    body = handler.rfile.read(content_length)
    if not body:
        return {}
    parsed = json.loads(body.decode("utf-8"))
    if not isinstance(parsed, dict):
        raise ValueError("request body must be a JSON object")
    return parsed


def crawl_request_from_http(handler: BaseHTTPRequestHandler) -> CrawlJobRequest:
    parsed_url = urllib.parse.urlsplit(handler.path)
    query = urllib.parse.parse_qs(parsed_url.query)
    body = parse_json_body(handler)

    key = str(body.get("key") or query.get("key", [DEFAULT_KEY])[0] or DEFAULT_KEY)
    size_value = body.get("size")
    if size_value is None:
        size_value = query.get("size", [DEFAULT_SIZE])[0]

    return CrawlJobRequest(key=key, size=positive_int(size_value, DEFAULT_SIZE))


def handle_crawl_request(api: CrawlerApi, handler: BaseHTTPRequestHandler) -> tuple[HTTPStatus, dict[str, object]]:
    try:
        request = crawl_request_from_http(handler)
    except (json.JSONDecodeError, ValueError) as exc:
        return HTTPStatus.BAD_REQUEST, {"error": str(exc)}
    return api.crawl_now(request)


def make_handler(api: CrawlerApi) -> type[BaseHTTPRequestHandler]:
    class Handler(BaseHTTPRequestHandler):
        def log_message(self, format: str, *args: object) -> None:
            logging.info("api %s - %s", self.address_string(), format % args)

        def do_GET(self) -> None:
            parsed = urllib.parse.urlsplit(self.path)
            if parsed.path == "/health":
                json_response(self, HTTPStatus.OK, api.health())
                return

            json_response(self, HTTPStatus.NOT_FOUND, {"error": "not found"})

        def do_POST(self) -> None:
            parsed = urllib.parse.urlsplit(self.path)
            if parsed.path != "/crawl":
                json_response(self, HTTPStatus.NOT_FOUND, {"error": "not found"})
                return

            status, payload = handle_crawl_request(api, self)
            json_response(self, status, payload)

    return Handler


def serve_api(host: str, port: int, api: CrawlerApi) -> None:
    server = ThreadingHTTPServer((host, port), make_handler(api))
    logging.info("crawler api listening on http://%s:%s", host, port)
    try:
        server.serve_forever()
    finally:
        server.server_close()


def serve(host: str, port: int, config: CrawlJobConfig) -> None:
    serve_api(host=host, port=port, api=CrawlerApi(config))
