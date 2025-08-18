import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinMultiplatform

plugins {
    kotlin("multiplatform") version "2.2.10"
    id("com.vanniktech.maven.publish") version "0.34.0"
}

group = "com.mocharealm.accompanist"
version = "0.3.0"

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(21)
    jvm()

//    js(IR) {
//        browser()
//        nodejs()
//    }

    iosX64()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(kotlin("stdlib"))
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }

        val jvmMain by getting {
            dependencies {
            }
        }

        val jvmTest by getting {
            dependencies {
            }
        }
    }
}

mavenPublishing {
    publishToMavenCentral(automaticRelease = true)
    signAllPublications()

    coordinates(group.toString(), "lyrics-core", version.toString())

    configure(
        KotlinMultiplatform(
            javadocJar = JavadocJar.None(),
            sourcesJar = true,
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
            url = "https://github.com/6xingyv/Accompanist-Lyrics"
            connection = "scm:git:git://github.com/6xingyv/Accompanist-Lyrics.git"
            developerConnection = "scm:git:ssh://git@github.com/6xingyv/Accompanist-Lyrics.git"
        }
    }
}