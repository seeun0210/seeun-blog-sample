# Bounded Context Domain Modules

Blog: https://blog.seeun.site/posts/bounded-context-domain-modules

이 샘플은 bounded context 기준으로 domain module을 나누는 방식을 최소 코드로 보여줍니다.

## Structure

```text
bounded-context-domain-modules/
  apps/
    api/
    backoffice/
  domain/
    billing/
    catalog/
    learning/
    organization/
  support/
    persistence/
  architecture-tests/
```

## What To Check

- `apps:api`는 수강 신청 흐름에 필요한 `learning`, `catalog`, `billing`만 의존합니다.
- `apps:backoffice`는 운영 화면에 필요한 `organization`, `billing`만 의존합니다.
- `domain:*` 모듈은 `apps:*`나 `support:*`에 의존하지 않습니다.
- `support:persistence`는 adapter 역할로 domain port를 구현합니다.
- `architecture-tests`는 위 규칙이 깨졌을 때 빌드가 실패하는 예시입니다.

## Run

```sh
./gradlew :bounded-context-domain-modules:architecture-tests:test
```

