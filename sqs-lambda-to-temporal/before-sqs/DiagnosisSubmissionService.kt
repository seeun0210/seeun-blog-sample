package site.seeun.blogsample.beforesqs

// 스니펫: 컴파일되지 않습니다(이전 SQS+Lambda 구조를 보여주기 위한 참고용).
//
// 진단 제출 한 번이 여러 상태 + outbox 메시지를 만든다. 여기까지는 '요청'일 뿐,
// 실제 리포트 생성은 아직 시작도 안 했다. 발행조차 별도 relay가 2초마다 폴링해서 한다.

@Service
class DiagnosisSubmissionService(
    private val entitlement: EntitlementDomainService,
    private val diagnosisRepository: DiagnosisRepository,
    private val generationJobRepository: GenerationJobRepository,
    private val reportGenerationRequestPort: DiagnosticReportGenerationRequestPort,
    private val outboxMessageRepository: OutboxMessageRepository,
) {
    @Transactional
    fun execute(command: SubmitDiagnosisCommand): DiagnosisResult {
        entitlement.holdForConsumption(command.userId)
        val diagnosis = diagnosisRepository.save(Diagnosis.create(command)) // PENDING -> PROCESSING
        val job = generationJobRepository.save(GenerationJob.create(targetType = DIAGNOSTIC_REPORT))
        val outbox =
            reportGenerationRequestPort.createOutboxMessage(
                DiagnosticReportGenerationRequestCreateCommand(diagnosis, job, command.contextRef),
            )
        outboxMessageRepository.save(outbox.toOutboxMessage()) // 발행은 DiagnosticReportGenerationOutboxRelay가 담당
        return DiagnosisResult(diagnosis.id)
    }
}

// DiagnosticReportGenerationOutboxRelay
//   @Scheduled(fixedDelayString = "...delay-ms:2000")  // 2초마다 폴링
//   -> OutboxProcessor.process() -> Publisher.publish() -> SQS(diagnostic-report-generation-request-events)
//   SQS: visibilityTimeout=900s, maxReceiveCount=3 -> DLQ
