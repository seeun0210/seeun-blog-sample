package site.seeun.blogsample.beforesqs

// 스니펫: 컴파일되지 않습니다.
//
// 실제 생성기(외부 Lambda/서비스, 이 레포에는 코드 없음)가 SQS 요청을 소비해 리포트를 만들고,
// 완료/실패를 SNS -> SQS 로 돌려준다. 아래는 그 콜백을 받는 소비자다.
// 동시성은 maxConcurrentMessages=1 — 완료 이벤트가 몰리면 한 번에 하나씩만 처리된다.

@Component
class DiagnosticReportSqsConsumer(
    private val objectMapper: ObjectMapper,
    private val handler: DiagnosticReportEventHandler,
) {
    @SqsListener(value = ["\${diagnostic-report.event-queue-url}"], factory = "defaultSqsListenerContainerFactory")
    fun consume(
        payload: String,
        acknowledgement: Acknowledgement,
    ) {
        val raw = unwrapSnsEnvelope(payload) // SNS "Message" 필드 언래핑
        val event =
            runCatching { objectMapper.readValue(raw, DiagnosticReportEventMessage::class.java) }
                .getOrElse {
                    acknowledgement.acknowledge() // 파싱 실패는 조용히 ack(멱등성)
                    return
                }
        handler.execute(event, raw)
        acknowledgement.acknowledge()
    }
}

// SqsListenerProperties(글로벌, prefix=sclass.sqs.listener):
//   maxConcurrentMessages = 1   // <- 직렬 처리 병목
//   maxMessagesPerPoll    = 1
//   pollTimeoutSeconds    = 10
