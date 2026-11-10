import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.testing.Test
import org.gradle.testing.jacoco.plugins.JacocoPluginExtension
import org.gradle.testing.jacoco.tasks.JacocoCoverageVerification
import org.gradle.testing.jacoco.tasks.JacocoReport

plugins {
    `java-library`
    jacoco
    `maven-publish`
    id("org.flowdigger.maven-publish")
    id("org.owasp.dependencycheck") version "13.0.0"
    id("com.github.spotbugs") version "6.5.13"
}

group = "org.flowdigger"
version = "1.0.2"

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
    withSourcesJar()
    withJavadocJar()
}

dependencies {
    testImplementation("junit:junit:4.13.2")
    add("spotbugsPlugins", "com.h3xstream.findsecbugs:findsecbugs-plugin:1.14.0")
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            artifactId = "xir-sax"

            pom {
                name.set("xir-sax")
                description.set("SAX parser/serializer and XMLReader for bracket-based XML/XDM S-expressions")
                url.set("https://github.com/jurgenei/xir-sax")

                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/license/mit")
                    }
                }

                developers {
                    developer {
                        id.set("jurgenei")
                        name.set("Jurgen Hildebrand")
                    }
                }

                scm {
                    connection.set("scm:git:https://github.com/jurgenei/xir-sax.git")
                    developerConnection.set("scm:git:ssh://git@github.com/jurgenei/xir-sax.git")
                    url.set("https://github.com/jurgenei/xir-sax")
                }
            }
        }
    }
}


tasks.withType<Test>().configureEach {
    useJUnit()
}

tasks.named<Test>("test") {
    finalizedBy(tasks.named("jacocoTestReport"))
}

extensions.configure<JacocoPluginExtension> {
    toolVersion = "0.8.12"
}

tasks.named<JacocoReport>("jacocoTestReport") {
    dependsOn(tasks.named("test"))
    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(false)
    }
}

tasks.named<JacocoCoverageVerification>("jacocoTestCoverageVerification") {
    dependsOn(tasks.named("jacocoTestReport"))
    violationRules {
        rule {
            element = "BUNDLE"
            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = "0.00".toBigDecimal()
            }
        }
    }
}

tasks.register("coverage") {
    group = "verification"
    description = "Runs tests, generates JaCoCo report, and verifies minimum coverage threshold."
    dependsOn(tasks.named("jacocoTestCoverageVerification"))
}

tasks.named("check") {
    dependsOn(tasks.named("jacocoTestCoverageVerification"))
}

extensions.getByName("dependencyCheck").withGroovyBuilder {
    setProperty("formats", listOf("HTML", "JSON", "XML"))
    setProperty("failBuildOnCVSS", 7.0f)
    setProperty("suppressionFile", "dependency-check-suppressions.xml")
    setProperty("skipConfigurations", listOf("spotbugs", "spotbugsPlugins"))

    getProperty("nvd").withGroovyBuilder {
        setProperty(
            "apiKey",
            providers.gradleProperty("org.owasp.dependencycheck.nvd.api.key").orNull
                ?: System.getenv("NVD_API_KEY")
        )
    }
}

tasks.withType<com.github.spotbugs.snom.SpotBugsTask>().configureEach {
    ignoreFailures = providers.gradleProperty("spotbugsIgnoreFailures")
        .map { it.toBoolean() }
        .orElse(true)
        .get()
    effort = com.github.spotbugs.snom.Effort.DEFAULT
    reportLevel = com.github.spotbugs.snom.Confidence.MEDIUM
    excludeFilter.set(file("spotbugs-exclude.xml"))
    reports.create("html").required.set(true)
    reports.create("xml").required.set(false)
}

tasks.register("allSecurityChecks") {
    group = "verification"
    description = "Run all security checks (Dependency-Check and SpotBugs)."
    dependsOn("check", "spotbugsMain")
    val dependencyCheckTask = tasks.findByName("dependencyCheckAnalyze") ?: tasks.findByName("dependencyCheck")
    if (dependencyCheckTask != null) {
        dependsOn(dependencyCheckTask)
    }
}
