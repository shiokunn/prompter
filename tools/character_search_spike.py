#!/usr/bin/env python3
"""Reference spike for DuckDuckGo's first two organic result titles.

This is not an app integration. Search result titles are evidence, not verified
character names: a title may be Japanese or unrelated to the queried person.
"""
from __future__ import annotations

import argparse
import sys
from html.parser import HTMLParser
from urllib.parse import parse_qs, urlencode, urljoin, urlparse
from urllib.request import Request, urlopen


class Results(HTMLParser):
    def __init__(self) -> None:
        super().__init__(convert_charrefs=True)
        self.results: list[tuple[str, str]] = []
        self._seen = 0
        self._href: str | None = None
        self._text: list[str] = []

    def handle_starttag(self, tag: str, attrs: list[tuple[str, str | None]]) -> None:
        values = dict(attrs)
        if tag == "a" and "result__a" in (values.get("class") or "").split():
            self._href = values.get("href") or ""
            self._text = []

    def handle_data(self, data: str) -> None:
        if self._href is not None:
            self._text.append(data)

    def handle_endtag(self, tag: str) -> None:
        if tag != "a" or self._href is None:
            return
        self._seen += 1
        title = " ".join("".join(self._text).split())
        href = urljoin("https://duckduckgo.com", self._href)
        parsed = urlparse(href)
        # DDG wraps external targets in a redirect. Do not accept arbitrary
        # schemes or turn a page title into a claimed character identity.
        if parsed.hostname in {"duckduckgo.com", "www.duckduckgo.com"} and parsed.path == "/l/":
            href = parse_qs(parsed.query).get("uddg", [""])[0]
        target = urlparse(href)
        if self._seen <= 2 and title and target.scheme in {"http", "https"} and target.hostname:
            self.results.append((title, href))
        self._href = None
        self._text = []


def first_two(html: str) -> list[tuple[str, str]]:
    parser = Results()
    parser.feed(html)
    return parser.results[:2]


def main() -> None:
    parser = argparse.ArgumentParser(description="Experimental public search; query is sent to DuckDuckGo")
    parser.add_argument("name")
    parser.add_argument("series", nargs="?", default="")
    args = parser.parse_args()
    query = " ".join(part for part in (args.name.strip(), args.series.strip(), "character english name") if part)
    request = Request("https://html.duckduckgo.com/html/?" + urlencode({"q": query}),
                      headers={"User-Agent": "Mozilla/5.0"})
    with urlopen(request, timeout=15) as response:
        html = response.read(2_000_000).decode("utf-8", errors="replace")
    results = first_two(html)
    if not results:
        print("No search results could be extracted; DuckDuckGo may have returned a challenge or changed its page.", file=sys.stderr)
        raise SystemExit(1)
    for title, url in results:
        print(f"{title}\n{url}\n")


if __name__ == "__main__":
    main()
