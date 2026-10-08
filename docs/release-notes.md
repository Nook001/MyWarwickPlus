# MyWarwick+ 0.26.0-beta.2

Local candidate for Android 9 and later, awaiting manual acceptance. Not yet
published on GitHub. This is an independent student project, not an official
University of Warwick application; a Warwick account is required.

## Changes

- Inbox: filter cached messages by source, such as Tabula or Comms, alongside
  text search. All removes the source restriction; Clear filters resets both.
  Filtering does not send requests or mark university messages as read.
- Home: four square website shortcuts, with a two-column layout on narrow
  screens or with larger system text.
- Me: smaller action tiles, up to four columns, reduced padding and icons.
  Long labels and larger system text can expand tile height.

Buses, Print Balance and Events APIs have been verified in an authenticated
browser. Their native pages and cache are still planned and are not included.

## Install

Use the signed `MyWarwickPlus-0.26.0-beta.2.apk`. It updates the public package
`io.github.nook001.mywarwickplus` with the same signing certificate, retaining
the existing session and cache. Earlier internal, Debug and Profile packages
are separate; a first installation of the public package requires sign-in.
Check `SHA256SUMS.txt` if needed.

## Limits and feedback

MyWarwick internal APIs may change or require renewed sign-in. Times use
Europe/London. Coursework is an aggregation feed, not a submission record.
There is no background sync, system notification, submission or automatic
updater. Non-empty library loan details and broader device coverage remain
unverified. Use official services for authoritative balances and submissions.

[Privacy](https://github.com/Nook001/MyWarwickPlus/blob/master/PRIVACY.md) ·
[Feedback](https://github.com/Nook001/MyWarwickPlus/issues)

Include app/Android versions and reproduction steps; remove personal data,
cookies and tokens from reports. This candidate does not indicate university
endorsement or authorization.
