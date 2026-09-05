# seeun-blog-sample

블로그 글에서 다룬 설계 아이디어를 실행 가능한 작은 코드로 분리해 둔 저장소입니다.

## Samples

각 모듈은 대응하는 블로그 글이 있습니다.

| 모듈 | 블로그 |
|---|---|
| [Bounded Context Domain Modules](./bounded-context-domain-modules) | [모듈로 바운디드 컨텍스트 경계 만들기](https://blog.seeun.site/posts/bounded-context-domain-modules) |
| [Querydsl to Exposed kapt Removal](./querydsl-to-exposed-kapt-removal) | [Querydsl에서 Exposed로, kapt 제거하기](https://blog.seeun.site/posts/querydsl-jpa-to-exposed-kapt-removal) |
| [SQS Lambda to Temporal](./sqs-lambda-to-temporal) | [SQS + Lambda에서 Temporal로](https://blog.seeun.site/posts/sqs-lambda-to-temporal) |
| [Spring AI Anthropic Cost Advisor](./spring-ai-anthropic-cost-advisor) | [Spring AI로 Anthropic 프롬프트 캐싱 비용 줄이기](https://blog.seeun.site/posts/spring-ai-anthropic-prompt-caching) |
| [JVM Business-path Warmup](./jvm-business-path-warmup) | [배포만 하면 느려지는 API? JVM 첫 요청 Warmup 개선기](https://blog.seeun.site/posts/jvm-aotcache-business-path-warmup) |
| [Spring MVC Thread Anatomy](./spring-mvc-thread-anatomy) | [요청 하나에 스레드 하나, 정말 그런가](https://blog.seeun.site/posts/spring-mvc-thread-model-virtual-thread-pinning) · [@Transactional의 정체는 ThreadLocal이다](https://blog.seeun.site/posts/transactional-threadlocal-connection-hold) |

## Run

```sh
./gradlew test
```
