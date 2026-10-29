# Development Progress

Updated: 2026-10-28

## Todo
- [ ] Replace character search with AniList first, Wikidata fallback; keep explicit user confirmation.
  - Status: in progress
  - Implementation: `CharacterCatalogSearch.kt` queries AniList first and Wikidata on empty results; UI presents candidates and confirmation, clearing the chosen name if the input changes. Manual English entry remains available.
  - Verification: GitHub Actions `40843761261` passed Kotlin unit tests and APK build for commit `640387d`. APK installed via ADB on `127.0.0.1:5555`; live AniList Naruto lookup, candidate selection, offline prompt identity and history restore observed. Wikidata fallback, Japanese-name coverage and NIKKE lookup remain unverified.
- [ ] Implement signed GitHub Releases JSON dictionary updates imported transactionally into Room.
  - Status: in progress
  - Implementation: initial Ed25519 signature and payload validation added in `DictionaryUpdateVerifier.kt`, using the user's public key; private key not read. This is not connected to the UI, GitHub, Room, or matcher. Existing `PromptDb` is a read-only SQLite asset; user history must remain separate.
  - Verification: tests added but not run; genuine signed payload, Android API compatibility, rollback and transaction tests still needed. Do not publish as a functioning updater.
- [ ] Curate character entries for the requested ten works with source and coverage checks.
  - Status: in progress
  - Implementation: `character.md` tracks entries for all ten requested works; Genshin NPCs are excluded per user. Updated policy explicitly keeps alternate forms, costumes, personas and units as separate records. Added later Nikke.gg roster forms/collaborations and official 2026 Nikke forms, separated Honkai Star Rail forms and proxy identities, and checked current official-reference rosters for Gakumas/Muv-Luv plus a 61-entry Wuthering.gg roster.
  - Verification: still partial, not a complete roster. Nikke.gg English variant labels need verified Japanese localization; Wuthering.gg displays 61 entries while the current table needs a row-by-row reconciliation (including repeated Rover elemental listings). All sources need date/coverage audits, and full roster scope (except excluded Genshin NPCs), alias lookup and DB generation remain open.
- [ ] M8: Add character identification mode before GPU support
  - Status: in progress
  - Implementation: mode and name/series/confirmed English fields are integrated into `MainActivity.kt`; explicit English name is prefixed to offline and AI prose. Existing history migrates to v2 and saves character fields. No canonical character tag resolution or full Japanese catalog integration yet.
  - Verification: CI `40675540442` passed; APK 0.8.0-m8 installed on `127.0.0.1:5555`; manual Cinderella/Nikke entry, offline conversion, fresh local generation (229 words, 43.2 seconds) and history restore were observed. Existing pre-migration history was visible.
- [ ] Add DuckDuckGo lookup for the top two organic results, with privacy disclosure and graceful failure.
  - Status: in progress
  - Implementation: `CharacterSearch.kt` provides a bounded, timeout-limited search and two-result HTML parser. The UI displays source titles and links, not automatically confirmed names; users manually enter the English name.
  - Verification: parser unit tests passed in CI. Live Android search returned no extractable results and displayed an error. Search success remains unverified.
- [ ] Redesign the application UI while preserving existing behavior.
  - Status: in progress
  - Implementation: global palette and typography plus the main screen header and character section were refreshed; existing history/settings layouts have not been comprehensively redesigned. Pre-redesign state saved in commit `0923870`, branch `backup/pre-character-redesign`.
  - Verification: app launched, main screen and history were observed on device; full UI coverage and accessibility review not performed.
- [ ] Implement character UI, reliable Japanese aliases for NIKKE and ten other works, identity preservation, history migration, and DuckDuckGo fallback.
  - Status: not started
  - Verification: not run

## Completed
- [x] Cloud build and Android baseline smoke test for existing v13.1
  - Implementation: GitHub Actions workflow dispatch `40671369534` built `main` at `c5b6a05`; installed debug APK with `adb install -r` on `127.0.0.1:5555`.
  - Verification: M1 matcher tests, Kotlin unit tests, and APK build passed in CI. On device, history → conversion → local refine succeeded for cases 20, 24 and 27; outputs were 232, 216 and 230 words, with no detected refusal or Japanese. All three refinements used existing cached results; fresh local model generation and M8 UI were not tested. Test created history rows 156–158.

## Next session
- Next action: diagnose DuckDuckGo's empty/challenge response on Android, improve character identification and complete the history/settings UI redesign.
- Blockers: live search success unverified; Japanese alias catalog incomplete. The history DB was upgraded to v2: the pre-redesign v1 APK does not support DB downgrade, so do not clear app data or install the old APK as a rollback without a data-preserving migration plan.
