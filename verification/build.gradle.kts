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
            "com/kerosene/common/security/AdminRoles.java", "com/kerosene/common/release/ReleaseManifestService.java")
    }
    test {
        java.setSrcDirs(listOf("../auth-service/src/test/java"))
        java.include("com/kerosene/common/admin/cell/**", "com/kerosene/common/release/ReleaseManifestServiceTest.java")
    }
}
tasks.test { useJUnitPlatform() }
