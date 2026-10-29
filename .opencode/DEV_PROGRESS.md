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
- [x] Curate character entries for the requested ten works with source and coverage checks.
  - Status: complete for the recorded scope
  - Implementation: `character.md` is the reviewable source of truth. It contains 621 records across the ten works, with per-work source and coverage rows plus an explicit completion-criteria section. Alternate forms, costumes, personas and units are separate records (`category=variant`); Genshin NPCs are excluded per user instruction. `tools/build_character_db.py` generates `app/src/main/assets/characterdb.sqlite` (series / characters / aliases / meta, 1,550 alias rows) from that file.
  - Verification: Wuthering.gg's full 61-entry display was reconciled with no missing rows. NIKKE combines the JP name list, Nikke.gg playable roster and 2026 official announcements; Aigis was the only visible Nikke.gg entry missing. `CharacterDb` lookups (Japanese name, English name, nickname, reading, width/case normalization, variant separation) pass Robolectric tests in GitHub Actions run `40857989940`, which also built the APK. English official-localization status, unverified NIKKE Japanese names and public-redistribution terms remain documented as open items, not silently assumed.
- [x] Add bundled character database with alias search to the character UI.
  - Implementation: `CharacterDb.search` matches canonical names, short names, readings and same-form aliases, returning series + category + source note. MainActivity shows "登録済みの候補" while typing; choosing one still requires the explicit "この英語名を確認して使う" action.
  - Verification: covered by `CharacterDbTest` in the CI run above. On-device behavior has not yet been observed.
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
