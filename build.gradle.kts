import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinJvm

plugins {
    kotlin("jvm") version "2.2.10"
    id("com.vanniktech.maven.publish") version "0.34.0"
}

group = "com.mocharealm.accompanist"
version = "0.2.1"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
}

java {
    withJavadocJar()
    withSourcesJar()
}

mavenPublishing {
    publishToMavenCentral(automaticRelease = true)

    signAllPublications()

    coordinates(group.toString(), "lyrics-core", version.toString())

    configure(KotlinJvm(
        // configures the -javadoc artifact, possible values:
        // - `JavadocJar.None()` don't publish this artifact
        // - `JavadocJar.Empty()` publish an empty jar
        // - `JavadocJar.Dokka("dokkaHtml")` when using Kotlin with Dokka, where `dokkaHtml` is the name of the Dokka task that should be used as input
        javadocJar = JavadocJar.None(),
        sourcesJar = true,
    ))

    pom {
        name = "Accompanist Lyrics Core"
        description = "A general lyrics library for Kotlin"
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