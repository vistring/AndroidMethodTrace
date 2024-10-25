package com.vistring.trace

import com.android.build.api.artifact.ScopedArtifact
import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.api.variant.ScopedArtifacts
import com.android.build.gradle.AppPlugin
import com.vistring.trace.config.VSMethodTraceConfig
import com.vistring.trace.config.VSMethodTraceInitConfig
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
import org.gradle.kotlin.dsl.extra
import org.gradle.kotlin.dsl.register
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream

class VSMethodTracePlugin : Plugin<Project> {

    companion object {

        const val TAG = "VSMethodTracePlugin"
        const val EXT_METHOD_TRACE_CONFIG = "methodTraceConfig"

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

        private val methodTraceConfig =
            project.extra[EXT_METHOD_TRACE_CONFIG] as VSMethodTraceConfig

        private var isMergeOutputFileStr = project
            .properties["vs_method_trace_is_merge_output_file"]
            ?.toString() ?: ""

        @TaskAction
        fun taskAction() {

            val classPathMatcher = PathMatcher(
                enableLog = methodTraceConfig.enableLog,
                includePrefixSet = methodTraceConfig.includePackagePrefixSet,
                excludePrefixSet = methodTraceConfig.excludePackagePrefixSet,
                includePatternSet = methodTraceConfig.includePackagePatternSet,
                excludePatternSet = methodTraceConfig.excludePackagePatternSet,
                matchAll = methodTraceConfig.matchAll,
            ).apply {
                if (methodTraceConfig.enableLog) {
                    println("classPathMatcher: $this")
                }
            }

            val methodAnnoPathMatcher = PathMatcher(
                enableLog = methodTraceConfig.enableLog,
                includeSet = methodTraceConfig.includeMethodAnnoSet,
                excludeSet = methodTraceConfig.excludeMethodAnnoSet,
                includePrefixSet = methodTraceConfig.includeMethodAnnoPrefixSet,
                excludePrefixSet = methodTraceConfig.excludeMethodAnnoPrefixSet,
                includePatternSet = methodTraceConfig.includeMethodAnnoPatternSet,
                excludePatternSet = methodTraceConfig.excludeMethodAnnoPatternSet,
                matchAll = methodTraceConfig.matchAll,
            ).apply {
                if (methodTraceConfig.enableLog) {
                    println("methodAnnoPathMatcher: $this")
                }
            }

            // 读取配置的属性 isMergeOutputFile
            val isMergeOutputFile = runCatching {
                isMergeOutputFileStr.toBoolean()
            }.getOrNull() ?: false

            // /Users/xxx/Documents/code/android/github/xxx/build/intermediates/classes/debug/ALL/classes.jar
            val outputFile = output.asFile.get()
            val allJarList = allJars.get()

            if (methodTraceConfig.enableLog) {
                println("$TAG, isMergeOutputFile = $isMergeOutputFile")
                println("$TAG, output = ${outputFile.path}, outputFileIsExist = ${outputFile.exists()}")
            }

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

            val jarOutput = JarOutputStream(
                BufferedOutputStream(
                    FileOutputStream(
                        output.get().asFile
                    )
                )
            )

            // 重置方法标志
            BytecodeInstrumentation.resetMethodFlag()

            targetAllJars.forEach { file ->
                val jarFile = JarFile(file.asFile)
                jarFile.entries().iterator().forEach { jarEntry ->
                    // jarEntry.name
                    // kotlin/ranges/ClosedRange.kotlin_metadata
                    // kotlin/ranges/RangesKt__RangesKt.class
                    try {
                        jarOutput.putNextEntry(JarEntry(jarEntry.name))
                        if (
                            jarEntry.isDirectory ||
                            !jarEntry.name.endsWith(
                                ".class"
                            )
                        ) {
                            jarFile.getInputStream(jarEntry).use {
                                it.copyTo(jarOutput)
                            }
                        } else {
                            jarOutput.write(
                                jarFile.getInputStream(jarEntry).use { classFileInputStream ->
                                    BytecodeInstrumentation.tryInstrument(
                                        resourcesFolder = methodTraceConfig.resourcesFolder,
                                        costTimeThreshold = methodTraceConfig.costTimeThreshold,
                                        enableLog = methodTraceConfig.enableLog,
                                        classPathMatcher = classPathMatcher,
                                        methodAnnoPathMatcher = methodAnnoPathMatcher,
                                        classFullName = jarEntry.name,
                                        classFileInputStream = classFileInputStream,
                                    )
                                }
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
                        // com/vistring/vlogger/android/media/Mp4AudioDecoder.class
                        jarOutput.putNextEntry(
                            JarEntry(
                                relativePath.replace(
                                    File.separatorChar,
                                    '/'
                                )
                            )
                        )
                        jarOutput.write(
                            file.inputStream().use { classFileInputStream ->
                                BytecodeInstrumentation.tryInstrument(
                                    resourcesFolder = methodTraceConfig.resourcesFolder,
                                    costTimeThreshold = methodTraceConfig.costTimeThreshold,
                                    enableLog = methodTraceConfig.enableLog,
                                    classPathMatcher = classPathMatcher,
                                    methodAnnoPathMatcher = methodAnnoPathMatcher,
                                    classFullName = relativePath,
                                    classFileInputStream = classFileInputStream,
                                )
                            }
                        )
                        jarOutput.closeEntry()
                    }
                }
            }

            jarOutput.close()

            if (methodTraceConfig.enableLog) {
                println("----------- outputFilePath: ${output.asFile.get().path}")
            }

        }

    }

    override fun apply(project: Project) {

        project.plugins.findPlugin(AppPlugin::class.java) ?: return

        // 添加扩展
        project.extensions.add("vsMethodTraceConfig", VSMethodTraceInitConfig::class.java)

        with(project) {

            plugins.withType(AppPlugin::class.java) {

                val androidComponents = extensions
                    .findByType(AndroidComponentsExtension::class.java)

                androidComponents?.onVariants { variant ->

                    val resourcesFolderFile = variant.sources.resources?.all?.orNull?.find {
                        it.asFile.path.contains("main/resources")
                    }?.let { directory ->
                        println("variant.name = ${variant.name} directory.asFile.path ====== ${directory.asFile.path}")
                        directory.asFile
                    }

                    val vsMethodTraceConfig =
                        project.extensions.findByType(VSMethodTraceInitConfig::class.java)
                    val isMethodTraceEnable = vsMethodTraceConfig?.enable ?: true

                    if (isMethodTraceEnable) {

                        // 存入 extra
                        project
                            .extra
                            .set(
                                EXT_METHOD_TRACE_CONFIG,
                                VSMethodTraceConfig(
                                    enableLog = vsMethodTraceConfig?.enableLog ?: false,
                                    resourcesFolder = resourcesFolderFile,
                                    costTimeThreshold = vsMethodTraceConfig?.costTimeThreshold
                                        ?: Long.MAX_VALUE,
                                    includePackagePrefixSet = vsMethodTraceConfig?.includePackagePrefixSet
                                        ?: emptySet(),
                                    excludePackagePrefixSet = vsMethodTraceConfig?.excludePackagePrefixSet
                                        ?: emptySet(),
                                    includePackagePatternSet = vsMethodTraceConfig?.includePackagePatternSet
                                        ?: emptySet(),
                                    excludePackagePatternSet = vsMethodTraceConfig?.excludePackagePatternSet
                                        ?: emptySet(),
                                    includeMethodAnnoSet = vsMethodTraceConfig?.includeMethodAnnoSet
                                        ?: emptySet(),
                                    excludeMethodAnnoSet = vsMethodTraceConfig?.excludeMethodAnnoSet
                                        ?: emptySet(),
                                    includeMethodAnnoPrefixSet = vsMethodTraceConfig?.includeMethodAnnoPrefixSet
                                        ?: emptySet(),
                                    excludeMethodAnnoPrefixSet = vsMethodTraceConfig?.excludeMethodAnnoPrefixSet
                                        ?: emptySet(),
                                    includeMethodAnnoPatternSet = vsMethodTraceConfig?.includeMethodAnnoPatternSet
                                        ?: emptySet(),
                                    excludeMethodAnnoPatternSet = vsMethodTraceConfig?.excludeMethodAnnoPatternSet
                                        ?: emptySet(),
                                    matchAll = vsMethodTraceConfig?.matchAll ?: false,
                                )
                            )

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

}