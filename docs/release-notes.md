# MyWarwick+ 0.26.0-beta.1

First public beta candidate for Android 9 and later. This is an independent
student project, not an official University of Warwick application. A Warwick
account is required; sign-in uses the university's official pages and MFA.

## Included

- Home: next class, remaining today's classes / tomorrow preview, deadlines,
  recent messages, and Moodle/Email/Tabula/Library shortcuts.
- Classes: continuous date-grouped list, date selection, locations and details.
- Tasks: upcoming/past coursework, search, exact due times and source links.
- Inbox: compact messages, search, full details and explicit older-page loading.
- Me: email copy, library/module summaries, service links and local sign-out.
- Five colour themes, offline cache and refresh progress based on actual tasks.

## Install and update

Download `MyWarwickPlus-0.26.0-beta.1.apk` from the release's attached assets
on https://github.com/Nook001/MyWarwickPlus/releases. Check `SHA256SUMS.txt`
if needed. Android may ask you to allow installation from your browser/file
manager; use the standard Android installation flow.

The public package is `io.github.nook001.mywarwickplus`. It installs separately
from the earlier internal `uk.ac.warwick.plus` prototype and from Debug/Profile
builds; the first installation requires a new sign-in. Do not uninstall your
old prototype just to try this beta. Future public updates are intended to
preserve data through the same package/signing certificate and supported
database migrations.

## Known limitations

- Uses MyWarwick's internal read-only aggregation APIs; backend changes or
  expired sessions may temporarily require re-sign-in or a client update.
- Timetables/deadlines use Europe/London time. Tasks are the items returned by
  the aggregation service, not a complete assignment history or submission status.
- No background synchronization, system notifications or coursework submission.
- Reading messages does not mark them read on the university website.
- Non-empty library loan details have not been validated. Use the official site
  for authoritative library balances, submissions and other consequential checks.
- Device coverage is currently limited; English UI, no automatic app updater.

Privacy: https://github.com/Nook001/MyWarwickPlus/blob/master/PRIVACY.md

Feedback: https://github.com/Nook001/MyWarwickPlus/issues

Please include the app/Android version and reproduction steps. Remove names,
emails, student IDs, private messages, cookies and tokens from public reports.

## Candidate status

This file prepares a release candidate, not evidence of public availability or
university authorization. Before publishing, finish the manual acceptance in
`docs/release.md`, back up signing material, and resolve the permitted scope
of third-party distribution. Publish as a GitHub **Pre-release** initially.
