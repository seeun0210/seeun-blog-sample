package site.seeun.blogsample.beforesqs

// 스니펫: 컴파일되지 않습니다.
//
// "리포트 생성"이라는 하나의 일이 이벤트 타입별 processor로 흩어진다.
// 상태 전이·멱등성·재시도 정책이 handler / 4개 processor / 여러 service에 나뉘어 있어,
// 전체 흐름을 따라가려면 파일을 여러 개 열어야 한다.

@Component
class DiagnosticReportEventHandler(
    processors: List<DiagnosticReportEventProcessor>,
) {
    private val byType = processors.associateBy { it.type }

    fun execute(
        event: DiagnosticReportEventMessage,
        raw: String,
    ) {
        val processor = byType[event.resolvedType] ?: return // 미지원 타입은 조용히 skip
        processor.process(event)?.let { dispatch(it) } // 후처리 알림(post-commit)
    }
}

// started -> 상태 기록 / completed -> 제출 완료 + 알림
// report.completed -> S3 위치 기록 + resultUrl + 알림 / report.failed -> 재시도 or 실패 분기

class DiagnosticReportCompletedEventProcessor(
    private val completionService: DiagnosticReportCompletionService,
) : DiagnosticReportEventProcessor {
    override val type = REPORT_COMPLETED

    override fun process(event: DiagnosticReportEventMessage): PostCommitNotification? {
        val loc = event.report ?: error("missing report location")
        completionService.execute(CompleteDiagnosticReportCommand(loc.bucket, loc.key, loc.jobId))
        return DiagnosisReportCompletedNotification(event.diagnosisId /* , phones, resultUrl ... */)
    }
}

class DiagnosticReportFailedEventProcessor(
    private val failureService: DiagnosticReportFailureService,
) : DiagnosticReportEventProcessor {
    override val type = REPORT_FAILED

    override fun process(event: DiagnosticReportEventMessage): PostCommitNotification? {
        failureService.execute(FailDiagnosticReportCommand(event.error)) // retryable이면 backoff 재예약
        return null
    }
}

// GenerationJobRetryBackoff: 1 -> 30s, 2 -> 2m, 3 -> 10m, 4 -> 30m, 5+ -> 1h
// 멱등성: DiagnosisStatus 종료 상태 검사 + 중복 이벤트 skip + processor의 null 반환에 분산
