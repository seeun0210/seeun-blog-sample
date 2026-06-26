# Spring AI Anthropic: Prompt Caching Cost & CostAdvisor

Blog: https://blog.seeun.site/posts/spring-ai-anthropic-prompt-caching (2편)

이 샘플은 2편의 두 가지를 코드로 고정합니다.

1. Anthropic 프롬프트 캐싱의 손익을 가르는 단가 산술 (실행되는 테스트)
2. `.entity()`가 가려버린 토큰·비용을 거두는 `CostAdvisor`(Spring AI `CallAdvisor`)의 모양 (스니펫)

## Structure

```text
spring-ai-anthropic-cost-advisor/
  advisor-snippet/
    CostAdvisor.kt              # 컴파일되지 않는 스니펫 (실제 CallAdvisor 모양)
  src/
    main/kotlin/.../TokenUsage.kt
    main/kotlin/.../AnthropicPricing.kt     # input ×1, cacheWrite ×1.25, cacheRead ×0.1
    test/kotlin/.../AnthropicPricingTest.kt # write만 쌓이면 net-negative 증명
```

## What To Check

- `AnthropicPricingTest`는 "캐시 적중(read)이 없으면 cache write는 그냥 1.25배 비싼 입력"임을 단가로 증명합니다.
- 우리 워크로드는 역량 섹션마다 프롬프트가 달라 4번 호출이 모두 cache write + read=0이었고, 그래서 캐싱이 손해였습니다.
- `advisor-snippet/CostAdvisor.kt`는 실제 운영 코드의 `CallAdvisor` 모양입니다. `.entity()`가 `ChatResponse`를 돌려주지 않으므로, usage는 advisor 체인에서 거둡니다.

## Run

```sh
./gradlew :spring-ai-anthropic-cost-advisor:test
```
