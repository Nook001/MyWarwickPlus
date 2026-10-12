# MyWarwick+ 0.29.0-beta.1

Public update for Android 9 and later. This is an independent student
project; a Warwick account is required.

## Changes

- Widgets: two new home-screen widgets show the time, place and name of your
  next class: a 2×2 small one and a 2×1 mini one. Both switch layout when
  resized. The existing 4×2 Next class widget is unchanged.
- Adding widgets: Settings → Home screen widgets has an Add button for each
  widget. It asks your launcher to show its own add dialog; some launchers
  don't support this, so you can still add the widgets from the launcher's
  widget list as usual.
- Language: choose System, English or Simplified Chinese in Settings. On
  Android 13 and later this is the same setting as the per-app language in
  Android settings. Times stay in UK time.
- Settings: each group is now one card, and colour themes are a single row
  of swatches, so all settings fit in much less space.
- Home: section names now match the tabs they open, so Deadlines is
  Tasks and Messages is Inbox; the Inbox page title is Inbox too.
- No database changes; existing account and student caches are preserved.

## Install and verify

Install the signed `MyWarwickPlus-0.29.0-beta.1.apk` over the public package
`io.github.nook001.mywarwickplus`. The signing certificate is unchanged.
Internal, Debug and Profile packages remain separate. Use `SHA256SUMS.txt`
for checksum verification.

## Limits

Reminders and widgets use the data saved at the last sync; the app still
does not sync in the background, so open it occasionally to keep them current.
Bus times contain no confirmed service date or real-time flag; the app preserves
source order and labels cached results rather than predicting departures.
Print top-ups and event registration happen on their official websites.
The event list is a server-provided snapshot, not a complete campus calendar.
Times use Europe/London. Internal APIs may change or require renewed sign-in.
There is no coursework submission or university-message read-state writeback.

[Privacy](https://github.com/Nook001/MyWarwickPlus/blob/master/PRIVACY.md) ·
[Feedback](https://github.com/Nook001/MyWarwickPlus/issues)

Report app/Android versions and reproduction steps; remove personal data,
cookies and tokens. This project is not an official Warwick application.
