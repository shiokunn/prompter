# Development Progress

Updated: 2026-10-29

## Todo
- [ ] Diagnose DuckDuckGo transport/markup, improve work-aware character lookup, and finish history/settings.
  - Status: implementation complete; final post-polish CI and emulator pass pending.
  - Implementation: public reproduction confirmed HTML endpoint HTTP 202 challenge versus Lite HTTP 200 results; added `research/duckduckgo/diagnosis.md`, Lite/result selectors, ad filtering, challenge/no-results/markup/error distinctions and browser escape hatch. `CharacterCatalogSearch` now normalizes JP/Kana/EN work titles, ranks canonical/alternative names, filters candidates by requested work and falls back to Wikidata after AniList transport failure while rejecting apparent non-fictional people. Local `CharacterDb` now shares NFKC/kana normalization, safely matches literal query text and filters with work synonyms. Search requests have explicit consent, cancellation guards and ignore stale response generations. History is searchable, presents input/prompt/character details, asks before deletes and has safe-area padding; settings are grouped with sticky save, leave-with-unsaved-changes confirmation, safe model/key deletion, and loading/error feedback. Shared selection/tap semantics and touch targets were improved without touching stored history.
  - Verification so far: Python M1 suite passed (15 tests); live production `CharacterSearch` call returned two genuine Cinderella/NIKKE sources from Lite in the shell probe; latest CI before final safe-area adjustment is run `40959056382`, 115/115 Kotlin tests and APK build passed. Installed with `adb install -r`; light-mode 1200×2670 Android 16 emulator capture showed readable history/settings and retained prior history. History footer safe-area issue was caught in that visual pass and fixed; latest re-run still pending. No app data was cleared.
- [ ] Replace character search with AniList first, Wikidata fallback; keep explicit user confirmation.
  - Status: in progress
  - Implementation: `CharacterCatalogSearch.kt` queries AniList first and Wikidata on empty results; UI presents candidates and confirmation, clearing the chosen name if the input changes. Manual English entry remains available.
  - Verification: GitHub Actions `40843761261` passed Kotlin unit tests and APK build for commit `640387d`. APK installed via ADB on `127.0.0.1:5555`; live AniList Naruto lookup, candidate selection, offline prompt identity and history restore observed. Wikidata fallback, Japanese-name coverage and NIKKE lookup remain unverified.
- [x] Implement signed GitHub Releases JSON dictionary updates imported transactionally into Room.
  - Status: implemented; release flow verified against the public distribution repository.
  - Implementation: `DictionaryUpdateClient.kt` reads the latest releases from `shiokunn/prompter-dictionary`, finds exact or versioned envelope assets, applies download bounds/timeouts, and uses release-tag versioning. `DictionaryUpdateVerifier.kt` verifies exact payload bytes with the embedded Ed25519 public key before parsing. `DictionaryUpdates.kt` stores a complete replacement snapshot transactionally in Room; the bundled SQLite remains read-only and history storage is untouched. `M1Matcher` assigns signed aliases priority 10,000 over bundled aliases and `PromptEngine` reloads after install. Settings has an explicit "更新を確認" action, current version/count, progress and result feedback.
  - Verification: GitHub Actions `40879374322` passed the full Kotlin suite and APK build. Verifier tests cover genuine signatures, tampering, unsigned/oversized data and rollback; Room tests cover atomic replacement/clear and alias precedence. On Android 16 emulator, release v2 was fetched, verified and displayed as installed (`署名辞書 v2`, 4 aliases); a subsequent check reported `最新です（v2）`. A follow-up build fixed support for versioned asset names, and CI `40879374322` covers it. Main UI was observed showing the NIKKE base record and its separate variant; selecting the variant populated name, work and confirmed English name.
  - Important: no background update runs; only the user-triggered settings action fetches releases. Failed fetch/signature/validation leaves the prior Room snapshot intact. Release signing key stays outside the app and public repository.
- [x] Remove small explanatory copy and redesign the character-mode UI.
  - Implementation: removed the main-screen subtitle and character lookup helper copy; replaced the two-mode segmented toggle with a switch, compact search controls, grouped tappable candidates and a selected-character confirmation card. Alternate forms have a clear badge, and selected web results keep a source link.
  - Verification: Android 16 emulator showed the compact picker and the Cinderella/Cinderella: Crystal Wave candidates; the variant row showed 別形態 and selection populated the selected state. This UI is included in the CI-built APK.
- [x] Curate character entries for the requested ten works with source and coverage checks.
  - Status: complete for the recorded scope
  - Implementation: `character.md` is the reviewable source of truth. It contains 621 records across the ten works, with per-work source and coverage rows plus an explicit completion-criteria section. Alternate forms, costumes, personas and units are separate records (`category=variant`); Genshin NPCs are excluded per user instruction. `tools/build_character_db.py` generates `app/src/main/assets/characterdb.sqlite` (series / characters / aliases / meta, 1,550 alias rows) from that file.
  - Verification: Wuthering.gg's full 61-entry display was reconciled with no missing rows. NIKKE combines the JP name list, Nikke.gg playable roster and 2026 official announcements; Aigis was the only visible Nikke.gg entry missing. `CharacterDb` lookups (Japanese name, English name, nickname, reading, width/case normalization, variant separation) pass Robolectric tests in GitHub Actions run `40857989940`, which also built the APK. English official-localization status, unverified NIKKE Japanese names and public-redistribution terms remain documented as open items, not silently assumed.
- [x] Add bundled character database with alias search to the character UI.
  - Implementation: `CharacterDb.search` matches canonical names, short names, readings and same-form aliases, returning series + category + source note. MainActivity shows "登録済みの候補" while typing; choosing one still requires the explicit "この英語名を確認して使う" action.
  - Verification: covered by `CharacterDbTest` in CI, and observed on device (Android 16 emulator, installed with `adb install -r` so existing data was preserved). Typing `UMR` surfaces `土間埋 / Umaru Doma` with its work and source, and the confirm action fills name, work and English name. Typing `Cinderella` shows the base NIKKE record and `Cinderella: Crystal Wave` as a separate result labelled 別形態, matching the no-auto-merge policy.
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
