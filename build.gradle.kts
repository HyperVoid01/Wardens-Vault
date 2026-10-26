// Top-level build file where you can add configuration options common to all sub-projects/modules.

/*
AGP 9's built-in Kotlin defaults to Kotlin 2.2, but the current Firebase libraries are
compiled with Kotlin 2.4. Declaring the newer Kotlin Gradle plugin here makes AGP use it
(this is the method from the AGP 9 release notes).
 */
buildscript {
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.4.20")
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.google.gms.google.services) apply false
}
