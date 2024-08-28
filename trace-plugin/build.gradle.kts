import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.archivesName

plugins {
    `kotlin-dsl`
    id("maven-publish")
}

group = "com.vistring"
archivesName = "method-trace-plugin"
version = "1.0.0"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(JavaVersion.VERSION_17.toString()))
    }
}

// 配置模块的 freeCompilerArgs 参数
/*tasks.withType(KotlinCompile).configureEach {
    kotlinOptions {
        freeCompilerArgs += [
                "-Xjvm-default=all",
        ]
    }
}*/

/*kotlin {
    jvmToolchain(JavaVersion.VERSION_17)
}*/

/*repositories {
    google()
    mavenCentral()
}*/

dependencies {
    implementation(gradleApi())
    implementation(localGroovy())
    implementation("org.ow2.asm:asm-commons:9.7")
    implementation("org.ow2.asm:asm-util:9.7")
    implementation("com.android.tools.build:gradle:8.1.4")
    implementation("com.google.code.gson:gson:2.11.0")
    implementation("com.joom.grip:grip:0.9.1")
    implementation("org.javassist:javassist:3.26.0-GA")
    implementation("com.github.gundy:semver4j:0.16.4")

    // 引入 junit 依赖
    testImplementation("junit:junit:4.13.2")

}

gradlePlugin {
    plugins {
        register("VSMethodTracePlugin") {
            id = "com.vistring.trace.method.plugin"
            implementationClass = "com.vistring.trace.VSMethodTracePlugin"
        }
    }
}

java {
    withSourcesJar()
    withJavadocJar()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = group.toString()
            artifactId = archivesName.get()
            version = version
            from(components["java"])
        }
    }
//    repositories {
//        maven {
//            name = "temp"
//            url = uri("$buildDir/LocalRepo")
//        }
//    }
}
