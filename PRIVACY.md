# MyWarwick+ privacy information

Updated: 8 October 2026. Applies to 0.26.0-beta.1.

MyWarwick+ is an independent student project, not an official University of
Warwick application. It connects directly to Warwick services; the project
does not operate a server that receives your account or student data.

## Sign-in and connections

Sign-in uses Warwick's official pages inside an Android WebView, including
any Microsoft sign-in and MFA pages required by the university. The app does
not provide its own password form, intercept passwords, or store your password.
Those services process sign-in under their own policies.

The WebView retains session cookies on your device. The app reads the relevant
cookies to authenticate HTTPS requests to `my.warwick.ac.uk`; it does not send
them to the developer or attach them to links opened in your browser. Warwick
receives normal request information, such as your IP address and the app's
versioned User-Agent.

## Data on your device

The app caches the account identity/name/email, timetable, coursework,
messages, module summaries and library summaries returned for your account,
along with refresh times. Preferences include your selected colour theme.
Data is stored in the app's private storage. There is no additional encrypted
database layer; protection also depends on Android's device security.

Cloud backup and Android app-data transfer are disabled by the app's backup
rules. The app has no advertising SDK, analytics SDK or automatic crash-report
upload. The sign-in WebView opts out of Android WebView metrics; services used
inside the WebView may operate their own cookies and telemetry.

Viewing messages here does not mark them read on MyWarwick. The app does not
submit coursework or change your university account settings.

## Your controls

- **Sign out** clears this app's session cookies, WebView storage/cache and
  cached student data. It does not sign you out of your system browser.
- **Clear app storage** or uninstall the app removes its local app data.
- **Copy email** writes your email to Android's clipboard only when you tap it;
  clipboard handling is then controlled by Android.
- External links open in your browser, which uses its own session and policies.

## Feedback

Public feedback can be submitted through
[GitHub Issues](https://github.com/Nook001/MyWarwickPlus/issues).
Do not post passwords, session cookies, tokens, student IDs, private messages
or screenshots containing personal data. There is no private support inbox
configured yet; report reproducible behaviour with personal details removed.
