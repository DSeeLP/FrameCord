/*
 * Created by Dirk in 2021.
 * © Copyright by DSeeLP
 */

plugins {
    kotlin("jvm")
    id("com.github.johnrengelman.shadow")
    kotlin("plugin.serialization")
}

dependencies {
    compileOnly(kotlin("stdlib-jdk8"))
    compileOnly(kotlin("reflect"))
    compileOnly(project(":core"))
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

    register<Copy>("copyShadow") {
        from(shadowJar.get().archiveFile.get().asFile)
        into(File("../../run/plugins/"))
        dependsOn(build)
    }
}
