import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
    id("groovy")
    id("com.jlessing.spring-integration-test") version "1.0.0"
}

integrationTest {
    bootJar = rootProject.tasks["bootJar"] as BootJar
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("org.spockframework:spock-core:1.3-groovy-2.4")
}