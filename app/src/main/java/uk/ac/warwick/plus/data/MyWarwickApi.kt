package uk.ac.warwick.plus.data

import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import uk.ac.warwick.plus.auth.AuthSession
import uk.ac.warwick.plus.auth.MY_WARWICK
import java.io.IOException
import java.util.concurrent.TimeUnit

class SignInRequiredException : Exception()
class ServiceException(val status: Int) : Exception()
data class SignedInUser(val code: String, val name: String, val csrfHeader: String, val csrfToken: String)

class MyWarwickApi(session: AuthSession) {
    private val client = OkHttpClient.Builder().cookieJar(session)
        .followRedirects(false).followSslRedirects(false)
        .connectTimeout(15, TimeUnit.SECONDS).readTimeout(25, TimeUnit.SECONDS)
        .callTimeout(35, TimeUnit.SECONDS).build()

    private data class JsonResponse(val code: Int, val body: String)
    private fun getResponse(path: String, user: SignedInUser? = null): JsonResponse {
        val request = Request.Builder().url(MY_WARWICK + path)
            .header("Accept", "application/json")
            .header("User-Agent", "MyWarwickPlus/0.1 (Android)")
            .apply {
                if (user != null && user.csrfHeader.equals("Csrf-Token", ignoreCase = true) && user.csrfToken.isNotBlank()) {
                    header("Csrf-Token", user.csrfToken)
                }
            }.build()
        client.newCall(request).execute().use { response ->
            if (response.code in 300..399 || response.code == 401 || response.code == 403) throw SignInRequiredException()
            if (!response.isSuccessful) throw ServiceException(response.code)
            if (!response.header("Content-Type").orEmpty().contains("application/json")) throw SignInRequiredException()
            val body = response.body ?: throw IOException("Empty response")
            // A bounded response prevents an unexpected HTML/error body exhausting memory.
            val source = body.source()
            source.request(4L * 1024 * 1024 + 1)
            if (source.buffer.size > 4L * 1024 * 1024) throw InvalidResponseException()
            return JsonResponse(response.code, source.readUtf8())
        }
    }

    private fun get(path: String, user: SignedInUser? = null) = getResponse(path, user).body

    fun probe(endpoint: ProbeEndpoint): ProbeResult {
        check(uk.ac.warwick.plus.BuildConfig.DEBUG)
        val start = System.nanoTime()
        val response = getResponse(endpoint.path, user())
        return ProbeSummary.parse(endpoint, response.code, (System.nanoTime() - start) / 1_000_000, response.body)
    }

    fun user(): SignedInUser {
        val root = JSONObject(get("/user/info"))
        val user = root.optJSONObject("user") ?: throw SignInRequiredException()
        if (root.opt("refresh") is String || !user.optBoolean("authenticated")) throw SignInRequiredException()
        return SignedInUser(user.getString("usercode"), user.optString("name"),
            user.optString("csrfHeader"), user.optString("csrfToken"))
    }

    fun timetable(user: SignedInUser) = TimetableParser.parse(get("/api/tiles/content/timetable", user))
}
