plugins {
    kotlin("jvm")
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    testImplementation(kotlin("test"))
    testImplementation("com.tngtech.archunit:archunit:1.4.1")
    testImplementation(project(":bounded-context-domain-modules:apps:api"))
    testImplementation(project(":bounded-context-domain-modules:apps:backoffice"))
    testImplementation(project(":bounded-context-domain-modules:domain:billing"))
    testImplementation(project(":bounded-context-domain-modules:domain:catalog"))
    testImplementation(project(":bounded-context-domain-modules:domain:learning"))
    testImplementation(project(":bounded-context-domain-modules:domain:organization"))
    testImplementation(project(":bounded-context-domain-modules:support:persistence"))
}

