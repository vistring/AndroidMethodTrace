// Top-level build file where you can add configuration options common to all sub-projects/modules.

buildscript {
    repositories {
        maven {
            url = uri(File(rootDir, "LocalRepo").path)
        }
    }
    dependencies {
        classpath("com.vistring:method-trace:1.0.0")
    }
}


plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.jetbrains.kotlin.android) apply false
    alias(libs.plugins.jetbrains.kotlin.jvm) apply false
    alias(libs.plugins.android.library) apply false
}