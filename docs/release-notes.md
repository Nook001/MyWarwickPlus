# MyWarwick+ 0.27.0-beta.1

Public release for Android 9 and later, marked as Latest on GitHub. The
original version name and tag are retained. This is an independent student
project; a Warwick account is required.

## Changes

- Home: Buses and Print occupy two half-width cards after Deadlines. Buses
  shows up to two departure times with compact route/destination and stop
  previews; tap for complete original route descriptions. Print
  shows the returned balance and opens the official account website.
- Events: up to three upcoming or ongoing campus activities at the bottom of
  Home, with full details and links to the original event website.
- Core student data loads first. Home pull-to-refresh includes the new
  services; returning to Home refreshes stale summaries without background
  polling. Failed requests retain cached data.
- Existing UI refinements are retained. Schema 5 upgrades to 6 by adding tables;
  existing account and student caches are preserved.

## Install and verify

Install the signed `MyWarwickPlus-0.27.0-beta.1.apk` over the public package
`io.github.nook001.mywarwickplus`. The signing certificate is unchanged.
Internal, Debug and Profile packages remain separate. Use `SHA256SUMS.txt`
for checksum verification.

The signed build passed the developer's physical-device acceptance. When
upgrading, check the bus and balance summaries, Events links, Home refresh and
offline cache on your device.

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
