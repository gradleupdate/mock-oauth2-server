import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
    id("groovy")
    id("com.jlessing.spring-integration-test") version "1.1.1"
}

integrationTest {
    bootJar = rootProject.tasks["bootJar"] as BootJar
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("io.jsonwebtoken:jjwt:0.9.1")
    testImplementation("org.spockframework:spock-core:1.3-groovy-2.4")
}
