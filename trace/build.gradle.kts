import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.archivesName

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.jetbrains.kotlin.android)
    id("maven-publish")
}

group = "com.vistring"
archivesName = "method-trace"
version = "1.0.0"

android {
    namespace = "com.vistring.trace"
    compileSdk = 34

    defaultConfig {
        minSdk = 24

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_17.toString()
    }

    publishing {
        singleVariant("release") {
            // if you don't want sources/javadoc, remove these lines
            withSourcesJar()
        }
    }

}

dependencies {

    compileOnly(libs.androidx.annotation.jvm)
    // okhttp
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // 引入 junit 依赖
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito:mockito-core:4.8.1")
    // https://mvnrepository.com/artifact/org.mockito.kotlin/mockito-kotlin
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.4.0")

}

tasks.register<Jar>("androidSourcesJar") {
    archiveClassifier.set("sources")
    from(
        android.sourceSets.getByName("main").java.srcDirs,
    )
}

artifacts {
    archives(tasks["androidSourcesJar"])
}

afterEvaluate {
    publishing {
        publications {
            // Creates a Maven publication called "release".
            create<MavenPublication>("release") {
                // Applies the component for the release build variant.
                from(components["release"])
                groupId = group.toString()
                artifactId = archivesName.get()
                version = version
            }
        }
    }
}