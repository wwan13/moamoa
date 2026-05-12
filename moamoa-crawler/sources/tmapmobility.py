from __future__ import annotations

from _api_sources import crawl_tmapmobility


def crawl(request, config) -> dict[str, object]:
    return crawl_tmapmobility(request)
