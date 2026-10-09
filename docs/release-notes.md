# MyWarwick+ 0.28.0-beta.1

Public update for Android 9 and later. This is an independent student
project; a Warwick account is required.

## Changes

- Sign-in: when the daily Warwick session lapses, the app renews it silently
  through Warwick's own sign-in refresh, as the official app does. You only
  need to sign in again if Warwick asks for your password or MFA.
- Updates: release builds check GitHub for a newer release at most once a
  day while open, and Me → App has a manual check. New versions open in your
  browser; nothing is downloaded automatically. Automatic checks can be
  turned off in Settings.
- Reminders: optional class reminders (10 minutes before) and deadline
  reminders (24 hours before), off by default and switched on in Settings.
  They are produced on the device from your saved timetable and coursework.
- Widget: a home-screen Next class widget with today's timeline, following
  your colour theme.
- Language: Simplified Chinese interface, following the system language;
  on Android 13 and later it can be chosen per app in Android settings.
  Times stay in UK time.
- No database changes; existing account and student caches are preserved.

## Install and verify

Install the signed `MyWarwickPlus-0.28.0-beta.1.apk` over the public package
`io.github.nook001.mywarwickplus`. The signing certificate is unchanged.
Internal, Debug and Profile packages remain separate. Use `SHA256SUMS.txt`
for checksum verification. Android asks for notification permission only
when you turn a reminder on.

## Limits

Reminders and the widget use the data saved at the last sync; the app still
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
