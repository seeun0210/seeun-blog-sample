# Spring MVC thread anatomy

Spring MVC가 요청을 어떤 스레드로 처리하는지, 가상 스레드를 켜면 무엇이 바뀌고 무엇이 그대로인지를 직접 재현하는 최소 예제입니다. 톰캣 워커 풀, JDK 21의 pinning, 커넥션 풀 크기, `@Transactional`의 커넥션 점유를 각각 측정 가능한 엔드포인트로 분리했습니다.

- 블로그 1편: [요청 하나에 스레드 하나, 정말 그런가](https://blog.seeun.site/posts/spring-mvc-thread-model-virtual-thread-pinning)
- 블로그 2편: [@Transactional의 정체는 ThreadLocal이다](https://blog.seeun.site/posts/transactional-threadlocal-connection-hold)
- 측정 원본과 재현 스크립트: [`benchmark`](./benchmark)

## 엔드포인트

| 경로 | 무엇을 보여주는가 |
|---|---|
| `GET /thread` | 요청을 처리한 스레드 이름을 로그로 남긴다. 플랫폼 스레드 풀에서는 `http-nio-8080-exec-N`이 재사용되고, 가상 스레드에서는 `tomcat-handler-N`이 매번 새로 만들어진다. |
| `GET /pin?ms=` | 요청마다 **새 락 객체**를 만들어 `synchronized` 안에서 블로킹한다. 락 경합이 0이므로 느려지면 원인은 pinning뿐이다. |
| `GET /nopin?ms=` | 같은 구조를 `ReentrantLock`으로. 가상 스레드가 캐리어에서 언마운트된다. |
| `GET /db?ms=` | H2에 느린 쿼리를 한 번 날린다. 커넥션을 `ms` 동안 점유한다. |
| `GET /tx/in`, `/tx/none` | 한 트랜잭션 안에서 커넥션이 재사용되는지, 트랜잭션이 없으면 매번 새로 대여되는지. |
| `GET /tx/async` | 트랜잭션 안에서 새 스레드로 나갔을 때 트랜잭션이 끊기는 것. 부모는 롤백, 자식은 커밋된다. |
| `GET /tx/hold?ms=`, `/tx/free?ms=` | `@Transactional`이 **쿼리 없이도** 커넥션을 점유한다는 것. |
| `GET /tx/pool` | HikariCP의 `active` / `idle` / 대기 수. |

`/pin`과 `/nopin`의 락 객체는 `private static volatile Object sink`에 흘려 둡니다. 스택에 갇힌 객체는 JIT의 escape analysis가 락 자체를 제거해 버려서 측정이 무의미해지기 때문입니다.

## 실행

```bash
./gradlew :spring-mvc-thread-anatomy:bootRun
```

기본 설정은 관찰이 쉽도록 일부러 작게 잡혀 있습니다 (`application.properties`).

```properties
server.tomcat.threads.max=5
server.tomcat.threads.min-spare=2
spring.threads.virtual.enabled=true
spring.datasource.hikari.maximum-pool-size=2
```

플랫폼 스레드 풀과 비교하려면 `-Dspring.threads.virtual.enabled=false`로 다시 띄우면 됩니다.

## 측정

```bash
JDK21=$(/usr/libexec/java_home -v 21)/bin/java \
JDK25=$(/usr/libexec/java_home -v 25)/bin/java \
./benchmark/run.sh all
```

서브커맨드는 `platform`, `pinning`, `sweep`, `cpu`, `pool`, `tx`입니다. 같은 bootJar를 실행 JVM만 바꿔가며 돌립니다 — 바이트코드는 21로 고정입니다.

측정 결과 원본은 [`benchmark/raw`](./benchmark/raw)의 CSV, 요약은 [`benchmark/summary.json`](./benchmark/summary.json)에 있습니다.

## 무엇이 나오는가

12코어 macOS, Spring Boot 4.1.1 / Tomcat 11.0.24 / HikariCP 7.0.2 기준입니다.

**가상 스레드를 켜면 `threads.max` 상한이 사라진다** — `threads.max=5`, 동시 8요청, 각 1초 블로킹: 플랫폼 2.07s → 가상 1.07s.

**JDK 21에서 `synchronized` 안의 블로킹은 캐리어를 잠근다** — 캐리어 1개, 동시 4요청:

| | JDK 21 | JDK 25 |
|---|---|---|
| `/nopin` (ReentrantLock) | 1.03s | 1.03s |
| `/pin` (synchronized) | 4.05s | 1.03s |

**pinning이 걸리면 동시 처리량이 캐리어 수(기본값 = `availableProcessors`)로 잘린다** — 동시 24요청 각 200ms에서, `/pin`의 소요 시간이 `ceil(24 / 캐리어) × 200ms`와 8개 지점 전부 오차 0.04초 이내로 일치합니다. `-XX:ActiveProcessorCount`로 CPU 수를 속여도 같은 숫자가 나옵니다.

**가상 스레드를 켜도 동시 쿼리 수는 커넥션 풀 크기가 상한이다** — 동시 8요청 각 200ms 쿼리에서 풀 크기 1/2/4/8에 따라 1.66 / 0.84 / 0.44 / 0.24초. JDK 21과 25가 같습니다(이 경로에서는 pinning이 없습니다).

**`@Transactional`은 쿼리가 없어도 커넥션을 잡는다** — 풀 2, 동시 8요청, 각 200ms 대기, SQL 0회:

| | 소요 | 대기 중 풀 |
|---|---|---|
| `@Transactional` + sleep | 0.85s | `active=2 idle=0 대기=6` |
| 트랜잭션 없이 sleep | 0.23s | `active=0 idle=2 대기=0` |

**트랜잭션은 스레드를 넘지 않는다** — `/tx/async`의 로그:

```
부모            | thread=tomcat-handler-4 | txActive=true  | resource(ThreadLocal)=true  | conn0
자식(새 스레드) | thread=child-thread     | txActive=false | resource(ThreadLocal)=false | conn1
→ 부모 롤백 후 남은 행: [{ID=2, NAME=child}]
```

## 읽어볼 소스

측정한 동작이 어디서 나오는지 확인하려면 해당 버전의 sources jar를 받아 아래 위치를 보면 됩니다.

| 위치 | 내용 |
|---|---|
| `NioEndpoint.java:587, 912, 986` | Acceptor → Poller → 워커 풀로 넘어가는 두 번의 스레드 교체 |
| `TaskQueue.java:91-110` | 큐에 넣기 전에 스레드부터 max까지 늘리려고 `offer()`가 일부러 `false`를 반환 |
| `ThreadPoolExecutor.java:1089` | 톰캣은 `prestartAllCoreThreads()`로 `min-spare`를 미리 띄운다 |
| `AbstractEndpoint.java:1936` | `new ThreadPoolExecutor(minSpare, max, …)` |
| `FrameworkServlet.java:997, 1012` | 요청 ThreadLocal을 심고 `finally`에서 지우는 자리 |
| `NioSocketImpl.java:172-179` | 소켓 대기가 `Thread.currentThread().isVirtual()`로 갈라지는 지점 |
| `ConcurrentBag.java:163` | HikariCP의 커넥션 대여 대기 (`SynchronousQueue.poll` → 언마운트됨) |
| `TransactionSynchronizationManager.java:77-93` | 트랜잭션 상태를 담은 `ThreadLocal` 6개 |
| `DataSourceUtils.java:103-117` | 모든 커넥션 획득이 지나는 ThreadLocal 조회 |
