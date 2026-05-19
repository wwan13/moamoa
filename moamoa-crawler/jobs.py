from __future__ import annotations

from dataclasses import dataclass
import importlib.util
from pathlib import Path
import re
import sys
from typing import Callable


DEFAULT_KEY = "musinsa"
DEFAULT_SIZE: int | None = None
SOURCE_DIR = Path(__file__).resolve().parent / "sources"


@dataclass(frozen=True)
class CrawlJobConfig:
    headless: bool
    wait: int
    scrolls: int
    scroll_wait: int


@dataclass(frozen=True)
class CrawlJobRequest:
    key: str = DEFAULT_KEY
    size: int | None = DEFAULT_SIZE


CrawlerFunc = Callable[[CrawlJobRequest, CrawlJobConfig], dict[str, object]]


def source_keys() -> list[str]:
    names: list[str] = []
    if not SOURCE_DIR.exists():
        return names

    for source_path in SOURCE_DIR.glob("*.py"):
        if source_path.name.startswith("_"):
            continue
        names.append(source_path.stem)
    return names


def load_crawler(key: str) -> CrawlerFunc:
    source_path = SOURCE_DIR / f"{key}.py"
    module_name = re.sub(r"[^0-9a-zA-Z_]", "_", f"moamoa_crawler_source_{key}")
    spec = importlib.util.spec_from_file_location(module_name, source_path)
    if spec is None or spec.loader is None:
        raise ValueError(f"crawler source cannot be loaded: {key}")

    module = importlib.util.module_from_spec(spec)
    source_dir = str(SOURCE_DIR)
    added_source_dir = source_dir not in sys.path
    if added_source_dir:
        sys.path.insert(0, source_dir)
    try:
        sys.modules[module_name] = module
        spec.loader.exec_module(module)
    except Exception:
        sys.modules.pop(module_name, None)
        raise
    finally:
        if added_source_dir:
            sys.path.remove(source_dir)

    crawler = getattr(module, "crawl", None)
    if not callable(crawler):
        raise ValueError(f"crawler source has no callable crawl function: {key}")
    return crawler


def supported_keys() -> list[str]:
    return sorted(source_keys())


def resolve_supported_key(key: str) -> str | None:
    requested_key = key.strip()
    if not requested_key:
        return None

    requested_key_folded = requested_key.casefold()
    for supported_key in supported_keys():
        if supported_key.casefold() == requested_key_folded:
            return supported_key
    return None


def run_crawl_job(request: CrawlJobRequest, config: CrawlJobConfig) -> dict[str, object]:
    resolved_key = resolve_supported_key(request.key)
    if resolved_key is None:
        allowed = ", ".join(supported_keys())
        raise ValueError(f"unsupported crawler key: {request.key}; supported keys: {allowed}")

    resolved_request = CrawlJobRequest(key=resolved_key, size=request.size)
    crawler = load_crawler(resolved_request.key)
    return crawler(resolved_request, config)
