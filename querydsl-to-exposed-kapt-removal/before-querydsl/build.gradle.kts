plugins {
    kotlin("jvm")
    kotlin("plugin.jpa")
    kotlin("kapt")
}

dependencies {
    api("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("com.querydsl:querydsl-jpa:5.1.0:jakarta")
    kapt("com.querydsl:querydsl-apt:5.1.0:jakarta")
}
