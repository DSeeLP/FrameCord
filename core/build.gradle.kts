/*
 * Created by Dirk in 2021.
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
val slf4jVersion: String by project
val javassistVersion: String by project
val classgraphVersion: String by project
val ktorVersion: String by project
val koinVersion: String by project
val mariadbVersion: String by project
val mysqlVersion: String by project
val sqliteVersion: String by project
val h2Version: String by project

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("reflect"))
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
    api(project(":api"))
    api("org.slf4j:jul-to-slf4j:$slf4jVersion")
    api("io.insert-koin:koin-logger-slf4j:$koinVersion")
    //api("org.slf4j:slf4j-simple:1.7.25")
    api("org.jline:jline:$jlineVersion")
    api("org.jline:jline-terminal-jna:$jlineVersion")
    api("org.jline:jline-reader:$jlineVersion")
    api("org.javassist:javassist:$javassistVersion")
    api("io.github.classgraph:classgraph:$classgraphVersion")
    api("io.ktor:ktor-client-core:$ktorVersion")
    api("io.ktor:ktor-client-cio:$ktorVersion")
    api("io.ktor:ktor-client-serialization:$ktorVersion")
    api("org.mariadb.jdbc:mariadb-java-client:$mariadbVersion")
    api("org.xerial:sqlite-jdbc:$sqliteVersion")
    api("com.h2database:h2:$h2Version")
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
