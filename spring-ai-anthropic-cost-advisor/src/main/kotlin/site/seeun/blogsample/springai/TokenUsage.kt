package site.seeun.blogsample.springai

/**
 * Anthropic 호출의 토큰 사용량. 프롬프트 캐싱은 입력 토큰을 세 갈래로 나눈다.
 *
 * - [inputTokens] 캐시되지 않은 일반 입력(input_tokens)
 * - [outputTokens] 출력(output_tokens)
 * - [cacheWriteTokens] 캐시 생성 입력(cache_creation_input_tokens) — 단가 ×1.25
 * - [cacheReadTokens] 캐시 적중 입력(cache_read_input_tokens) — 단가 ×0.1
 *
 * Spring AI에서는 ChatResponse.metadata.usage 의 nativeUsage(com.anthropic ... Usage)에서 읽는다.
 */
data class TokenUsage(
    val inputTokens: Long = 0,
    val outputTokens: Long = 0,
    val cacheWriteTokens: Long = 0,
    val cacheReadTokens: Long = 0,
)
