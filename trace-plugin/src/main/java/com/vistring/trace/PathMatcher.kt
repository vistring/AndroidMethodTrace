package com.vistring.trace

/**
 * 路径匹配器
 */
class PathMatcher(
    val enableLog: Boolean = false,
    val enableAdvancedMatch: Boolean,
    val includePackagePrefixSet: Set<String>,
    val excludePackagePrefixSet: Set<String>,
    val includePackagePatternSet: Set<String>,
    val excludePackagePatternSet: Set<String>,
) {

    private val includePackagePatternSetAdapter = includePackagePatternSet.map {
        it.toPattern()
    }

    private val excludePackagePatternSetAdapter = excludePackagePatternSet.map {
        it.toPattern()
    }

    /**
     * @param className xxx.xxx.xxx
     */
    fun isMatch(className: String): Boolean {
        return if (enableAdvancedMatch) {
            !excludePackagePatternSetAdapter.any { pattern ->
                pattern.matcher(className).matches()
            } && includePackagePatternSetAdapter.any { pattern ->
                pattern.matcher(className).matches()
            }
        } else {
            !excludePackagePrefixSet.any {
                className.startsWith(prefix = it)
            } && includePackagePrefixSet.any {
                className.startsWith(prefix = it)
            }
        }
    }

    override fun toString(): String {
        return "PathMatcher(enableLog=$enableLog, enableAdvancedMatch=$enableAdvancedMatch, includePackagePrefixSet=$includePackagePrefixSet, excludePackagePrefixSet=$excludePackagePrefixSet, includePackagePatternSet=$includePackagePatternSet, excludePackagePatternSet=$excludePackagePatternSet, includePackagePatternSetAdapter=$includePackagePatternSetAdapter, excludePackagePatternSetAdapter=$excludePackagePatternSetAdapter)"
    }


}