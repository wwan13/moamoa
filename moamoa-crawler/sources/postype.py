from __future__ import annotations

from _api_sources import crawl_postype


def crawl(request, config) -> dict[str, object]:
    return crawl_postype(request)
