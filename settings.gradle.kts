pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
    }
}

rootProject.name = "seeun-blog-sample"

include("bounded-context-domain-modules:apps:api")
include("bounded-context-domain-modules:apps:backoffice")
include("bounded-context-domain-modules:domain:billing")
include("bounded-context-domain-modules:domain:catalog")
include("bounded-context-domain-modules:domain:learning")
include("bounded-context-domain-modules:domain:organization")
include("bounded-context-domain-modules:support:persistence")
include("bounded-context-domain-modules:architecture-tests")

include("querydsl-to-exposed-kapt-removal")
include("sqs-lambda-to-temporal")
include("spring-ai-anthropic-cost-advisor")
include("jvm-business-path-warmup")
include("spring-mvc-thread-anatomy")
