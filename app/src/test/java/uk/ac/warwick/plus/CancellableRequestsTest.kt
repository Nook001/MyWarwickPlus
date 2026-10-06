package uk.ac.warwick.plus

import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.Assert.*
import org.junit.Test
import uk.ac.warwick.plus.data.CancellableRequests
import java.net.InetAddress
import java.net.ServerSocket
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

class CancellableRequestsTest {
    @Test fun cancellationInterruptsAnActualStalledResponseBody() = runBlocking {
        val started = CompletableDeferred<Unit>()
        val finished = CompletableDeferred<Unit>()
        val server = ServerSocket(0, 1, InetAddress.getLoopbackAddress())
        val serverThread = thread(isDaemon = true) {
            try {
                server.accept().use { socket ->
                    val input = socket.getInputStream().bufferedReader()
                    while (!input.readLine().isNullOrEmpty()) { }
                    socket.getOutputStream().apply {
                        write("HTTP/1.1 200 OK\r\nContent-Length: 1000\r\n\r\n{".toByteArray())
                        flush()
                    }
                    started.complete(Unit)
                    socket.getInputStream().read() // Client cancellation closes this connection.
                }
            } catch (_: java.io.IOException) { }
        }
        val requests = CancellableRequests()
        val client = OkHttpClient.Builder().callTimeout(30, TimeUnit.SECONDS).build()
        val call = client.newCall(Request.Builder().url("http://localhost:${server.localPort}/").build())
        val job = launch {
            requests.run {
                requests.attach(call)
                try { call.execute().use { it.body!!.string() } }
                finally { finished.complete(Unit) }
            }
        }
        try {
            withTimeout(5_000) { started.await() }
            withTimeout(2_000) { job.cancelAndJoin(); finished.await() }
            assertTrue(call.isCanceled())
            assertTrue(job.isCancelled)
        } finally {
            call.cancel(); server.close(); job.cancelAndJoin()
            serverThread.join(1_000)
            client.connectionPool.evictAll()
            client.dispatcher.executorService.shutdown()
        }
    }

    @Test fun cancellationBeforeCallRegistrationCannotMissTheCallOrPoisonTheNextRequest() = runBlocking {
        val started = CompletableDeferred<Unit>()
        val finished = CompletableDeferred<Unit>()
        val release = CountDownLatch(1)
        val requests = CancellableRequests()
        val client = OkHttpClient()
        val call = client.newCall(Request.Builder().url("https://example.invalid/").build())
        val job = launch {
            requests.run {
                started.complete(Unit)
                try {
                    check(release.await(5, TimeUnit.SECONDS))
                    requests.attach(call)
                } finally { finished.complete(Unit) }
            }
        }
        try {
            withTimeout(5_000) { started.await() }
            job.cancel()
            release.countDown()
            withTimeout(2_000) { job.join(); finished.await() }
            assertTrue(call.isCanceled())
            val next = client.newCall(Request.Builder().url("https://example.invalid/").build())
            requests.run { requests.attach(next) }
            assertFalse(next.isCanceled())
        } finally { release.countDown(); job.cancelAndJoin() }
    }
}
