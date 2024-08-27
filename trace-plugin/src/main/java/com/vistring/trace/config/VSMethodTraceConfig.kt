package com.vistring.trace.config

/**
 * Gradle 中的配置类
 * 这里必须写 open class, 不能用 data class, 否则在 Gradle 插件使用中会出错
 */
open class VSMethodTraceInitConfig(
    // 是否开启方法耗时统计
    var enable: Boolean? = null,
    // 是否开启日志
    var enableLog: Boolean? = null,
    // 是否开启高级匹配
    var enableAdvancedMatch: Boolean? = null,
    // 包含的包名前缀
    var includePackagePrefixSet: Set<String>? = null,
    // 排除的包名前缀
    var excludePackagePrefixSet: Set<String>? = null,
    // 包含的包名正则
    var includePackagePatternSet: Set<String>? = null,
    // 排除的包名正则
    var excludePackagePatternSet: Set<String>? = null,
    // 是否匹配所有 (不建议使用, 因为很多三方的也会被统计到, 但是你却没办法去修正)
    var matchAll: Boolean? = null,
)

data class VSMethodTraceConfig(
    val enableLog: Boolean,
    val enableAdvancedMatch: Boolean,
    val includePackagePrefixSet: Set<String>,
    val excludePackagePrefixSet: Set<String>,
    val includePackagePatternSet: Set<String>,
    val excludePackagePatternSet: Set<String>,
    val matchAll: Boolean,
)