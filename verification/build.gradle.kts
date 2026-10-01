import java.io.File

plugins { java }
repositories { mavenCentral() }
java { toolchain { languageVersion.set(JavaLanguageVersion.of(21)) } }
dependencies {
    implementation(platform("org.springframework.boot:spring-boot-dependencies:3.5.15"))
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-security")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
sourceSets {
    main {
        java.setSrcDirs(listOf("../auth-service/src/main/java"))
        java.include("com/kerosene/common/admin/cell/**", "com/kerosene/common/admin/SystemReleaseController.java",
            "com/kerosene/common/security/AdminRoles.java", "com/kerosene/common/release/ReleaseManifestService.java",
            "com/kerosene/common/release/ReleaseCanonicalJson.java", "com/kerosene/common/release/bank/**",
            "com/kerosene/common/release/ReleaseEvidenceFiles.java",
            "com/kerosene/common/security/EndpointPolicyRegistry.java", "com/kerosene/config/ReleaseJsonConfig.java")
    }
    test {
        java.setSrcDirs(listOf("../auth-service/src/test/java"))
        java.include("com/kerosene/common/admin/cell/**", "com/kerosene/common/release/ReleaseManifestServiceTest.java",
            "com/kerosene/common/release/bank/**")
    }
}
tasks.test { useJUnitPlatform { excludeTags("node-core-wire") } }
tasks.register<Test>("nodeCoreWireTest") {
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    useJUnitPlatform { includeTags("node-core-wire") }
    doFirst {
        val probe = providers.environmentVariable("KEROSENE_NODE_BANK_PROBE").orNull
        check(!probe.isNullOrBlank() && File(probe).isAbsolute && File(probe).canExecute()) {
            "KEROSENE_NODE_BANK_PROBE must identify the explicitly built disposable Node probe"
        }
    }
}
