package com.vistring.trace.config

/**
 * Gradle 中的配置类
 * 这里必须写 open class, 不能用 data class, 否则在 Gradle 插件使用中会出错
 */
open class VSMethodTraceInitConfig(
    var enable: Boolean? = null,
    var enableLog: Boolean? = null,
    var enableAdvancedMatch: Boolean? = null,
    var includePackagePrefixSet: Set<String>? = null,
    var excludePackagePrefixSet: Set<String>? = null,
    var includePackagePatternSet: Set<String>? = null,
    var excludePackagePatternSet: Set<String>? = null,
)

data class VSMethodTraceConfig(
    val enableLog: Boolean,
    val enableAdvancedMatch: Boolean,
    val includePackagePrefixSet: Set<String>,
    val excludePackagePrefixSet: Set<String>,
    val includePackagePatternSet: Set<String>,
    val excludePackagePatternSet: Set<String>,
)