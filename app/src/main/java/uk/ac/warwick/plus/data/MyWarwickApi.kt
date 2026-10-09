package uk.ac.warwick.plus.data

import uk.ac.warwick.plus.BuildConfig

import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import uk.ac.warwick.plus.auth.AuthSession
import uk.ac.warwick.plus.auth.MY_WARWICK
import java.util.concurrent.TimeUnit

/** [refreshUrl] is MyWarwick's own SSO round trip, offered when only its session has lapsed. */
class SignInRequiredException(val refreshUrl: String? = null) : Exception()
class ServiceException(val status: Int) : Exception()
data class SignedInUser(val code: String, val name: String, val csrfHeader: String, val csrfToken: String)

class MyWarwickApi(session: AuthSession) : StudentApi {
    private val requests = CancellableRequests()
    override suspend fun <T> request(operation: () -> T): T = requests.run(operation)
    private val client = OkHttpClient.Builder().cookieJar(session)
        .followRedirects(false).followSslRedirects(false)
        .connectTimeout(15, TimeUnit.SECONDS).readTimeout(25, TimeUnit.SECONDS)
        .callTimeout(35, TimeUnit.SECONDS).build()

    private data class JsonResponse(val code: Int, val body: String)
    fun cancelRequests() = client.dispatcher.cancelAll()
    private fun getResponse(path: String, user: SignedInUser? = null): JsonResponse {
        val request = Request.Builder().url(MY_WARWICK + path)
            .header("Accept", "application/json")
            .header("User-Agent", "MyWarwickPlus/${BuildConfig.VERSION_NAME} (Android)")
            .apply {
                if (user != null && user.csrfHeader.equals("Csrf-Token", ignoreCase = true) && user.csrfToken.isNotBlank()) {
                    header("Csrf-Token", user.csrfToken)
                }
            }.build()
        val call = client.newCall(request)
        requests.attach(call)
        call.execute().use { response ->
            if (response.code in 300..399 || response.code == 401 || response.code == 403) throw SignInRequiredException()
            if (!response.isSuccessful) throw ServiceException(response.code)
            if (!response.header("Content-Type").orEmpty().contains("application/json")) throw SignInRequiredException()
            val body = response.body
            // A bounded response prevents an unexpected HTML/error body exhausting memory.
            val source = body.source()
            source.request(4L * 1024 * 1024 + 1)
            if (source.buffer.size > 4L * 1024 * 1024) throw InvalidResponseException()
            return JsonResponse(response.code, source.readUtf8())
        }
    }

    private fun get(path: String, user: SignedInUser? = null) = getResponse(path, user).body

    fun probe(endpoint: ProbeEndpoint): ProbeResult {
        check(BuildConfig.DEBUG)
        val start = System.nanoTime()
        val response = getResponse(endpoint.path, user())
        return ProbeSummary.parse(endpoint, response.code, (System.nanoTime() - start) / 1_000_000, response.body)
    }

    override fun user(): SignedInUser {
        val root = JSONObject(get("/user/info"))
        val user = root.optJSONObject("user") ?: throw SignInRequiredException()
        val refresh = root.opt("refresh") as? String
        if (refresh != null || !user.optBoolean("authenticated")) throw SignInRequiredException(refresh?.takeIf { it.isNotBlank() })
        return SignedInUser(user.requiredString("usercode"), user.stringOrEmpty("name"),
            user.stringOrEmpty("csrfHeader"), user.stringOrEmpty("csrfToken"))
    }

    override fun timetable(user: SignedInUser) = TimetableParser.parse(get("/api/tiles/content/timetable", user))
    override fun account(user: SignedInUser): String {
        val content = tileContent(get("/api/tiles/content/account", user), "account")
        val email = content.opt("email")
        if (email != null && email != JSONObject.NULL && email !is String) throw InvalidResponseException()
        return (email as? String)?.trim().orEmpty()
    }
    override fun coursework(user: SignedInUser) = CourseworkParser.parse(get("/api/tiles/content/coursework", user))
    override fun feed(kind: FeedKind, user: SignedInUser, before: String?) = FeedParser.parse(kind, get(kind.path(before), user))
    override fun service(kind: ServiceKind, user: SignedInUser) = ServiceParser.parse(kind, get("/api/tiles/content/${kind.tile}", user))
}
