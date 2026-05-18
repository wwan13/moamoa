from __future__ import annotations

from _api_sources import crawl_nhncloud


def crawl(request, config) -> dict[str, object]:
    del config
    return crawl_nhncloud(request)
