package com.vistring.trace

/**
 * 路径匹配器
 */
class PathMatcher(
    val enableLog: Boolean = false,
    val includePackagePrefixSet: Set<String> = emptySet(),
    val excludePackagePrefixSet: Set<String> = emptySet(),
    val includePackagePatternSet: Set<String> = emptySet(),
    val excludePackagePatternSet: Set<String> = emptySet(),
    val matchAll: Boolean = false,
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
        val isExcluded = excludePackagePatternSetAdapter.any { pattern ->
            pattern.matcher(className).matches()
        } || excludePackagePrefixSet.any {
            className.startsWith(prefix = it)
        }
        val isIncluded = includePackagePatternSetAdapter.any { pattern ->
            pattern.matcher(className).matches()
        } || includePackagePrefixSet.any {
            className.startsWith(prefix = it)
        }
        return if (matchAll) {
            !isExcluded
        } else {
            !isExcluded && isIncluded
        }
    }

    override fun toString(): String {
        return "PathMatcher(enableLog=$enableLog, includePackagePrefixSet=$includePackagePrefixSet, excludePackagePrefixSet=$excludePackagePrefixSet, includePackagePatternSet=$includePackagePatternSet, excludePackagePatternSet=$excludePackagePatternSet, includePackagePatternSetAdapter=$includePackagePatternSetAdapter, excludePackagePatternSetAdapter=$excludePackagePatternSetAdapter)"
    }


}