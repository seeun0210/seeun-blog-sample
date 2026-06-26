package site.seeun.blogsample.springai

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 프롬프트 캐싱이 이 워크로드(섹션마다 다른 프롬프트, 리포트당 한 번)에서 왜 손해였는지를
 * 단가 산술로 보여준다. 핵심은 "캐시 적중(read)이 없으면 write는 그냥 비싼 입력"이라는 점이다.
 */
class AnthropicPricingTest {
    private val pricing = AnthropicPricing()
    private val eps = 1e-9

    @Test
    fun `캐시 쓰기만 쌓이고 읽기가 0이면 캐시 없을 때보다 비싸다`() {
        // 같은 24K 프리픽스를 한 번 호출.
        val noCache = pricing.estimateUsd(TokenUsage(inputTokens = 24_000, outputTokens = 1_000))
        val cacheWriteOnly = pricing.estimateUsd(TokenUsage(cacheWriteTokens = 24_000, outputTokens = 1_000))

        // write는 입력 ×1.25 → read가 없으면 순손해.
        assertTrue(cacheWriteOnly > noCache)
        assertEquals(noCache + 24_000 * (3.0 / 1_000_000.0) * 0.25, cacheWriteOnly, eps)
    }

    @Test
    fun `프리픽스가 매번 달라 4번 모두 write면 캐시 없을 때보다 손해다`() {
        // 우리 워크로드: 4개 역량 호출이 각자 다른 프롬프트 → 매번 cache write, read=0.
        val withoutCaching = (1..4).sumOf { pricing.estimateUsd(TokenUsage(inputTokens = 24_000, outputTokens = 1_000)) }
        val withCaching = (1..4).sumOf { pricing.estimateUsd(TokenUsage(cacheWriteTokens = 24_000, outputTokens = 1_000)) }

        assertTrue(withCaching > withoutCaching)
    }

    @Test
    fun `캐시가 적중하면(read) 입력 비용은 1할로 떨어진다`() {
        val firstCallWrite = pricing.estimateUsd(TokenUsage(cacheWriteTokens = 24_000, outputTokens = 1_000))
        val laterCallRead = pricing.estimateUsd(TokenUsage(cacheReadTokens = 24_000, outputTokens = 1_000))

        // read는 입력 ×0.1 → 재사용이 충분할 때만 캐싱이 이득.
        assertTrue(laterCallRead < firstCallWrite)
        assertEquals(24_000 * (3.0 / 1_000_000.0) * 0.1 + 1_000 * (15.0 / 1_000_000.0), laterCallRead, eps)
    }
}
