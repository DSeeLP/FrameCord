/*
 * Copyright (c) 2021 DSeeLP & KotlinCord contributors
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

val defaultGroupName = "io.github.dseelp.kotlincord"
val projectVersion: String by project

group = defaultGroupName
version = projectVersion

plugins {
    base
    java
    `maven-publish`
    signing
    id("org.jetbrains.dokka") version "1.5.0" apply false

    kotlin("jvm") version "1.5.21" apply false
    kotlin("multiplatform") version "1.5.21" apply false
    id("com.github.johnrengelman.shadow") version "6.1.0" apply false
    kotlin("plugin.serialization") version "1.5.21" apply false
}

val isDeployingToCentral = System.getenv().containsKey("DEPLOY_CENTRAL")

if (isDeployingToCentral) println("Deploying to central...")

val rootProject = project

val multiplatformProjects = arrayOf<String>()

val excludedModules = arrayOf("moderation", "plugins", "privatechannels")
allprojects {

    group = defaultGroupName
    version = projectVersion

    repositories {
        mavenLocal()
        mavenCentral()
        maven(url = "https://jitpack.io")
    }

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = "11"
        kotlinOptions.freeCompilerArgs += "-Xopt-in=kotlin.RequiresOptIn"
    }

    apply(plugin = "maven-publish")
    if (multiplatformProjects.contains(project.name))
        apply(plugin = "org.jetbrains.kotlin.multiplatform")
    else apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "signing")
    apply(plugin = "org.jetbrains.dokka")
    apply(plugin = "java")

    java {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    val docsDir = File(
        rootProject.rootDir, "docs/${
            if (project == rootProject) {
                "bundled"
            } else {
                project.name
            }
        }"
    )

    val licenseFile = File(rootProject.rootDir, "LICENSE")

    val javadocJar: TaskProvider<Jar> by tasks.registering(Jar::class) {

        val configureTask: org.jetbrains.dokka.gradle.AbstractDokkaTask.() -> Unit = {
            outputDirectory.set(File(docsDir, "html"))
        }

        val task = if (project == rootProject) {
            val dokkaHtmlMultiModule by tasks.getting(org.jetbrains.dokka.gradle.DokkaMultiModuleTask::class) {
                configureTask(this)
                removeChildTasks(subprojects.filter { excludedModules.contains(it.name) })
                archiveBaseName.set("bundled")
            }
            dependsOn(dokkaHtmlMultiModule)
            dokkaHtmlMultiModule
        } else {
            val dokkaHtml by tasks.getting(org.jetbrains.dokka.gradle.DokkaTask::class) {
                configureTask(this)
            }
            dependsOn(dokkaHtml)
            dokkaHtml
        }

        archiveClassifier.set("javadoc")
        destinationDirectory.set(docsDir)
        from(task.outputDirectory)
    }

    tasks.register("generateDocs") {
        if (!excludedModules.contains(this@allprojects.name)) dependsOn(javadocJar)
    }

    tasks.withType<Jar>().onEach {
        it.from(licenseFile)
    }

    if (!excludedModules.contains(this@allprojects.name)) {
        val distributionDir = File(rootProject.rootDir, "distribution")
        val assembleGithubDistribution = tasks.register<Copy>("assembleGitHubDistribution") {
            dependsOn(tasks.build)
            if (project == rootProject) {
                from(File(rootProject.rootDir, "docs/bundled/html")) {
                    into("docs/html")
                }
                from(File(rootProject.rootDir, "templates"))
                dependsOn(javadocJar)
                from(javadocJar.get().archiveFile) {
                    rename { "javadoc.jar" }
                    into("docs")
                }
            } else if (project.name == "core") {
                val shadowJar = tasks.getByName<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar")
                from(shadowJar.archiveFile) {
                    rename { "${project.name}.jar" }
                }
            }
            into(distributionDir)
        }

        tasks.register<Zip>("gitHubDistribution") {
            dependsOn(assembleGithubDistribution)
            if (project != rootProject) return@register
            from(distributionDir)
            //from(File(distributionDir, "core.jar"))
            archiveFileName.set("kotlincord.zip")
        }
    }

    val sourcesJar by tasks.registering(Jar::class) {
        archiveClassifier.set("sources")
        from(sourceSets.main.get().allSource)
    }

    publishing {
        if (excludedModules.contains(this@allprojects.name)) return@publishing
        if (rootProject == this@allprojects) return@publishing
        repositories {
            if (isDeployingToCentral) maven(url = "https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/") {
                credentials {
                    username = System.getenv("MAVEN_USERNAME")
                    password = System.getenv("MAVEN_PASSWORD")
                }
            } else mavenLocal()
        }
        publications {
            register(this@allprojects.name, MavenPublication::class) {
                from(components["kotlin"])
                artifact(javadocJar.get())
                artifact(sourcesJar.get())

                pom {
                    url.set("https://github.com/DSeeLP/Kommon")
                    name.set("KotlinCord")
                    description.set("KotlinCord, an simple DiscordBot Framework for Kotlin")
                    developers {
                        developer {
                            name.set("DSeeLP")
                            organization.set("com.github.dseelp")
                            organizationUrl.set("https://www.github.com/DSeeLP")
                        }
                    }
                    licenses {
                        license {
                            name.set("MIT LICENSE")
                            url.set("https://www.opensource.org/licenses/mit-license.php")
                        }
                    }
                    scm {
                        connection.set("scm:git:git://github.com/DSeeLP/KotlinCord.git")
                        developerConnection.set("scm:git:git://github.com/DSeeLP/KotlinCord.git")
                        url.set("https://github.com/DSeeLP/KotlinCord/")
                    }
                }
            }
        }
    }

    signing {
        if (!isDeployingToCentral) return@signing
        useInMemoryPgpKeys(
            //System.getenv("SIGNING_ID"),
            System.getenv("SIGNING_KEY"),
            System.getenv("SIGNING_PASSWORD")
        )
        publishing.publications.onEach {
            sign(it)
        }
    }
}

tasks {
    wrapper {
        gradleVersion = "7.1.1"
    }
}