import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinMultiplatform
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    val kotlinVersion = "2.3.0"
    kotlin("multiplatform") version kotlinVersion
    kotlin("plugin.serialization") version kotlinVersion
    id("com.vanniktech.maven.publish") version "0.35.0"
    id("org.jetbrains.dokka") version "2.1.0"
}

group = "com.mocharealm.accompanist"
version = providers.gradleProperty("releaseVersion").getOrElse("0.5.0")

kotlin {
    jvmToolchain(21)
    jvm()
    js(IR) {
        browser {
            testTask {
                useKarma {
                    useChromeHeadless()
                }
            }
        }
        nodejs()
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
        nodejs()
    }

    macosX64()
    macosArm64()
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain {
            dependencies {
                implementation(kotlin("stdlib"))
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.10.0")
            }
        }

        commonTest {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}

publishing {
    repositories {
        maven {
            name = "ci"
            url = layout.buildDirectory.dir("ci-maven").get().asFile.toURI()
        }
        maven {
            name = "local"
            url = uri("file:///E:/maven")
        }
    }
}

mavenPublishing {
    publishToMavenCentral(automaticRelease = true)
    if (!providers.gradleProperty("localUnsigned").map(String::toBoolean).getOrElse(false) &&
        !providers.gradleProperty("ciPackaging").map(String::toBoolean).getOrElse(false)) {
        signAllPublications()
    }

    coordinates(group.toString(), "lyrics-core", version.toString())

    configure(
        KotlinMultiplatform(
            javadocJar = JavadocJar.Dokka("dokkaGeneratePublicationHtml"),
            sourcesJar = true
        )
    )

    pom {
        name = "Accompanist Lyrics Core"
        description = "A general lyrics library for Kotlin Multiplatform"
        inceptionYear = "2025"
        url = "https://mocharealm.com/open-source"
        licenses {
            license {
                name = "The Apache License, Version 2.0"
                url = "http://www.apache.org/licenses/LICENSE-2.0.txt"
                distribution = "http://www.apache.org/licenses/LICENSE-2.0.txt"
            }
        }
        developers {
            developer {
                id = "6xingyv"
                name = "Simon Scholz"
                url = "https://github.com/6xingyv"
            }
        }
        scm {
            url = "https://github.com/6xingyv/accompanist-lyrics-core"
            connection = "scm:git:git://github.com/6xingyv/accompanist-lyrics-core.git"
            developerConnection = "scm:git:ssh://git@github.com/6xingyv/accompanist-lyrics-core.git"
        }
    }
}
