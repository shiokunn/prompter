# DuckDuckGo character lookup diagnosis (2026-10-29)

Public reproduction query: `シンデレラ NIKKE character english name`, User-Agent `Mozilla/5.0`.

| Endpoint | HTTP | Observed response |
| --- | --- | --- |
| `https://html.duckduckgo.com/html/` | 202 | 14,284 characters, `challenge-form` and `anomaly.js`, no `result__a` links |
| `https://lite.duckduckgo.com/lite/` | 200 | 23,697 characters, ten `result-link` entries, no challenge form |

The old implementation accepted every 2xx status and parsed only `a.result__a`.
Consequently a bot-challenge page became a generic “no extractable results” error.
It was not evidence of a missing character or an unsupported Japanese query.
`CharacterSearch` was also no longer connected to the picker after AniList was introduced.

The app now uses the lightweight endpoint, recognizes both result layouts, and
distinguishes authentication/rate limits, a genuine empty search, unknown markup,
HTTP failure and oversized responses. It does not solve/bypass challenges or retry
them automatically. Users can explicitly continue in their browser. Search titles
remain source evidence and are never confirmed English character identities.

These observations are point-in-time and network-specific: Lite can also require
authentication later. Deterministic regression fixtures cover that case.
