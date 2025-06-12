import org.jetbrains.kotlin.gradle.plugin.KotlinPluginWrapper

plugins {
    id("com.android.application") version "8.2.2" apply false
    kotlin("android") version "1.9.20" apply false
    id("com.google.devtools.ksp") version "1.9.20-1.0.13" apply false
    id("androidx.navigation.safeargs.kotlin") version "2.7.7" apply false
}
