# Third-party notices

MyWarwick+ application code is licensed under MIT. That license does not
relicense its dependencies, Warwick's services/content, or university branding.

The Android runtime dependencies include:

| Project | License | Upstream |
| --- | --- | --- |
| AndroidX (Compose, Activity, Lifecycle, Browser, Room and transitives) | Apache-2.0 | https://android.googlesource.com/platform/frameworks/support/ |
| Kotlin standard library | Apache-2.0 | https://github.com/JetBrains/kotlin |
| kotlinx.coroutines | Apache-2.0 | https://github.com/Kotlin/kotlinx.coroutines |
| OkHttp and Okio | Apache-2.0 | https://github.com/square/okhttp ; https://github.com/square/okio |

Upstream authors retain their copyrights. Existing dependency license/notice
resources are retained in the APK, and `assets/legal/` includes this notice,
MIT, Apache License 2.0 and the privacy information. The release packaging
script also distributes these files. This table names the principal runtime
projects, not a complete dependency inventory; the release's resolved runtime
dependency report is archived with its build records.

The official MyWarwick repository and deployed frontend were consulted for
protocol/behaviour evidence. This project does not bundle their frontend,
official Android source, university logo, or account screenshots. Retrieved
student content belongs to its respective owners and is not covered by MIT.
