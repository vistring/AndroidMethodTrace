// Top-level build file where you can add configuration options common to all sub-projects/modules.

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.jetbrains.kotlin.android) apply false
    alias(libs.plugins.jetbrains.kotlin.jvm) apply false
    alias(libs.plugins.android.library) apply false
}

// 通过 includeBuild 引入的 trace-plugin 作为独立的模块，默认并不会包含进来，需要手动添加
tasks.register("publishToMavenLocal") {
    dependsOn(gradle.includedBuild("trace-plugin").task(":publishToMavenLocal"))
    dependsOn(subprojects.filter { it.tasks.findByName("publishToMavenLocal") != null }
        .map { it.tasks.named("publishToMavenLocal") })
}