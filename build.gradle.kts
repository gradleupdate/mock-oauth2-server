import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.net.HttpURLConnection
import java.net.URL

repositories {
    mavenCentral()
}

plugins {
    kotlin("jvm") version "1.3.41"
    id("org.asciidoctor.jvm.convert") version "2.3.0"
    kotlin("plugin.jpa") version "1.3.41"
    id("org.springframework.boot") version "2.1.6.RELEASE"
    kotlin("plugin.spring") version "1.3.41"
    id("net.researchgate.release") version "2.8.1"
    id("idea")
}

release {
    preTagCommitMessage = "[Release] - pre tag commit: "
    tagCommitMessage = "[Release] - Version "
    newVersionCommitMessage = "[Release] - new version commit: "
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.9.9")
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.3.41")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.3.41")
    implementation("org.springframework.boot:spring-boot-starter-web:2.1.6.RELEASE")
    implementation("org.springframework.boot:spring-boot-starter-hateoas:2.1.6.RELEASE")
    implementation("io.jsonwebtoken:jjwt:0.9.1")
    testImplementation("org.springframework.restdocs:spring-restdocs-mockmvc:2.0.3.RELEASE")
}

group = "com.jlessing.orbit.oauth2.server"
java.sourceCompatibility = JavaVersion.VERSION_1_8
java.targetCompatibility = JavaVersion.VERSION_1_8

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "1.8"
    }
}
