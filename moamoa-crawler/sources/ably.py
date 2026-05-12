from __future__ import annotations

from _api_sources import crawl_ably


def crawl(request, config) -> dict[str, object]:
    return crawl_ably(request)
