# JVM business-path warmup

JDK 25 AOTCache를 유지하면서 실제 비즈니스 경로에 남은 첫 요청 비용을 readiness 이전으로 옮기는 최소 예제입니다.

- 블로그: [“방금 배포하셨나요?”에서 시작한 JVM 첫 요청 warmup](https://blog.seeun.site/posts/jvm-aotcache-business-path-warmup)
- 측정 원본과 Grafana 재현: [`benchmark`](./benchmark)

## 구성

`StartupWarmup`은 `ApplicationRunner`와 `HealthIndicator`를 함께 구현합니다. 등록한 모든 경로가 성공하기 전에는 `OUT_OF_SERVICE`, 모두 성공한 뒤에는 `UP`을 반환합니다.

`LocalHttpWarmupRequestExecutor`는 `127.0.0.1`로 GET 요청을 보내 실제 HTTP routing, filter, controller, service, DB, JSON 직렬화 경로를 통과시킵니다. 외부 URL은 받지 않으며 2xx가 아닌 응답은 실패로 처리합니다.

```kotlin
@Bean
fun startupWarmup(
    @Value("\${server.port:8080}") serverPort: Int,
): StartupWarmup =
    StartupWarmup(
        paths = listOf("/api/v1/catalog"),
        requestExecutor =
            LocalHttpWarmupRequestExecutor(
                serverPort = serverPort,
                requestTimeout = Duration.ofSeconds(3),
            ),
    )
```

운영 경로는 반드시 read-only이고 부작용이 없어야 합니다. 외부 결제, 알림, 메시지 발행처럼 재실행할 수 없는 작업을 warmup 대상으로 삼으면 안 됩니다.

## Test

```bash
./gradlew :jvm-business-path-warmup:test
```

테스트는 다음 동작을 검증합니다.

- warmup 전에는 `OUT_OF_SERVICE`
- 등록한 경로를 모두 호출한 뒤 `UP`
- 한 경로라도 실패하면 계속 `OUT_OF_SERVICE`
- 2xx가 아니면 실패
- 외부 URL은 거부
