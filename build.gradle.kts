plugins {
    kotlin("jvm") version "2.3.0" apply false
}

subprojects {
    group = "site.seeun.blogsample"
    version = "0.1.0"

    tasks.withType<org.gradle.api.tasks.testing.Test>().configureEach {
        useJUnitPlatform()
    }
}

