package site.seeun.blogsample.springai.snippet

// 스니펫: 컴파일되지 않습니다(Spring AI 2.0.0 의존이 필요). 실제 운영 코드의 모양을 보여줍니다.
//
// .entity()로 구조화 출력을 받으면 파싱된 DTO만 돌아오고 ChatResponse(=usage)는 손에 안 들어온다.
// 그래서 토큰·비용은 호출부가 아니라 CallAdvisor에서 거둔다 — 모든 ChatClient 호출이 체인을 통과하므로
// 호출 코드는 한 줄도 바꾸지 않는다.

@Component
class CostAdvisor(
    private val meterRegistry: MeterRegistry,
    private val costEstimator: CostEstimator,
) : CallAdvisor {
    override fun getName() = "costAdvisor"

    override fun getOrder() = Ordered.LOWEST_PRECEDENCE

    override fun adviseCall(
        request: ChatClientRequest,
        chain: CallAdvisorChain,
    ): ChatClientResponse {
        val response = chain.nextCall(request) // 실제 모델 호출은 체인 끝에서 일어난다
        val usage = toTokenUsage(response.chatResponse())
        CostAccumulator.add(currentRunKey(), usage, callCount = 1)
        record(usage)
        return response
    }

    private fun toTokenUsage(chatResponse: ChatResponse?): TokenUsage {
        val usage = chatResponse?.metadata?.usage ?: return TokenUsage()
        return TokenUsage(
            inputTokens = (usage.promptTokens ?: 0).toLong(),
            outputTokens = (usage.completionTokens ?: 0).toLong(),
            cacheWriteTokens = usage.cacheWriteInputTokens ?: 0L, // Anthropic cache_creation_input_tokens
            cacheReadTokens = usage.cacheReadInputTokens ?: 0L, // Anthropic cache_read_input_tokens
        )
    }

    // 등록: ChatClient.Builder.defaultAdvisors(SimpleLoggerAdvisor, costAdvisor)
}
