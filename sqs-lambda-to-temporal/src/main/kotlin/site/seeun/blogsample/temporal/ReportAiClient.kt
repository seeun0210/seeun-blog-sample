package site.seeun.blogsample.temporal

/**
 * AI 호출 경계(seam).
 *
 * 운영에서는 Spring AI `ChatClient` 구현이 들어간다(블로그 본문 참고):
 * ```
 * chatClient.prompt().system(instruction).user(context)
 *     .call().entity(SomeDto::class.java) { it.useProviderStructuredOutput() }
 * ```
 * 테스트에서는 외부 호출 없이 결정적 가짜 구현으로 대체한다.
 */
fun interface ReportAiClient {
    fun write(
        instruction: String,
        context: String,
    ): String
}
