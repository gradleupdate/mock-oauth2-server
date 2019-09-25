import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.net.HttpURLConnection
import java.net.URL
import java.nio.file.Files
import java.nio.file.StandardCopyOption

repositories {
    mavenCentral()
}

plugins {
    kotlin("jvm") version "1.3.41"
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

object Versions {
    val springBoot = System.getProperty("versions.spring-boot")!!
    val jjwt = System.getProperty("versions.jjwt")!!
    val kotlin = System.getProperty("versions.kotlin")!!
    val jacksonKotlin = System.getProperty("versions.jackson-kotlin")!!
    val swagger = System.getProperty("versions.swagger")!!
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("io.springfox:springfox-swagger2:${Versions.swagger}")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:${Versions.jacksonKotlin}")
    implementation("org.jetbrains.kotlin:kotlin-reflect:${Versions.kotlin}")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:${Versions.kotlin}")
    implementation("org.springframework.boot:spring-boot-starter-web:${Versions.springBoot}")
    implementation("org.springframework.boot:spring-boot-starter-hateoas:${Versions.springBoot}")
    implementation("io.jsonwebtoken:jjwt:${Versions.jjwt}")
    testImplementation("org.springframework.boot:spring-boot-starter-test:${Versions.springBoot}")
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

val downloadOpenAPI by tasks.registering() {
    dependsOn(project(":mock-oauth2-server-intTest").tasks["startSpringApplication"])
    finalizedBy(project(":mock-oauth2-server-intTest").tasks["stopSpringApplication"])
    doLast {
        val openAPIConnection = URL("http://localhost:8023/v2/api-docs").openConnection() as HttpURLConnection
        var openApiRes = ""
        openAPIConnection.inputStream.bufferedReader().use { openApiRes = it.readText() }
        val generatedPagesDir = project.projectDir.toPath().resolve("build/generated-pages").toFile()
        generatedPagesDir.mkdirs()
        generatedPagesDir.toPath().resolve("api.json").toFile().writeText(openApiRes)
        Files.copy(project.projectDir.toPath().resolve("gradle/pages/index.html"), generatedPagesDir.toPath().resolve("index.html"), StandardCopyOption.REPLACE_EXISTING)
    }
}
