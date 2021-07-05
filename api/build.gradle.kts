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
    val mainClazz = ""
    mainClass.set(mainClass)
    @Suppress("DEPRECATION")
    mainClassName = mainClazz
}

val coroutinesVersion: String by project
val kordVersion: String by project
val koinVersion: String by project
val configurateVersion: String by project
val datetimeVersion: String by project
val kommonVersion: String by project
val serializationVersion: String by project
val jansiVersion: String by project
val exposedVersion: String by project
val hikaricpVersion: String by project

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("reflect"))
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
    api("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:$coroutinesVersion")
    api("dev.kord:kord-core:$kordVersion")
    api("io.insert-koin:koin-core:$koinVersion")
    api("org.fusesource.jansi:jansi:$jansiVersion")
    api("io.github.dseelp.kommon:command:$kommonVersion") {
        isChanging = true
    }
    api("org.jetbrains.kotlinx:kotlinx-serialization-json:$serializationVersion")
    api("org.spongepowered:configurate-jackson:$configurateVersion")
    api("org.spongepowered:configurate-yaml:$configurateVersion")
    api("org.spongepowered:configurate-extra-kotlin:$configurateVersion")
    api("org.jetbrains.kotlinx:kotlinx-datetime:$datetimeVersion")
    api("org.jetbrains.exposed:exposed-core:$exposedVersion")
    api("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    api("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    api("com.zaxxer:HikariCP:$hikaricpVersion")
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
