import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

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
