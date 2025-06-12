pluginManagement {
    repositories {
        google()
        mavenCentral() // Avant gradlePluginPortal, pour éviter les erreurs de dépendances Kotlin/KSP
        gradlePluginPortal()
        maven("https://maven.pkg.jetbrains.space/public/p/kotlinx-html/maven")
    }
}

dependencyResolutionManagement {
    // repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS) // désactivation temporaire afin de tester la résolution des dépendances sans blocage

    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS) // ligne ajoutée pour tester en priorité les dépôts définis ici

    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.pkg.jetbrains.space/public/p/kotlinx-html/maven")
    }
}

rootProject.name = "WebRadio"
include(":WebradioApp:app") // chemin correct pour la structure du projet
