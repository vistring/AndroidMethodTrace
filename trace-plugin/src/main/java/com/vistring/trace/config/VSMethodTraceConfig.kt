package com.vistring.trace.config

/**
 * Gradle 中的配置类
 * 这里必须写 open class, 不能用 data class, 否则在 Gradle 插件使用中会出错
 * 下面几个参数都是可以混用的
 * [includePackagePrefixSet]
 * [excludePackagePrefixSet]
 * [includePackagePatternSet]
 * [excludePackagePatternSet]
 * [matchAll]
 */
open class VSMethodTraceInitConfig(
    // 方法耗时阈值
    var costTimeThreshold: Long? = null,
    // 是否开启方法耗时统计
    var enable: Boolean? = null,
    // 是否开启日志
    var enableLog: Boolean? = null,
    // 包含的包名前缀
    var includePackagePrefixSet: Set<String>? = null,
    // 排除的包名前缀
    var excludePackagePrefixSet: Set<String>? = null,
    // 包含的包名正则
    var includePackagePatternSet: Set<String>? = null,
    // 排除的包名正则
    var excludePackagePatternSet: Set<String>? = null,
    // 包含被注解的方法
    var includeMethodAnnoSet: Set<String>? = null,
    // 排除被注解的方法
    var excludeMethodAnnoSet: Set<String>? = null,
    // 包含被注解前缀的方法
    var includeMethodAnnoPrefixSet: Set<String>? = null,
    // 排除被注解前缀的方法
    var excludeMethodAnnoPrefixSet: Set<String>? = null,
    // 包含被注解的方法
    var includeMethodAnnoPatternSet: Set<String>? = null,
    // 排除被注解的方法
    var excludeMethodAnnoPatternSet: Set<String>? = null,
    // 是否匹配所有 (不建议使用, 因为很多三方的也会被统计到, 但是你却没办法去修正)
    var matchAll: Boolean? = null,
)

data class VSMethodTraceConfig(
    val enableLog: Boolean,
    val costTimeThreshold: Long,
    val includePackagePrefixSet: Set<String>,
    val excludePackagePrefixSet: Set<String>,
    val includePackagePatternSet: Set<String>,
    val excludePackagePatternSet: Set<String>,
    val includeMethodAnnoSet: Set<String>,
    val excludeMethodAnnoSet: Set<String>,
    val includeMethodAnnoPrefixSet: Set<String>,
    val excludeMethodAnnoPrefixSet: Set<String>,
    val includeMethodAnnoPatternSet: Set<String>,
    val excludeMethodAnnoPatternSet: Set<String>,
    val matchAll: Boolean,
)