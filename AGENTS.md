# AGENTS.md

Guidance for coding agents, and the people driving them, working on MyWarwick+.
Read this before changing code. Human contributors start with
[CONTRIBUTING.md](CONTRIBUTING.md); detailed docs in `docs/` are written in
Simplified Chinese.

## The project

An independent native Android client for the University of Warwick's
MyWarwick. Kotlin, Jetpack Compose, one `app` module, Room cache, OkHttp.
Sign-in happens on Warwick's official SSO pages in a WebView; the app then
makes read-only GET requests to `my.warwick.ac.uk` with that session. There is
no project server, analytics or background sync.

| Path (`app/src/main/java/uk/ac/warwick/plus/`) | Responsibility |
| --- | --- |
| `auth/` | WebView SSO, CookieManager, origin-restricted CookieJar, silent SSO refresh |
| `data/` | API client, strict parsers, Room entities/DAO, `TimetableRepository` |
| `ui/` | ViewModel, sync/retry, state snapshots, screens and pure presentation helpers |
| `ui/components/` | Shared cards, rows, headers, search and detail-sheet scaffolds |
| `config/` | `AppLabels` and other display-name sources |
| `reminders/`, `widget/`, `update/` | Local notifications, home-screen widget, GitHub release check |

Resources: `res/values/strings.xml` (English) and `res/values-zh-rCN/strings.xml`
(Simplified Chinese). JVM tests: `app/src/test`. Instrumented tests:
`app/src/androidTest`.

## Build and check

JDK 21, Android SDK Platform 37, min SDK 28. On Windows use `.\gradlew.bat`.

```shell
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
./gradlew :app:compileDebugAndroidTestKotlin   # when changing APIs the instrumented tests use
```

All of these must pass with no new lint findings or compiler warnings.

## Hard rules

Privacy and security:

- Never log, print, commit or paste cookies, tokens, CSRF values, passwords or
  real student data (names, IDs, emails, messages, timetables). Use invented
  fixtures. Remove personal data from screenshots.
- Session cookies go only to `https://my.warwick.ac.uk:443` through
  `AuthSession`; never attach them to other hosts or browser links. The only
  other request is the cookie-free GitHub `releases/latest` check. A new host
  needs maintainer agreement and a `PRIVACY.md` update in the same change.
- Read-only access: no write requests, no read-state writeback, no coursework
  submission.
- Don't probe or scrape endpoints in bulk. Document a new endpoint's observed
  structure in `docs/api-progress.md` before depending on it.
- Parsers reject a malformed batch as a whole instead of skipping bad items;
  failures keep the existing cache.
- Never create, replace or commit signing keys, and don't change
  `versionCode`/`versionName`; releases are the maintainer's job.

Code:

- Match the surrounding style: concise Kotlin, comments only for constraints
  the code cannot show.
- Every user-visible string goes in `strings.xml` **and** its Simplified
  Chinese translation in the same change (lint fails on missing translations).
  Display text is never used as a key, route or stored value.
- Dates and times go through `ui/StudentDates.kt` (Europe/London; wording
  follows the app language), never the device time zone.
- Components receive display values and callbacks; they don't hold a
  ViewModel, make requests or change the account. No DI framework, extra
  modules or per-screen ViewModels without a concrete need.
- Schema changes need a Room migration that preserves existing caches; never
  use destructive migration.

Tests:

- Don't add or extend UI/instrumented tests unless the maintainer asks; keep
  `androidTest` compiling when you change APIs it uses.
- Add focused JVM tests only for non-trivial logic (parsing, time rules,
  sync/retry, scheduling). No tests for styling, spacing or colour tweaks, and
  no assertions that merely restate the implementation.
- Never run `connectedAndroidTest` on a device with a signed-in app: it
  uninstalls the app and wipes the session.

## Pull requests

- One topic per PR, minimal diff, no unrelated reformatting.
- Commit subjects are short imperative English sentences without prefixes,
  e.g. `Show next class day on Home after today ends`.
- Use the PR template: what and why, how it was verified (checks run, device
  and Android version if tested), screenshots for UI changes, and any API,
  privacy or schema impact.
- Update docs when behaviour changes: `docs/architecture.md` for UI and
  behaviour rules, `docs/api-progress.md` for endpoints, `PRIVACY.md` for data
  or network changes. Release notes and versions are left to the maintainer.
- Say when a PR is substantially agent-generated; a human must have read the
  whole diff.

## Where to look

| Need | Document |
| --- | --- |
| Code responsibilities, UI and component rules | [docs/architecture.md](docs/architecture.md) |
| Endpoints, response shapes, auth rules | [docs/api-progress.md](docs/api-progress.md) |
| Setup, manual checks, performance tooling | [docs/development.md](docs/development.md) |
| Signing and publishing (maintainer) | [docs/release.md](docs/release.md) |
| Data handling | [PRIVACY.md](PRIVACY.md) |

## Maintainer sessions

These apply when working for the repository owner:

- Deploy to the physical phone by explicit serial with an upgrade install
  (`adb -s <serial> install -r`) to keep sign-in and cache; installing on an
  emulator does not count as phone deployment. The debug package is
  `io.github.nook001.mywarwickplus.debug`.
- Simple features and UI tweaks are verified by building, the relevant
  existing checks and the owner's manual feedback on the phone.
- For each delivered version, briefly report the Git commit state and keep
  `docs/api-progress.md` current.
