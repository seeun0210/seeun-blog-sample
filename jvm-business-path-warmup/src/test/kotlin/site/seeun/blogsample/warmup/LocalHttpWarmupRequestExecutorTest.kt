package site.seeun.blogsample.warmup

import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.time.Duration
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class LocalHttpWarmupRequestExecutorTest {
    private val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)

    @AfterTest
    fun tearDown() {
        server.stop(0)
    }

    @Test
    fun `loopback 주소로 GET 요청을 보낸다`() {
        val requestCount = AtomicInteger()
        server.createContext("/api/v1/catalog") { exchange ->
            requestCount.incrementAndGet()
            exchange.sendResponseHeaders(200, -1)
            exchange.close()
        }
        server.start()
        val executor =
            LocalHttpWarmupRequestExecutor(
                serverPort = server.address.port,
                requestTimeout = Duration.ofSeconds(1),
            )

        executor.execute("/api/v1/catalog")

        assertEquals(1, requestCount.get())
    }

    @Test
    fun `2xx 응답이 아니면 warmup 실패로 처리한다`() {
        server.createContext("/api/v1/catalog") { exchange ->
            exchange.sendResponseHeaders(503, -1)
            exchange.close()
        }
        server.start()
        val executor =
            LocalHttpWarmupRequestExecutor(
                serverPort = server.address.port,
                requestTimeout = Duration.ofSeconds(1),
            )

        assertFailsWith<IllegalStateException> {
            executor.execute("/api/v1/catalog")
        }
    }

    @Test
    fun `외부 URL은 warmup 경로로 사용할 수 없다`() {
        val executor =
            LocalHttpWarmupRequestExecutor(
                serverPort = server.address.port,
                requestTimeout = Duration.ofSeconds(1),
            )

        listOf("https://example.com/api", "//example.com/api").forEach { externalUrl ->
            assertFailsWith<IllegalArgumentException> {
                executor.execute(externalUrl)
            }
        }
    }
}
