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

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("reflect"))
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
    api("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:$coroutinesVersion")
    api("net.dv8tion:JDA:$jdaVersion")
    api("io.insert-koin:koin-core:$koinVersion")
    api("io.insert-koin:koin-logger-slf4j:$koinVersion")
    api("org.fusesource.jansi:jansi:2.3.2")
    api("com.uchuhimo:konf:1.1.2")
    api("io.github.dseelp.kommon:command:0.3.0") {
        isChanging = true
    }
    api("org.jetbrains.kotlinx:kotlinx-serialization-json:1.2.1")
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
