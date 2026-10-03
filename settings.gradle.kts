dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven("https://repo.viaversion.com")
    }
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "viaminestom"
