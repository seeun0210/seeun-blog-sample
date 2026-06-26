# SQS + Lambda Event Pipeline to Temporal Workflow

Blog: https://blog.seeun.site/posts/sqs-lambda-to-temporal (1편)

이 샘플은 진단 리포트 생성을 SQS+Lambda 이벤트 기반에서 Temporal 워크플로로 옮겼을 때
오케스트레이션이 어떻게 한곳으로 모이는지 보여줍니다.

## Structure

```text
sqs-lambda-to-temporal/
  before-sqs/                         # 컴파일되지 않는 스니펫 (이전 구조)
    DiagnosisSubmissionService.kt       # 요청 -> outbox (relay가 2초마다 SQS 발행)
    DiagnosticReportSqsConsumer.kt      # SNS->SQS 콜백 소비, maxConcurrentMessages=1
    DiagnosticReportEventProcessors.kt  # handler + 4 processor로 흩어진 완료/실패/재시도
  src/
    main/kotlin/.../model.kt
    main/kotlin/.../ReportAiClient.kt              # AI 호출 경계(Spring AI seam)
    main/kotlin/.../ReportGenerationActivities.kt
    main/kotlin/.../ReportGenerationActivitiesImpl.kt  # 결정적 분석 + AI 서술
    main/kotlin/.../ReportGenerationWorkflow.kt
    main/kotlin/.../ReportGenerationWorkflowImpl.kt    # 흐름 전체가 한 메서드
    test/kotlin/.../ReportGenerationWorkflowTest.kt    # TestWorkflowEnvironment
```

## What To Check

- `before-sqs/`는 실제 동작 코드가 아니라, 흩어진 이전 구조를 보여주는 스니펫입니다.
- 실행되는 코드는 Temporal 워크플로 버전입니다.
- `generate()` 한 메서드가 결정적 분석 -> 병렬 서술(fan-out) -> 배리어 -> 조립 전체를 보여줍니다.
- 숫자는 `analyzeScores`(결정적 분석)가 소유하고, AI는 `ReportAiClient`로 문장만 채웁니다.
- 유료 AI 호출은 `RetryOptions`(maxAttempts=3 + 지수 백오프)로 비용을 묶습니다(기본은 무제한 재시도).
- Kotlin data class는 Temporal `DataConverter`에 Jackson Kotlin module을 등록해야 직렬화됩니다.

## Run

```sh
./gradlew :sqs-lambda-to-temporal:test
```
