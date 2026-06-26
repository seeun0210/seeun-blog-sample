package site.seeun.blogsample.springai

/**
 * Anthropic 토큰 단가(USD per 1M). 캐시 쓰기는 입력 단가의 1.25배, 캐시 읽기는 0.1배다.
 *
 * 이 배수가 프롬프트 캐싱의 손익을 가른다:
 * - 캐시를 쓰고(write) 나중에 충분히 읽으면(read) 전체 입력 비용이 내려간다.
 * - 캐시를 쓰기만 하고 읽지 못하면(read=0) 입력에 25%를 더 낸 셈이 된다.
 */
data class AnthropicPricing(
    val inputPerMillion: Double = 3.0,
    val outputPerMillion: Double = 15.0,
    val cacheWriteMultiplier: Double = 1.25,
    val cacheReadMultiplier: Double = 0.1,
) {
    fun estimateUsd(usage: TokenUsage): Double {
        val inputRate = inputPerMillion / 1_000_000.0
        val outputRate = outputPerMillion / 1_000_000.0
        return usage.inputTokens * inputRate +
            usage.outputTokens * outputRate +
            usage.cacheWriteTokens * inputRate * cacheWriteMultiplier +
            usage.cacheReadTokens * inputRate * cacheReadMultiplier
    }
}
