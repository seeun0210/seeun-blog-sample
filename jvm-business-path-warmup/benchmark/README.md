# Benchmark and Grafana dashboard

블로그에 사용한 local Docker benchmark의 익명화된 원본과 Grafana dashboard입니다. production traffic이나 production record는 포함하지 않습니다.

## Data

- `raw/results.csv`: 조건별 5회 독립 재기동 결과
- `raw/load-traffic.csv`: 실제 로컬 요청 2,864건의 timestamp, phase, status, latency
- `summary.json`: 중앙값과 cold penalty 요약
- `load-summary.json`: 60초 부하의 RPS와 latency 분위수
- `charts/*.png`: Grafana 13.1.3에서 렌더링한 캡처

측정 환경은 JDK 25.0.3 AOTCache, Docker Desktop ARM64, 1 CPU, 768MiB, heap 128–352MiB였습니다. 같은 AOT cache를 유지하고 runtime warmup만 켜고 껐습니다. 각 조건은 다섯 번씩 컨테이너를 재기동했고 실행 순서를 번갈아 배치했습니다.

## Grafana dashboard 다시 열기

```bash
node build-grafana-dashboard.mjs
docker compose up -d
```

브라우저에서 다음 주소를 엽니다.

- 측정 dashboard: <http://127.0.0.1:33000/d/jvm-warmup-local>
- JVM 원리 dashboard: <http://127.0.0.1:33000/d/jvm-warmup-mechanism>

종료할 때는 다음 명령을 실행합니다.

```bash
docker compose down
```

`generate-load.mjs`는 URL과 출력 CSV 경로를 받아 60초 동안 12→45→110→25→90→35 RPS의 read-only 부하를 보냅니다.

```bash
node generate-load.mjs http://127.0.0.1:8080/api/read-only raw/load-traffic.csv
```

대상 API와 데이터 준비는 각 애플리케이션 환경에 맞춰야 합니다. side effect가 있는 API에는 실행하지 마세요.
