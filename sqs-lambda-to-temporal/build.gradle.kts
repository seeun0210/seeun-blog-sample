plugins {
    kotlin("jvm")
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation("io.temporal:temporal-sdk:1.35.0")

    testImplementation("io.temporal:temporal-testing:1.35.0")
    testImplementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.18.2")
    testImplementation(kotlin("test"))
}
