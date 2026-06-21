# Querydsl to Exposed kapt Removal

Blog: https://blog.seeun.site/posts/querydsl-jpa-to-exposed-kapt-removal

이 샘플은 Querydsl/JPA 기반 persistence를 Exposed DSL로 옮길 때 코드 경계가 어떻게 바뀌는지 보여줍니다.

## Structure

```text
querydsl-to-exposed-kapt-removal/
  before-querydsl/
    build.gradle.kts
    LessonJpaQueryRepository.kt
  src/
    main/kotlin/.../Lesson.kt
    main/kotlin/.../LessonsTable.kt
    main/kotlin/.../LessonMapper.kt
    main/kotlin/.../LessonRepository.kt
    test/kotlin/.../LessonRepositoryTest.kt
```

## What To Check

- `before-querydsl`는 `kotlin("kapt")`, Querydsl APT, Q 타입 의존 흐름만 보여주는 스니펫입니다.
- 실행되는 코드는 Exposed DSL 버전입니다.
- repository는 generated Q type 없이 `LessonsTable`과 mapper만으로 동작합니다.
- domain model은 Exposed 타입을 import하지 않습니다.

## Run

```sh
./gradlew :querydsl-to-exposed-kapt-removal:test
```
