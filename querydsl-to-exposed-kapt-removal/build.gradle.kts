plugins {
    kotlin("jvm")
}

kotlin {
    jvmToolchain(21)
}

val exposedVersion = "1.3.0"

dependencies {
    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-java-time:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")

    runtimeOnly("com.h2database:h2:2.3.232")

    testImplementation(kotlin("test"))
}
