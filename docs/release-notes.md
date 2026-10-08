# MyWarwick+ 0.27.1-beta.1

Public update for Android 9 and later. This is an independent student
project; a Warwick account is required.

## Changes

- Home: once today's classes have finished, the agenda lists every class on
  the next day with classes instead of disappearing. On Friday evening it
  previews Monday; weekends keep the existing greeting.
- Home: when the next class is not today, the card is labelled Tomorrow or
  with its date, shows a countdown within 24 hours, and no longer shows a
  day timeline that could be mistaken for today.
- Home: the Library shortcut opens the in-app Library summary; Back returns
  to Home. The official Library website remains one tap away on that page.
- No database or API changes; existing account and student caches are
  preserved.

## Install and verify

Install the signed `MyWarwickPlus-0.27.1-beta.1.apk` over the public package
`io.github.nook001.mywarwickplus`. The signing certificate is unchanged.
Internal, Debug and Profile packages remain separate. Use `SHA256SUMS.txt`
for checksum verification.

## Limits

Bus times contain no confirmed service date or real-time flag; the app preserves
source order and labels cached results rather than predicting departures.
Print top-ups and event registration happen on their official websites.
The event list is a server-provided snapshot, not a complete campus calendar.
Times use Europe/London. Internal APIs may change or require renewed sign-in.
There is no background sync, system notification, coursework submission or
university-message read-state writeback.

[Privacy](https://github.com/Nook001/MyWarwickPlus/blob/master/PRIVACY.md) ·
[Feedback](https://github.com/Nook001/MyWarwickPlus/issues)

Report app/Android versions and reproduction steps; remove personal data,
cookies and tokens. This project is not an official Warwick application.
