import java.net.URI
import kotlinx.kover.gradle.plugin.dsl.AggregationType.COVERED_PERCENTAGE
import kotlinx.kover.gradle.plugin.dsl.CoverageUnit.BRANCH
import kotlinx.kover.gradle.plugin.dsl.CoverageUnit.INSTRUCTION
import kotlinx.kover.gradle.plugin.dsl.CoverageUnit.LINE
import kotlinx.kover.gradle.plugin.dsl.GroupingEntityType.APPLICATION
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.kotlin.lang)
    alias(libs.plugins.kotlinx.kover)
    alias(libs.plugins.sonarqube.scanner)
    `maven-publish`
    java
}

group = "com.thomas"

repositories {
    mavenCentral()
    mavenLocal()
    google()
    maven {
        url = uri("https://repo.repsy.io/mvn/${System.getenv("REPSY_USERNAME")}/thomas-release")
        credentials {
            username = System.getenv("REPSY_USERNAME")
            password = System.getenv("REPSY_PASSWORD")
        }
    }
}

dependencies {
    kover(project(":cache-handler"))
    kover(project(":cache-handler-caffeine"))
}

subprojects {

    val libs = rootProject.libs

    apply(plugin = libs.plugins.kotlin.lang.get().pluginId)
    apply(plugin = libs.plugins.kotlinx.kover.get().pluginId)
    apply(plugin = libs.plugins.sonarqube.scanner.get().pluginId)
    apply(plugin = "maven-publish")
    apply(plugin = "java")

    group = rootProject.group
    version = rootProject.version

    java.sourceCompatibility = JavaVersion.valueOf(libs.versions.target.get())
    java.targetCompatibility = JavaVersion.valueOf(libs.versions.target.get())

    kotlin {
        jvmToolchain(libs.versions.jdk.get().toInt())
    }

    repositories {
        mavenCentral()
        mavenLocal()
        google()
        maven {
            url = uri("https://repo.repsy.io/mvn/${System.getenv("REPSY_USERNAME")}/thomas-release")
            credentials {
                username = System.getenv("REPSY_USERNAME")
                password = System.getenv("REPSY_PASSWORD")
            }
        }
    }

    dependencies {
        implementation(libs.bundles.kotlin.stdlib.all)
        implementation(libs.bundles.kotlinx.coroutines.all)
        implementation(libs.bundles.log.logback.all)

        implementation(libs.thomas.core)

        testImplementation(libs.bundles.junit.all)
    }

    tasks.test {
        useJUnitPlatform()
    }

    tasks.withType<KotlinCompile> {
        compilerOptions.jvmTarget.set(JvmTarget.valueOf(libs.versions.jvm.get()))
    }

    java {
        withJavadocJar()
        withSourcesJar()
    }

    publishing {
        publications {
            create<MavenPublication>("maven") {
                groupId = project.group.toString()
                artifactId = project.name
                version = project.version.toString()
                from(components["java"])

                pom {
                    packaging = "jar"
                    name.set("T.H.O.M.A.S. Cache - ${project.name}")
                    description.set("T.H.O.M.A.S. Cache module holds the cache features, functionalities and implementation to be used on other projects.")
                    licenses {
                        license {
                            name.set("MIT license")
                            url.set("https://opensource.org/licenses/MIT")
                        }
                    }
                    developers {
                        developer {
                            name.set("Nicanor Bondarenco")
                            email.set("nicanor_bondarenco@hotmail.com")
                        }
                    }
                    scm {
                        connection.set("scm:git:git://github.com/NicoBondarenco/thomas-cache.git")
                        developerConnection.set("scm:git:ssh://github.com/NicoBondarenco/thomas-cache.git")
                        url.set("https://github.com/NicoBondarenco/thomas-cache")
                    }
                }
            }
        }
        repositories {
            maven {
                val repository = if (project.version.toString().endsWith("SNAPSHOT")) {
                    "snapshot"
                } else {
                    "release"
                }
                url = URI.create("https://repo.repsy.io/mvn/${System.getenv("REPSY_USERNAME")}/thomas-$repository")
                credentials {
                    username = System.getenv("REPSY_USERNAME")
                    password = System.getenv("REPSY_PASSWORD")
                }
            }
        }
    }
}

kover {
    reports {
        total {
            verify {
                onCheck = false
                rule("Branch Coverage of Tests must be more than 95%") {
                    disabled = false
                    groupBy = APPLICATION
                    bound {
                        aggregationForGroup = COVERED_PERCENTAGE
                        coverageUnits = BRANCH
                        minValue = 95
                    }
                }
                rule("Line Coverage of Tests must be more than 95%") {
                    disabled = false
                    groupBy = APPLICATION
                    bound {
                        aggregationForGroup = COVERED_PERCENTAGE
                        coverageUnits = LINE
                        minValue = 95
                    }
                }
                rule("Instruction Coverage of Tests must be more than 95%") {
                    disabled = false
                    groupBy = APPLICATION
                    bound {
                        aggregationForGroup = COVERED_PERCENTAGE
                        coverageUnits = INSTRUCTION
                        minValue = 95
                    }
                }
            }
            xml {
                onCheck = false
            }
            html {
                onCheck = false
            }
        }
    }
}

sonar {
    properties {
        property("sonar.projectName", "T.H.O.M.A.S. Cache")
        property("sonar.projectKey", "thomas-Cache")
        property("sonar.login", System.getenv("THOMAS_CACHE_SONAR_LOGIN"))
        property("sonar.host.url", System.getenv("THOMAS_CACHE_SONAR_URL"))
        property("sonar.coverage.jacoco.xmlReportPaths", "${layout.buildDirectory.get()}/reports/kover/report.xml")
        property("sonar.verbose", true)
        property("sonar.qualitygate.wait", true)
    }
}
