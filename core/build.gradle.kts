/*
 * Created by Dirk on 19.6.2021.
 * © Copyright by DSeeLP
 */

plugins {
    application
    kotlin("jvm")
    id("com.github.johnrengelman.shadow")
    kotlin("plugin.serialization")
}

application {
    val main = "de.dseelp.kotlincord.core.CordBootstrap"
    mainClass.set(main)
    @Suppress("DEPRECATION")
    mainClassName = main
}

val coroutinesVersion: String by project
val jlineVersion: String by project

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("reflect"))
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
    api(project(":api"))
    api("org.slf4j:jul-to-slf4j:1.7.25")
    //api("org.slf4j:slf4j-simple:1.7.25")
    api("org.jline:jline:$jlineVersion")
    api("org.jline:jline-terminal-jna:$jlineVersion")
    api("org.jline:jline-reader:$jlineVersion")
    api("org.javassist:javassist:3.28.0-GA")
    api("io.github.classgraph:classgraph:4.8.108")
}
java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

val implementationVersion = version

tasks {
    named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
        archiveBaseName.set("shadow")
        mergeServiceFiles()
        manifest {
            attributes(mapOf("Implementation-Version" to implementationVersion))
        }
    }
}

tasks {
    build {
        dependsOn(shadowJar)
    }
}
