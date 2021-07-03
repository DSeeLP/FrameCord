/*
 * Created by Dirk in 2021.
 * © Copyright by DSeeLP
 */

rootProject.name = "kotlincord"
include("core", "api", "test")
include("plugins")
include("plugins:moderation")
include("frontend")
include("frontend:app", "frontend:backend", "frontend:ui")

pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}