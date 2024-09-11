package com.vistring.trace

import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class TestPathMatcher {

    @Test
    fun testPackagePrefixMatch() {
        val pathMatcher = PathMatcher(
            includePackagePrefixSet = setOf(
                "com.vistring.trace",
            ),
            excludePackagePrefixSet = setOf(
                "com.vistring.trace.demo",
            ),
        )
        assert(
            pathMatcher.isMatch(
                className = "com.vistring.trace.Test"
            ) && pathMatcher.isMatch(
                className = "com.vistring.trace.Test.ttt"
            )
        )
        assert(
            !pathMatcher.isMatch(
                className = "com.vistring.trace.demo"
            ) && !pathMatcher.isMatch(
                className = "com.vistring.trace.demo.ttt"
            )
        )
    }

    @Test
    fun testPackagePatternMatch() {
        val pathMatcher = PathMatcher(
            includePackagePatternSet = setOf(
                "com.vistring.trace.*",
            ),
            excludePackagePatternSet = setOf(
                "com.vistring.trace.demo.*",
            ),
        )
        assert(
            pathMatcher.isMatch(
                className = "com.vistring.trace.Test"
            ) && pathMatcher.isMatch(
                className = "com.vistring.trace.Test.ttt"
            )
        )
        assert(
            !pathMatcher.isMatch(
                className = "com.vistring.trace.demo"
            ) && !pathMatcher.isMatch(
                className = "com.vistring.trace.demo.ttt"
            )
        )
    }

    @Test
    fun testMatchAllParameterMatch() {
        val pathMatcher = PathMatcher(
            excludePackagePrefixSet = setOf(
                "com.vistring.trace.demo",
            ),
            excludePackagePatternSet = setOf(
                "com.vistring.trace.demo.*",
            ),
            matchAll = true,
        )
        assert(
            pathMatcher.isMatch(
                className = "com.vistring.trace.Test"
            ) && pathMatcher.isMatch(
                className = "com.vistring.trace.Test.ttt"
            )
        )
        assert(
            !pathMatcher.isMatch(
                className = "com.vistring.trace.demo"
            ) && !pathMatcher.isMatch(
                className = "com.vistring.trace.demo.ttt"
            )
        )
    }

    @Test
    fun test1() {
        assert(
            (listOf(
                "Ljava/lang/String;", DescriptorParser.INT,
            ) to DescriptorParser.VOID) == DescriptorParser.parseMethodDescriptor(
                descriptor = "(Ljava/lang/String;I)V",
            )
        )
        assert(
            (listOf(
                "Ljava/lang/String;",
                "[I",
                "[Ljava/lang/String;",
            ) to DescriptorParser.VOID) == DescriptorParser.parseMethodDescriptor(
                descriptor = "(Ljava/lang/String;[I[Ljava/lang/String;)V",
            )
        )
        assert(
            (listOf(
                DescriptorParser.INT,
                DescriptorParser.INT,
                DescriptorParser.LONG,
                DescriptorParser.DOUBLE,
            ) to "[[Lkotlin/Pair;") == DescriptorParser.parseMethodDescriptor(
                descriptor = "(IIJD)[[Lkotlin/Pair;",
            )
        )
    }

}