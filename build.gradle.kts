import com.jfrog.bintray.gradle.BintrayExtension
import org.camuthig.credentials.core.FileCredentialsStore
import org.camuthig.credentials.core.KeyFileMissing
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.3.20"
    `java-library`
    `maven-publish`
    id("com.jfrog.bintray") version "1.8.4"
}

group = "org.camuthig.credentials"
version = "0.1.0"

repositories {
    mavenCentral()
    jcenter()
}

buildscript {
    repositories {
        maven {
            url = uri("https://dl.bintray.com/camuthig/maven")
        }
    }
    dependencies {
        classpath("org.camuthig.credentials:core:0.1.0")
    }
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("com.typesafe:config") {
        version {
            require("[1.0,2.0[")
            prefer("1.4")
        }
    }
    testImplementation(kotlin("test-junit"))
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

kotlin.sourceSets["main"].kotlin.srcDirs("src")
kotlin.sourceSets["test"].kotlin.srcDirs("test")

sourceSets["main"].resources.srcDirs("resources")
sourceSets["test"].resources.srcDirs("test/resources")

tasks.register<Jar>("sourcesJar") {
    from(sourceSets.main.get().allJava)
    archiveClassifier.set("sources")
}

tasks.register<Jar>("javadocJar") {
    from(tasks.javadoc)
    archiveClassifier.set("javadoc")
}

publishing {
    repositories {
        maven {
            url = uri("$buildDir/repo")
        }
    }
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            artifact(tasks["sourcesJar"])
            artifact(tasks["javadocJar"])

            artifactId = "core"

            pom {
                licenses {
                    license {
                        name.set("The Apache Software License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                        distribution.set("repo")
                    }
                }
                developers {
                    developer {
                        id.set("camuthig")
                        name.set("Chris Muthig")
                    }
                }

                scm {
                    url.set("https://github.com/camuthig/kotlin-credentials")
                }
            }
        }
    }
}

bintray {
    user = credentials("bintray.user")
    key = credentials("bintray.key")
    publish = true
    setPublications("maven")
    pkg(delegateClosureOf<BintrayExtension.PackageConfig> {
        repo = "maven"
        name = "credentials-core"
        websiteUrl = "https://camuthig.dev"
        githubRepo = "camuthig/kotlin-credentials"
        vcsUrl = "https://github.com/camuthig/kotlin-credentials"
        description = "A library for storing credentials within a project in an encrypted file"
        setLabels("kotlin")
        setLicenses("Apache-2.0")
        desc = description
    })
}

fun getStore(): FileCredentialsStore {
    return FileCredentialsStore(File("publishing.conf.enc"), File("publishing.key"))
}

fun credentials(key: String): String {
    return try {
        getStore().load().getString(key)
    } catch (e: KeyFileMissing) {
        // These values are only necessary for publishing, so we can ignore them if, for example, we are just running
        // the test suite
        ""
    }
}

tasks.register<DefaultTask>("credentialsGenerate") {
    doLast {
        getStore().generate()
    }
}

tasks.register<DefaultTask>("credentialsUpsert") {
    doLast {
        getStore().upsert(System.getProperty("credentials.key"), System.getProperty("credentials.value"))
    }
}

tasks.register<DefaultTask>("credentialsDelete") {
    doLast {
        getStore().delete(System.getProperty("credentials.key"))
    }
}
