package site.seeun.blogsample.warmup

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

class LocalHttpWarmupRequestExecutor(
    serverPort: Int,
    private val requestTimeout: Duration,
) : StartupWarmupRequestExecutor {
    private val baseUri = URI.create("http://127.0.0.1:$serverPort")
    private val client = HttpClient.newBuilder().connectTimeout(requestTimeout).build()

    override fun execute(path: String) {
        require(path.startsWith("/") && !path.startsWith("//")) {
            "Warmup path must be an application-relative path: $path"
        }
        val request =
            HttpRequest.newBuilder(baseUri.resolve(path))
                .timeout(requestTimeout)
                .GET()
                .build()

        val response = client.send(request, HttpResponse.BodyHandlers.discarding())
        check(response.statusCode() in 200..299) {
            "Warmup request failed: $path returned ${response.statusCode()}"
        }
    }
}
