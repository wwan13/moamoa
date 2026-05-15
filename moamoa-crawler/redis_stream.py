from __future__ import annotations

import json
import logging
import os
import socket
import time
import urllib.parse
from dataclasses import dataclass
from pathlib import Path
from typing import Any, Callable

from jobs import DEFAULT_SIZE, CrawlJobConfig, CrawlJobRequest, run_crawl_job, supported_keys


@dataclass(frozen=True)
class RedisStreamConfig:
    redis_url: str
    stream: str
    response_stream: str
    group: str
    consumer: str
    block_ms: int
    count: int
    ack_on_failure: bool


@dataclass(frozen=True)
class RedisConnectionInfo:
    host: str
    port: int
    db: int
    username: str | None
    password: str | None


class RedisProtocolError(RuntimeError):
    pass


class RedisClient:
    def __init__(self, url: str) -> None:
        self.info = parse_redis_url(url)
        self._socket: socket.socket | None = None
        self._file: Any | None = None

    def __enter__(self) -> "RedisClient":
        self.connect()
        return self

    def __exit__(self, exc_type: object, exc: object, tb: object) -> None:
        self.close()

    def connect(self) -> None:
        self.close()
        self._socket = socket.create_connection((self.info.host, self.info.port), timeout=10)
        self._file = self._socket.makefile("rb")
        if self.info.password:
            if self.info.username:
                self.command("AUTH", self.info.username, self.info.password)
            else:
                self.command("AUTH", self.info.password)
        if self.info.db:
            self.command("SELECT", str(self.info.db))

    def close(self) -> None:
        if self._file is not None:
            self._file.close()
            self._file = None
        if self._socket is not None:
            self._socket.close()
            self._socket = None

    def command(self, *parts: object) -> Any:
        if self._socket is None or self._file is None:
            self.connect()
        assert self._socket is not None
        encoded = encode_command(parts)
        self._socket.sendall(encoded)
        return self._read_response()

    def _read_response(self) -> Any:
        assert self._file is not None
        prefix = self._file.read(1)
        if not prefix:
            raise RedisProtocolError("redis connection closed")

        if prefix == b"+":
            return self._read_line().decode("utf-8", errors="replace")
        if prefix == b"-":
            message = self._read_line().decode("utf-8", errors="replace")
            raise RedisProtocolError(message)
        if prefix == b":":
            return int(self._read_line())
        if prefix == b"$":
            size = int(self._read_line())
            if size == -1:
                return None
            data = self._file.read(size)
            self._file.read(2)
            return data
        if prefix == b"*":
            size = int(self._read_line())
            if size == -1:
                return None
            return [self._read_response() for _ in range(size)]

        raise RedisProtocolError(f"unsupported redis response prefix: {prefix!r}")

    def _read_line(self) -> bytes:
        assert self._file is not None
        line = self._file.readline()
        if not line:
            raise RedisProtocolError("redis connection closed")
        return line.rstrip(b"\r\n")


def parse_redis_url(url: str) -> RedisConnectionInfo:
    parsed = urllib.parse.urlsplit(url)
    if parsed.scheme not in {"redis", ""}:
        raise ValueError("only redis:// URLs are supported")
    db = int(parsed.path.strip("/") or "0")
    return RedisConnectionInfo(
        host=parsed.hostname or "127.0.0.1",
        port=parsed.port or 6379,
        db=db,
        username=urllib.parse.unquote(parsed.username) if parsed.username else None,
        password=urllib.parse.unquote(parsed.password) if parsed.password else None,
    )


def load_dotenv(path: Path) -> dict[str, str]:
    values: dict[str, str] = {}
    if not path.exists():
        return values

    for line in path.read_text(encoding="utf-8").splitlines():
        stripped = line.strip()
        if not stripped or stripped.startswith("#") or "=" not in stripped:
            continue
        key, value = stripped.split("=", 1)
        values[key.strip()] = value.strip().strip('"').strip("'")
    return values


def default_dotenv_paths() -> list[Path]:
    module_root = Path(__file__).resolve().parent
    return [Path(".env"), module_root.parent / ".env"]


def redis_url_from_env(dotenv_path: Path | None = None) -> str:
    dotenv_paths = [dotenv_path] if dotenv_path else default_dotenv_paths()
    env_file_values: dict[str, str] = {}
    for path in dotenv_paths:
        env_file_values.update(load_dotenv(path))

    def get(name: str, default: str = "") -> str:
        return os.environ.get(name) or env_file_values.get(name) or default

    explicit_url = get("REDIS_URL")
    if explicit_url:
        return explicit_url

    host = get("REDIS_HOST", "127.0.0.1")
    port = get("REDIS_PORT", "6379")
    database = get("REDIS_DATABASE", "0")
    password = get("REDIS_PASSWORD")

    auth = f":{urllib.parse.quote(password, safe='')}@" if password else ""
    return f"redis://{auth}{host}:{port}/{database}"


def encode_command(parts: tuple[object, ...]) -> bytes:
    chunks = [f"*{len(parts)}\r\n".encode("ascii")]
    for part in parts:
        data = str(part).encode("utf-8")
        chunks.append(f"${len(data)}\r\n".encode("ascii"))
        chunks.append(data + b"\r\n")
    return b"".join(chunks)


