package uk.ac.warwick.plus.data

import java.net.URI
import uk.ac.warwick.plus.auth.MY_WARWICK

private val browserBase = URI("$MY_WARWICK/")

/** The system browser receives a URL only, never the app's cookie jar or CSRF headers. */
fun safeExternalUrl(raw: String): String? = safeBrowserUrl(raw, warwickOnly = false)
fun safeCourseworkUrl(raw: String): String? = safeBrowserUrl(raw, warwickOnly = true)

private fun safeBrowserUrl(raw: String, warwickOnly: Boolean): String? = runCatching {
    val uri = browserBase.resolve(raw)
    val host = uri.host?.lowercase().orEmpty()
    if (raw.isNotBlank() && uri.scheme == "https" && host.isNotBlank() && uri.rawUserInfo == null &&
        uri.port in listOf(-1, 443) && (!warwickOnly || host == "warwick.ac.uk" || host.endsWith(".warwick.ac.uk")))
        uri.toASCIIString() else null
}.getOrNull()
