package site.seeun.blogsample.warmup

import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.health.contributor.Health
import org.springframework.boot.health.contributor.HealthIndicator
import java.util.concurrent.atomic.AtomicBoolean

fun interface StartupWarmupRequestExecutor {
    fun execute(path: String)
}

class StartupWarmup(
    private val paths: List<String>,
    private val requestExecutor: StartupWarmupRequestExecutor,
) : ApplicationRunner, HealthIndicator {
    private val ready = AtomicBoolean(false)

    override fun run(args: ApplicationArguments) {
        paths.forEach(requestExecutor::execute)
        ready.set(true)
    }

    override fun health(): Health =
        if (ready.get()) {
            Health.up().build()
        } else {
            Health.outOfService().build()
        }
}
