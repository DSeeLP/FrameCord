/*
 * Copyright (c) 2021 DSeeLP & FrameCord contributors
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

plugins {
    application
    java
    kotlin("jvm")
    id("com.github.johnrengelman.shadow")
    kotlin("plugin.serialization")
}

application {
    val main = "io.github.dseelp.framecord.core.CordBootstrap"
    mainClass.set(main)
    @Suppress("DEPRECATION")
    mainClassName = main

}

val coroutinesVersion: String by project
val jlineVersion: String by project
val slf4jVersion: String by project
val javassistVersion: String by project
val classgraphVersion: String by project
val koinVersion: String by project
val mariadbVersion: String by project
val mysqlVersion: String by project
val sqliteVersion: String by project
val h2Version: String by project
val ktorVersion: String by project

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
    api("io.ktor:ktor-client-cio:$ktorVersion")
    api("org.mariadb.jdbc:mariadb-java-client:$mariadbVersion")
    api("org.xerial:sqlite-jdbc:$sqliteVersion")
    api("com.h2database:h2:$h2Version")
    api(project(":rest:server"))
}

val implementationVersion = version

tasks {
    jar {
        manifest {
            attributes("prodBuild" to implementationVersion)
        }
    }
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
