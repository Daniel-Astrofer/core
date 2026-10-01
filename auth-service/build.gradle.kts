plugins {

    java
    id("org.springframework.boot") version "3.5.15"
    id("io.spring.dependency-management") version "1.1.6"
    // Supply Chain Defense: varre CVEs conhecidos em todas as dependências (NVD)
    id("org.owasp.dependencycheck") version "10.0.4"
    jacoco
}


group = "kerosene"
version = "PRE-ALPHA"
description = "backend"

java {
    sourceCompatibility = JavaVersion.VERSION_21
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
	mavenCentral()
}

configurations.configureEach {
    exclude(group = "org.bouncycastle", module = "bcprov-jdk15to18")
}

dependencies {
    implementation("io.kerosene.contracts:kerosene-contracts:0.2.0-SNAPSHOT")
    implementation("kerosene:kerosene-shared:PRE-ALPHA")
    implementation("io.jsonwebtoken:jjwt-api:0.13.0")

    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.13.0")

    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.13.0")
    implementation("org.bitcoinj:bitcoinj-core:0.15.10") {
        exclude(group = "org.bouncycastle", module = "bcprov-jdk15to18")
    }
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-web")
	runtimeOnly("com.h2database:h2")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    implementation("org.jboss.aerogear:aerogear-otp-java:1.0.0.M1")
    implementation("commons-codec:commons-codec:1.16.0")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-websocket")
    implementation("org.postgresql:postgresql:42.7.7")
    implementation("org.zeromq:jeromq:0.6.0")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-cbor")
    implementation("net.logstash.logback:logstash-logback-encoder:7.4")
    implementation("com.bucket4j:bucket4j-core:8.7.0")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("io.micrometer:micrometer-registry-prometheus")
    implementation("org.flywaydb:flyway-core")
    runtimeOnly("org.flywaydb:flyway-database-postgresql")

    implementation("io.micrometer:micrometer-tracing-bridge-brave")
    implementation("io.micrometer:micrometer-observation")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("com.tngtech.archunit:archunit-junit5:1.3.0")

    implementation("org.bouncycastle:bcpkix-jdk18on:1.84")
    implementation("org.bouncycastle:bcprov-jdk18on:1.84")
    constraints {
        implementation("com.google.protobuf:protobuf-java:3.25.9") {
            because("CVE-2024-7254 affects the older version pulled transitively by bitcoinj")
        }
        implementation("io.netty:netty-codec-http2:4.2.6.Final") {
            because("CVE-2025-55163 affects older Netty HTTP/2 implementations")
        }
    }
    compileOnly("jakarta.annotation:jakarta.annotation-api:2.1.1")
    compileOnly("javax.annotation:javax.annotation-api:1.3.2")
}



tasks.withType<Test> {
	useJUnitPlatform { excludeTags("node-core-wire") }
    finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(false)
    }
}

tasks.named<org.gradle.language.jvm.tasks.ProcessResources>("processResources") {
    val webAdminBuild = file("web-admin-build").takeIf {
        it.resolve("index.html").exists()
    }

    if (webAdminBuild != null) {
        from(webAdminBuild) {
            into("static")
        }
    }
}

// ─── OWASP Supply Chain Defense ─────────────────────────────────────────────
// Executa com: ./gradlew dependencyCheckAnalyze
// No CI/CD: adicionar ao pipeline antes do build de produção.
// Documenta falhas em build/reports/dependency-check-report.html
dependencyCheck {
    // Falha a build se qualquer dependência tiver CVSS >= 7.0 (HIGH/CRITICAL)
    failBuildOnCVSS = 7.0f

    nvd {
        apiKey = System.getenv("NVD_API_KEY") ?: ""
    }

    // Formatos do relatório: HTML legível + JSON para CI
    formats = listOf("HTML", "JSON")

    analyzers {
        // Ativa análise de JARs (principal para projetos Java)
        jarEnabled = true
        nodeAuditEnabled = false   // Sem Node.js neste projeto
    }
}
