/*
 * Created by Dirk on 19.6.2021.
 * © Copyright by DSeeLP
 */

plugins {
    kotlin("jvm")
    id("com.github.johnrengelman.shadow")
    kotlin("plugin.serialization")
}

val coroutinesVersion: String by project
val jdaVersion: String by project
val koinVersion: String by project
val configurateVersion: String by project
val datetimeVersion: String by project
val kommonVersion: String by project
val serializationVersion: String by project
val jansiVersion: String by project

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("reflect"))
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
    api("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:$coroutinesVersion")
    api("net.dv8tion:JDA:$jdaVersion")
    api("io.insert-koin:koin-core:$koinVersion")
    api("io.insert-koin:koin-logger-slf4j:$koinVersion")
    api("org.fusesource.jansi:jansi:$jansiVersion")
    api("io.github.dseelp.kommon:command:$kommonVersion") {
        isChanging = true
    }
    api("org.jetbrains.kotlinx:kotlinx-serialization-json:$serializationVersion")
    api("org.spongepowered:configurate-hocon:$configurateVersion")
    api("org.spongepowered:configurate-gson:$configurateVersion")
    api("org.spongepowered:configurate-yaml:$configurateVersion")
    api("org.spongepowered:configurate-extra-kotlin:$configurateVersion")
    api("org.jetbrains.kotlinx:kotlinx-datetime:$datetimeVersion")
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
