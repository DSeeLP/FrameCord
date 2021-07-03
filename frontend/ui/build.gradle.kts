plugins {
    kotlin("jvm")
}

val ktorVersion: String by project

dependencies {
    compileOnly(project(":api"))
}