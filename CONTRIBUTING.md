# Contributing to MyWarwick+

Thanks for helping. MyWarwick+ is an independent student project, not an
official University of Warwick application, so contributions must keep it
read-only, private and respectful of Warwick's services.

## Ways to help

- **Bug reports and ideas:** open an [issue](https://github.com/Nook001/MyWarwickPlus/issues)
  with the app and Android versions and steps to reproduce. Never include
  passwords, cookies, tokens, student IDs, private messages or unredacted
  screenshots.
- **Translations and copy:** English and Simplified Chinese live in
  `app/src/main/res/values*/strings.xml`.
- **Code:** small fixes are welcome directly as PRs. For new features, new
  Warwick endpoints or larger refactors, open an issue first so we can agree
  on scope.

Out of scope: write operations on university systems, analytics or tracking,
project-operated servers, bulk probing of internal APIs, and anything that
needs credentials other than Warwick's own sign-in.

## Development setup

1. Install Android Studio with JDK 21 and Android SDK Platform 37.
2. Clone the repository and open its root; the SDK path goes in the untracked
   `local.properties`.
3. Run the checks (Windows: `.\gradlew.bat`):

   ```shell
   ./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
   ```

The debug build installs as a separate package, so it never touches the
public app's sign-in or data. Sign in with your own Warwick account; there is
no test account or mock server.

## Making changes

[AGENTS.md](AGENTS.md) holds the project rules for both people and coding
agents. In short:

- Keep requests read-only and cookies restricted to `my.warwick.ac.uk`.
- Put every visible string in English and Simplified Chinese resources.
- Use `StudentDates` for dates and times (UK time).
- Add JVM tests for non-trivial logic only; don't add UI tests unless asked.
- Don't change versions, signing or release notes.

## Pull requests

- One focused change per PR, with a short imperative commit subject.
- Fill in the PR template, including how you verified the change and
  screenshots for UI changes (personal data removed).
- Update the relevant document when behaviour, endpoints or data handling
  change.
- AI-assisted PRs are fine. Say so, and make sure you have read the whole
  diff.

## Security

Please don't post vulnerability details publicly. Open an issue titled
"Security contact request" without details, and the maintainer will arrange a
private channel.

## License

By contributing you agree that your contribution is licensed under the
project's [MIT License](LICENSE).
