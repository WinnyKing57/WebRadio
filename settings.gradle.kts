pluginManagement {
    repositories {
        // Ordre recommandé : google, mavenCentral, puis gradlePluginPortal
        google()
        mavenCentral()
        gradlePluginPortal()
        // Ajout de repo supplémentaire si nécessaire
        maven("https://maven.pkg.jetbrains.space/public/p/kotlinx-html/maven")
    }
}

dependencyResolutionManagement {
    // Utilisez FAIL_ON_PROJECT_REPOS uniquement si vous êtes sûr que tous les dépôts sont ici.
    // repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)

    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS) // Priorise les dépôts définis ici

    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.pkg.jetbrains.space/public/p/kotlinx-html/maven")
    }
}

rootProject.name = "WebRadio"
include(":WebradioApp:app")
