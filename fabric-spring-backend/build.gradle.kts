plugins {
	kotlin("jvm") version "2.2.21"
	kotlin("plugin.spring") version "2.2.21"
	kotlin("plugin.jpa") version "2.2.21"
	id("org.springframework.boot") version "4.0.5"
	id("io.spring.dependency-management") version "1.1.7"
}

// Specify which @SpringBootApplication to use as entry point
// (FabricApplication handles Fabric Gateway + WebSocket + OpenAPI)
springBoot {
	mainClass = "org.fabric.api.FabricApplicationKt"
}

group = "com.mpcorp"
version = "0.0.1-SNAPSHOT"
description = "Self-Sovereign Identity Application"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(17)
	}
}

configurations {
	compileOnly {
		extendsFrom(configurations.annotationProcessor.get())
	}
}

repositories {
	mavenCentral()
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-security")
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("tools.jackson.module:jackson-module-kotlin")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.18.3")
	implementation("com.fasterxml.jackson.core:jackson-databind:2.18.3")
	implementation("io.jsonwebtoken:jjwt-api:0.12.6")
	runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
	runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")
	runtimeOnly("com.mysql:mysql-connector-j")
	compileOnly("org.projectlombok:lombok")
	annotationProcessor("org.projectlombok:lombok")
	// Hyperledger Fabric Gateway SDK
	implementation("org.hyperledger.fabric:fabric-gateway:1.7.0")
	implementation("io.grpc:grpc-netty-shaded:1.68.1")
	// Swagger / SpringDoc OpenAPI  (used by org.fabric.api.config.OpenApiConfig)
	implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.6")
	// WebSocket / STOMP  (used by WebSocketConfig & FabricEventPublisher)
	implementation("org.springframework.boot:spring-boot-starter-websocket")
	// Kotlin Logging  (used by mu.KotlinLogging)
	implementation("io.github.microutils:kotlin-logging-jvm:3.0.5")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
	testImplementation("org.springframework.boot:spring-boot-starter-security-test")
	testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
	compilerOptions {
		freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
	}
}

allOpen {
	annotation("jakarta.persistence.Entity")
	annotation("jakarta.persistence.MappedSuperclass")
	annotation("jakarta.persistence.Embeddable")
}

tasks.withType<Test> {
	useJUnitPlatform()
}
