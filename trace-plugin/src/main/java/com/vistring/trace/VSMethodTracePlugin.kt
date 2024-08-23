package com.vistring.trace

import com.android.build.api.artifact.ScopedArtifact
import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.api.variant.ScopedArtifacts
import com.android.build.gradle.AppPlugin
import com.joom.grip.Grip
import com.joom.grip.GripFactory
import com.joom.grip.annotatedWith
import com.joom.grip.classes
import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.CompileClasspath
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.register
import org.objectweb.asm.Opcodes
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream

class VSMethodTracePlugin : Plugin<Project> {

    companion object {

        const val TAG = "VSMethodTracePlugin"

    }

    abstract class ModifyClassesTask : DefaultTask() {

        @get:InputFiles
        abstract val allJars: ListProperty<RegularFile>

        @get:InputFiles
        abstract val allDirectories: ListProperty<Directory>

        @get:OutputFile
        abstract val output: RegularFileProperty

        @get:Classpath
        abstract val bootClasspath: ListProperty<RegularFile>

        @get:CompileClasspath
        abstract var classpath: FileCollection

        private var isMergeOutputFileStr = project
            .properties["vs_method_trace_is_merge_output_file"]
            ?.toString() ?: ""

        @TaskAction
        fun taskAction() {

            // 读取配置的属性 isMergeOutputFile

            val isMergeOutputFile = runCatching {
                isMergeOutputFileStr.toBoolean()
            }.getOrNull() ?: false

            // /Users/xxx/Documents/code/android/github/xxx/build/intermediates/classes/debug/ALL/classes.jar
            val outputFile = output.asFile.get()
            val allJarList = allJars.get()

            println("$TAG, isMergeOutputFile = $isMergeOutputFile")
            println("$TAG, output = ${outputFile.path}, outputFileIsExist = ${outputFile.exists()}")

            // 输入的 jar、aar、源码
            val inputs = (allJarList + allDirectories.get()).map { it.asFile.toPath() }

            // 系统依赖
            val classPaths = bootClasspath.get().map { it.asFile.toPath() }
                .toSet() + classpath.files.map { it.toPath() }

            val grip: Grip =
                GripFactory.newInstance(Opcodes.ASM9)
                    .create(classPaths + inputs)

            val targetAllJars = if (isMergeOutputFile) {
                // 是否 AllJars 中有输出文件
                val isAllJarsContainsOutputFile = allJarList
                    .find {
                        it.asFile == outputFile
                    } != null
                if (isAllJarsContainsOutputFile) {
                    val tempFile = File.createTempFile(
                        "vsMethodTraceOutput",
                        ".${outputFile.extension}"
                    )
                    println("$TAG, tempFile = ${tempFile.path}")
                    outputFile.copyTo(
                        target = tempFile,
                        overwrite = true,
                    )
                    allJarList
                        .filter {
                            it.asFile != outputFile
                        } + listOf(
                        RegularFile { tempFile }
                    )
                } else {
                    allJarList
                }
            } else {
                allJarList
            }

            // 找到所有满足条件的 class
            val moduleNameMap = grip
                .select(classes)
                .from(inputs)
                .where(
                    annotatedWith(
                        annotationType = com.joom.grip.mirrors.getType(
                            descriptor = "Lcom/xiaojinzi/component/anno/support/ModuleApplicationAnno;",
                        )
                    )
                )
                .execute()
                .classes
                .associate {
                    it.name
                        .removePrefix(prefix = "com.xiaojinzi.component.impl.")
                        .removeSuffix(suffix = "ModuleGenerated") to "${it.name}.class"
                }

            println("$TAG, moduleNameMap = $moduleNameMap")

            val jarOutput = JarOutputStream(
                BufferedOutputStream(
                    FileOutputStream(
                        output.get().asFile
                    )
                )
            )

            targetAllJars.forEach { file ->
                val jarFile = JarFile(file.asFile)
                jarFile.entries().iterator().forEach { jarEntry ->
                    try {
                        jarOutput.putNextEntry(JarEntry(jarEntry.name))
                        if (jarEntry.isDirectory) {
                            jarFile.getInputStream(jarEntry).use {
                                it.copyTo(jarOutput)
                            }
                        } else {
                            jarOutput.write(
                                AsmTestUtil.test(
                                    name = jarEntry.name,
                                    classFileInputStream = jarFile.getInputStream(jarEntry),
                                )
                            )
                        }
                        jarFile.getInputStream(jarEntry).use {
                            it.copyTo(jarOutput)
                        }
                        jarOutput.closeEntry()
                    } catch (e: Exception) {
                        // ignore
                    }
                }
                jarFile.close()
            }

            allDirectories.get().forEach { directory ->
                directory.asFile.walk().forEach { file ->
                    if (file.isFile) {
                        val relativePath = directory.asFile.toURI().relativize(file.toURI()).path
                        // println("relativePath = $relativePath")
                        jarOutput.putNextEntry(
                            JarEntry(
                                relativePath.replace(
                                    File.separatorChar,
                                    '/'
                                )
                            )
                        )
                        jarOutput.write(
                            AsmTestUtil.test(
                                name = relativePath,
                                classFileInputStream = file.inputStream(),
                            )
                        )
                        jarOutput.closeEntry()
                    }
                }
            }

            jarOutput.close()

            println("----------- outputFilePath: ${output.asFile.get().path}")

        }

    }

    override fun apply(project: Project) {

        with(project) {

            plugins.withType(AppPlugin::class.java) {

                val androidComponents = extensions
                    .findByType(AndroidComponentsExtension::class.java)

                androidComponents?.onVariants { variant ->
                    val name = "${variant.name}VSMethodTrace"
                    val taskProvider = tasks.register<ModifyClassesTask>(name) {
                        group = "vsMethodTrace"
                        description = name
                        bootClasspath.set(androidComponents.sdkComponents.bootClasspath)
                        classpath = variant.compileClasspath
                    }

                    variant.artifacts.forScope(ScopedArtifacts.Scope.ALL)
                        .use(taskProvider)
                        .toTransform(
                            ScopedArtifact.CLASSES,
                            ModifyClassesTask::allJars,
                            ModifyClassesTask::allDirectories,
                            ModifyClassesTask::output
                        )

                }

            }

        }

    }

}