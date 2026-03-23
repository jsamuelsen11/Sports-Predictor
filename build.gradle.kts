import net.ltgt.gradle.errorprone.errorprone

plugins {
    java
    jacoco
    checkstyle
    alias(libs.plugins.spotless)
    alias(libs.plugins.spotbugs)
    alias(libs.plugins.errorprone)
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
}

group = "com.sportspredictor"
version = "0.1.0-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

dependencyManagement {
    imports {
        mavenBom(libs.spring.ai.bom.get().toString())
    }
}

dependencies {
    // Spring Boot starters
    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.spring.boot.starter.cache)
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.spring.boot.starter.actuator)

    // Spring AI MCP server
    implementation(libs.spring.ai.starter.mcp.server.webmvc)

    // Database
    runtimeOnly(libs.sqlite.jdbc)
    runtimeOnly(libs.hibernate.community.dialects)
    implementation(libs.flyway.core)

    // Caching
    implementation(libs.caffeine)

    // Resilience
    implementation(libs.resilience4j.spring.boot3)
    implementation(libs.resilience4j.ratelimiter)
    implementation(libs.resilience4j.circuitbreaker)
    implementation(libs.resilience4j.retry)

    // Lombok
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
    testCompileOnly(libs.lombok)
    testAnnotationProcessor(libs.lombok)

    // Code quality
    errorprone(libs.error.prone.core)

    // Test
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.wiremock.standalone)
    testImplementation(libs.archunit.junit5)
}

tasks.withType<Test> {
    useJUnitPlatform()
}

// ---------------------------------------------------------------------------
// Code Quality
// ---------------------------------------------------------------------------

// Spotless — Palantir Java Format
spotless {
    java {
        target("src/*/java/**/*.java")
        palantirJavaFormat()
        removeUnusedImports()
        trimTrailingWhitespace()
        endWithNewline()
    }
}

// Checkstyle — Google style
checkstyle {
    toolVersion = libs.versions.checkstyle.get()
    configFile = file("config/checkstyle/checkstyle.xml")
    isIgnoreFailures = false
    maxWarnings = 0
}

tasks.withType<Checkstyle> {
    reports {
        xml.required.set(false)
        html.required.set(true)
    }
}

// SpotBugs — static analysis
spotbugs {
    effort.set(com.github.spotbugs.snom.Effort.MAX)
    reportLevel.set(com.github.spotbugs.snom.Confidence.MEDIUM)
    excludeFilter.set(file("config/spotbugs/exclude-filter.xml"))
}

tasks.withType<com.github.spotbugs.snom.SpotBugsTask> {
    reports.create("html") {
        required.set(true)
        setStylesheet("fancy-hist.xsl")
    }
    reports.create("xml") {
        required.set(false)
    }
}

// Error Prone — compile-time bug detection
tasks.withType<JavaCompile>().configureEach {
    options.errorprone.disableWarningsInGeneratedCode = true
    options.errorprone.disable("MissingSummary")
}

// JaCoCo — coverage enforcement
jacoco {
    toolVersion = libs.versions.jacoco.get()
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

tasks.jacocoTestCoverageVerification {
    dependsOn(tasks.jacocoTestReport)
    violationRules {
        rule {
            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = "0.70".toBigDecimal()
            }
        }
    }
}

tasks.check {
    dependsOn(tasks.named("spotlessCheck"))
    dependsOn(tasks.jacocoTestCoverageVerification)
}
