package site.seeun.blogsample.warmup

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import org.springframework.boot.DefaultApplicationArguments
import org.springframework.boot.health.contributor.Status

class StartupWarmupTest {
    @Test
    fun `warmup 실행 전에는 트래픽을 받지 않는다`() {
        val warmup = StartupWarmup(listOf("/api/v1/catalog")) { }

        assertEquals(Status.OUT_OF_SERVICE, warmup.health().status)
    }

    @Test
    fun `등록한 경로를 모두 호출한 뒤에만 트래픽을 받는다`() {
        val executedPaths = mutableListOf<String>()
        val warmup =
            StartupWarmup(
                paths = listOf("/api/v1/catalog", "/api/v1/home"),
                requestExecutor = executedPaths::add,
            )

        warmup.run(DefaultApplicationArguments())

        assertEquals(listOf("/api/v1/catalog", "/api/v1/home"), executedPaths)
        assertEquals(Status.UP, warmup.health().status)
    }

    @Test
    fun `한 경로라도 실패하면 트래픽을 받지 않는다`() {
        val warmup =
            StartupWarmup(listOf("/api/v1/catalog")) {
                error("warmup request failed")
            }

        assertFailsWith<IllegalStateException> {
            warmup.run(DefaultApplicationArguments())
        }

        assertEquals(Status.OUT_OF_SERVICE, warmup.health().status)
    }
}