def ensure_group(client: RedisClient, stream: str, group: str) -> None:
    try:
        client.command("XGROUP", "CREATE", stream, group, "$", "MKSTREAM")
        logging.info("created redis stream group: stream=%s group=%s", stream, group)
    except RedisProtocolError as exc:
        if "BUSYGROUP" not in str(exc):
            raise


RunCrawl = Callable[[CrawlJobRequest], dict[str, object]]


def consume(redis_config: RedisStreamConfig, crawl_config: CrawlJobConfig, run_crawl: RunCrawl | None = None) -> None:
    crawl = run_crawl or (lambda request: run_crawl_job(request, crawl_config))

    with RedisClient(redis_config.redis_url) as client:
        ensure_group(client, redis_config.stream, redis_config.group)
        logging.info(
            "redis stream consumer started: stream=%s responseStream=%s group=%s consumer=%s",
            redis_config.stream,
            redis_config.response_stream,
            redis_config.group,
            redis_config.consumer,
        )

        while True:
            try:
                response = client.command(
                    "XREADGROUP",
                    "GROUP",
                    redis_config.group,
                    redis_config.consumer,
                    "COUNT",
                    redis_config.count,
                    "BLOCK",
                    redis_config.block_ms,
                    "STREAMS",
                    redis_config.stream,
                    ">",
                )
                for message_id, fields in parse_xreadgroup_response(response):
                    handle_message(client, redis_config, message_id, fields, crawl)
            except (OSError, RedisProtocolError):
                logging.exception("redis stream consumer disconnected; reconnecting")
                time.sleep(3)
                client.connect()


def parse_xreadgroup_response(response: Any) -> list[tuple[str, dict[str, str]]]:
    if not response:
        return []

    messages: list[tuple[str, dict[str, str]]] = []
    for stream_entry in response:
        if not isinstance(stream_entry, list) or len(stream_entry) != 2:
            continue
        for message in stream_entry[1] or []:
            if not isinstance(message, list) or len(message) != 2:
                continue
            message_id = decode(message[0])
            raw_fields = message[1] or []
            fields: dict[str, str] = {}
            for index in range(0, len(raw_fields), 2):
                fields[decode(raw_fields[index])] = decode(raw_fields[index + 1])
            messages.append((message_id, fields))
    return messages


def decode(value: object) -> str:
    if isinstance(value, bytes):
        return value.decode("utf-8", errors="replace")
    return str(value)


def handle_message(
    client: RedisClient,
    redis_config: RedisStreamConfig,
    message_id: str,
    fields: dict[str, str],
    run_crawl: RunCrawl,
) -> None:
    try:
        requests = crawl_requests_from_fields(fields)
        for request in requests:
            result = run_crawl(request)
            publish_response(client, redis_config.response_stream, result)
            logging.info("redis crawl completed: messageId=%s key=%s posts=%s", message_id, request.key, result["postCount"])
        client.command("XACK", redis_config.stream, redis_config.group, message_id)
    except Exception:
        logging.exception("redis crawl failed: messageId=%s fields=%s", message_id, fields)
        if redis_config.ack_on_failure:
            client.command("XACK", redis_config.stream, redis_config.group, message_id)


def publish_response(client: RedisClient, response_stream: str, payload: dict[str, object]) -> None:
    client.command(
        "XADD",
        response_stream,
        "*",
        "payload",
        json.dumps(payload, ensure_ascii=False),
    )


def crawl_requests_from_fields(fields: dict[str, str]) -> list[CrawlJobRequest]:
    payload = fields.get("payload")
    if payload:
        parsed = json.loads(payload)
        if not isinstance(parsed, dict):
            raise ValueError("redis payload must be a JSON object")
        fields = {**fields, **{str(key): str(value) for key, value in parsed.items()}}

    key = (fields.get("key") or "").strip()
    if not key:
        raise ValueError("redis crawl event requires key")

    size = parse_size(fields.get("size"))
    if key == "*":
        return [CrawlJobRequest(key=source_key, size=size) for source_key in supported_keys()]
    return [CrawlJobRequest(key=key, size=size)]


def parse_size(value: str | None) -> int:
    if value is None or value == "":
        return DEFAULT_SIZE
    parsed = int(value)
    if parsed <= 0:
        raise ValueError("size must be a positive integer")
    return parsed


def run_redis_stream(
    *,
    redis_url: str,
    stream: str,
    response_stream: str,
    group: str,
    consumer: str,
    block_ms: int,
    count: int,
    ack_on_failure: bool,
    headless: bool,
    wait: int,
    scrolls: int,
    scroll_wait: int,
    run_crawl: RunCrawl | None = None,
) -> None:
    consume(
        redis_config=RedisStreamConfig(
            redis_url=redis_url,
            stream=stream,
            response_stream=response_stream,
            group=group,
            consumer=consumer,
            block_ms=block_ms,
            count=count,
            ack_on_failure=ack_on_failure,
        ),
        crawl_config=CrawlJobConfig(
            headless=headless,
            wait=wait,
            scrolls=scrolls,
            scroll_wait=scroll_wait,
        ),
        run_crawl=run_crawl,
    )
