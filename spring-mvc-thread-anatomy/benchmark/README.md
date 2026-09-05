# benchmark

이 모듈의 모든 수치를 재현합니다. 같은 bootJar를 실행 JVM만 바꿔가며 돌리므로 바이트코드는 항상 Java 21 타깃입니다.

```bash
JDK21=$(/usr/libexec/java_home -v 21)/bin/java \
JDK25=$(/usr/libexec/java_home -v 25)/bin/java \
./run.sh all
```

| 서브커맨드 | 측정 | 출력 |
|---|---|---|
| `platform` | 플랫폼 스레드 풀 vs 가상 스레드 | `raw/platform-vs-virtual.csv` |
| `pinning` | 캐리어 1개로 고정한 `synchronized` vs `ReentrantLock`, JDK 21/25 | — |
| `sweep` | 캐리어 수(`jdk.virtualThreadScheduler.parallelism`)를 1~24로 | `raw/pinning-parallelism.csv` |
| `cpu` | `-XX:ActiveProcessorCount`로 CPU 수 자체를 바꿔가며 | `raw/pinning-cpu.csv` |
| `pool` | HikariCP 풀 크기 1~8 | `raw/connection-pool.csv` |
| `tx` | `@Transactional`의 커넥션 점유, 스레드 경계 | `raw/transaction-hold.csv` |

`sweep`과 `cpu`는 조합마다 애플리케이션을 재기동하므로 몇 분 걸립니다. 각 측정 전에 warmup 버스트를 한 번 돌립니다 — 넣지 않으면 첫 측정만 JIT 때문에 크게 튑니다.

## 측정 환경

`summary.json`의 `environment`에 기록해 두었습니다. 12코어 macOS(arm64), Spring Boot 4.1.1, Tomcat 11.0.24, HikariCP 7.0.2, temurin-21.0.12.1 / openjdk-25.0.2.

## 읽는 법

`raw/*.csv`의 `theoretical_s`는 이론값입니다.

- pinning: `ceil(요청 수 / 캐리어 수) × 블로킹 시간` — 캐리어 하나가 한 번에 요청 하나씩만 처리한다고 가정
- 커넥션 풀: `ceil(요청 수 / 풀 크기) × 쿼리 시간`

측정값이 이론값에 붙으면 그 자원이 상한이라는 뜻이고, 이론값보다 훨씬 빠르면 그 자원은 상한이 아니라는 뜻입니다. `/nopin` 행이 이론값을 20배 이기는 게 그 예입니다.

## 주의

- 락 객체는 `PinController.sink`에 흘려 둡니다. escape analysis가 락을 제거하면 `/pin`이 `/nopin`처럼 동작해 측정이 무의미해집니다.
- `run.sh`는 zsh 전용입니다. `path`는 zsh에서 `PATH`에 묶인 특수 변수라 지역변수로 쓰면 함수 안에서 명령을 찾지 못합니다 — `burst()`가 `ep`를 쓰는 이유입니다.
- 포트 8080을 고정으로 씁니다. 다른 프로세스가 잡고 있으면 기동 대기에서 멈춥니다.
