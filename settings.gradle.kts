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
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google {
            content {
                includeGroupByRegex("androidx(\\..*)?")
                includeGroupByRegex("com\\.android(\\..*)?")
                includeGroupByRegex("com\\.google\\.android(\\..*)?")
                includeGroupByRegex("com\\.google\\.firebase(\\..*)?")
                includeGroupByRegex("com\\.google\\.testing\\.platform(\\..*)?")
            }
        }
        mavenCentral {
            content {
                excludeGroupByRegex("androidx(\\..*)?")
                excludeGroupByRegex("com\\.android(\\..*)?")
                excludeGroupByRegex("com\\.google\\.android\\..*")
                excludeGroupByRegex("com\\.google\\.firebase(\\..*)?")
                excludeGroupByRegex("com\\.google\\.testing\\.platform(\\..*)?")
            }
        }
    }
}

rootProject.name = "tablet-demo"
include(":app")
include(":ui")
include(":domain")
include(":data")
include(":benchmark")
