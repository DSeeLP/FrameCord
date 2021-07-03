/*
 * Created by Dirk in 2021.
 * © Copyright by DSeeLP
 */

val defaultGroupName = "io.github.dseelp.kotlincord"
val defaultVersion = "0.1"

group = defaultGroupName
version = defaultVersion

plugins {
    base
    java
    `maven-publish`
    signing
    id("org.jetbrains.dokka") version "1.4.32" apply false

    kotlin("jvm") version "1.5.10" apply false
    kotlin("multiplatform") version "1.5.10" apply false
    id("com.github.johnrengelman.shadow") version "6.1.0" apply false
    kotlin("plugin.serialization") version "1.5.10" apply false
    id("org.jetbrains.compose") version (System.getenv("COMPOSE_TEMPLATE_COMPOSE_VERSION") ?: "0.5.0-build229") apply false
}

val isDeployingToCentral = System.getenv().containsKey("DEPLOY_CENTRAL")

if (isDeployingToCentral) println("Deploying to central...")

allprojects {

    group = defaultGroupName
    version = defaultVersion

    repositories {
        mavenLocal()
        mavenCentral()
        maven(url = "https://m2.dv8tion.net/releases")
        maven(url = "https://jitpack.io")
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = "11"
        kotlinOptions.freeCompilerArgs += "-Xopt-in=kotlin.RequiresOptIn"
    }

    apply(plugin = "maven-publish")
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "signing")
    apply(plugin = "org.jetbrains.dokka")
    apply(plugin = "java")

    java {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    val dokkaHtml by tasks.getting(org.jetbrains.dokka.gradle.DokkaTask::class)

    val javadocJar: TaskProvider<Jar> by tasks.registering(Jar::class) {
        dependsOn(dokkaHtml)
        archiveClassifier.set("javadoc")
        from(dokkaHtml.outputDirectory)
    }

    val sourcesJar by tasks.registering(Jar::class) {
        archiveClassifier.set("sources")
        from(sourceSets.main.get().allSource)
    }


    val excludedModules = arrayOf("test", "frontend", "app", "backend")

    publishing {
        if (excludedModules.contains(this@allprojects.name)) return@publishing
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
