# Development Progress

Updated: 2026-10-28

## Todo
- [ ] M8: Add character identification mode before GPU support
  - Status: in progress
  - Implementation: `tools/build_characters.py` builds a candidate catalog from the existing Danbooru data; it preserves ambiguous English names and does not guess series relationships. Two Japanese aliases are seeds only. The catalog is not yet integrated into the app.
  - Verification: `python3 tools/test_characters.py` passed 4 tests; full local catalog built with 40,931 characters and 88,603 aliases, SQLite integrity check passed. Android build and device verification not run.
- [ ] Add DuckDuckGo lookup for the top two organic results, with privacy disclosure and graceful failure.
  - Status: in progress
  - Implementation: `tools/character_search_spike.py` is a Python-only parser prototype; it shows result titles and URLs, not verified names. It is not integrated into the Android app.
  - Verification: parser unit tests passed; live prototype returned no extractable results in this environment, so live behavior is not verified.
- [ ] Implement character UI, reliable Japanese aliases for NIKKE and ten other works, identity preservation, history migration, and DuckDuckGo fallback.
  - Status: not started
  - Verification: not run

## Completed
- [x] Cloud build and Android baseline smoke test for existing v13.1
  - Implementation: GitHub Actions workflow dispatch `40671369534` built `main` at `c5b6a05`; installed debug APK with `adb install -r` on `127.0.0.1:5555`.
  - Verification: M1 matcher tests, Kotlin unit tests, and APK build passed in CI. On device, history → conversion → local refine succeeded for cases 20, 24 and 27; outputs were 232, 216 and 230 words, with no detected refusal or Japanese. All three refinements used existing cached results; fresh local model generation and M8 UI were not tested. Test created history rows 156–158.

## Next session
- Next action: wire the catalog into character selection and generation, without deleting existing history.
- Blockers: dependable Japanese alias sources and DuckDuckGo integration still require validation; the trial catalog is approximately 15 MiB. The user confirmed that “2” means the top two search results.
