plugins {
    kotlin("jvm")
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":bounded-context-domain-modules:domain:billing"))
    implementation(project(":bounded-context-domain-modules:domain:organization"))
}

