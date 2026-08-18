plugins {
    kotlin("jvm")
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation("org.springframework.boot:spring-boot:4.0.3")
    implementation("org.springframework.boot:spring-boot-health:4.0.3")

    testImplementation(kotlin("test"))
}
