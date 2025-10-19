
@file:Suppress("UnstableApiUsage") // <-- Anotación aquí, aplicada al archivo entero

pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }

    plugins {
        // Ojo con la versión hardcoded aquí. Puede diverger con la de libs.version.toml
        id("com.google.devtools.ksp") version "2.2.20-2.0.3" apply false
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "ProcrastinApp"
include(":app")
 