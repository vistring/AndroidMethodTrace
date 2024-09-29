package com.vistring.trace

/**
 * 路径匹配器
 */
class PathMatcher(
    private val enableLog: Boolean,
    val includeSet: Set<String> = emptySet(),
    val excludeSet: Set<String> = emptySet(),
    val includePrefixSet: Set<String> = emptySet(),
    val excludePrefixSet: Set<String> = emptySet(),
    val includePatternSet: Set<String> = emptySet(),
    val excludePatternSet: Set<String> = emptySet(),
    val matchAll: Boolean = false,
) {

    private val includePackagePatternSetAdapter = includePatternSet.map {
        it.toPattern()
    }

    private val excludePackagePatternSetAdapter = excludePatternSet.map {
        it.toPattern()
    }

    /**
     * @param target 待匹配的字符串
     */
    fun isMatch(target: String?): Boolean {
        if (target.isNullOrEmpty()) {
            return false
        }
        val isExcluded = excludeSet.any {
            target == it
        } || excludePrefixSet.any {
            target.startsWith(prefix = it)
        } || excludePackagePatternSetAdapter.any { pattern ->
            pattern.matcher(target).matches()
        }
        val isIncluded = includeSet.any {
            target == it
        } || includePrefixSet.any {
            target.startsWith(prefix = it)
        } || includePackagePatternSetAdapter.any { pattern ->
            pattern.matcher(target).matches()
        }
        return if (matchAll) {
            !isExcluded
        } else {
            !isExcluded && isIncluded
        }
    }

    override fun toString(): String {
        return "PathMatcher(enableLog=$enableLog, includeSet=$includeSet, excludeSet=$excludeSet, includePrefixSet=$includePrefixSet, excludePrefixSet=$excludePrefixSet, includePatternSet=$includePatternSet, excludePatternSet=$excludePatternSet, matchAll=$matchAll, includePackagePatternSetAdapter=$includePackagePatternSetAdapter, excludePackagePatternSetAdapter=$excludePackagePatternSetAdapter)"
    }

}