package com.vistring.trace

import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class TestPathMatcher {

    @Test
    fun testNormalPathMatch() {
        val pathMatcher = PathMatcher(
            includeSet = setOf(
                "com.vistring.trace.Test",
            ),
            excludeSet = setOf(
                "com.vistring.trace.Test.ttt",
            ),
        )
        assert(
            pathMatcher.isMatch(
                target = "com.vistring.trace.Test"
            ) && !pathMatcher.isMatch(
                target = "com.vistring.trace.Test.ttt"
            )
        )
    }

    @Test
    fun testPathPrefixMatch() {
        val pathMatcher = PathMatcher(
            includePrefixSet = setOf(
                "com.vistring.trace",
            ),
            excludePrefixSet = setOf(
                "com.vistring.trace.demo",
            ),
        )
        assert(
            pathMatcher.isMatch(
                target = "com.vistring.trace.Test"
            ) && pathMatcher.isMatch(
                target = "com.vistring.trace.Test.ttt"
            )
        )
        assert(
            !pathMatcher.isMatch(
                target = "com.vistring.trace.demo"
            ) && !pathMatcher.isMatch(
                target = "com.vistring.trace.demo.ttt"
            )
        )
    }

    @Test
    fun testPathPatternMatch() {
        val pathMatcher = PathMatcher(
            includePatternSet = setOf(
                "com.vistring.trace.*",
            ),
            excludePatternSet = setOf(
                "com.vistring.trace.demo.*",
            ),
        )
        assert(
            pathMatcher.isMatch(
                target = "com.vistring.trace.Test"
            ) && pathMatcher.isMatch(
                target = "com.vistring.trace.Test.ttt"
            )
        )
        assert(
            !pathMatcher.isMatch(
                target = "com.vistring.trace.demo"
            ) && !pathMatcher.isMatch(
                target = "com.vistring.trace.demo.ttt"
            )
        )
    }

    @Test
    fun testMatchAllParameterMatch() {
        val pathMatcher = PathMatcher(
            excludePrefixSet = setOf(
                "com.vistring.trace.demo",
            ),
            excludePatternSet = setOf(
                "com.vistring.trace.demo.*",
            ),
            matchAll = true,
        )
        assert(
            pathMatcher.isMatch(
                target = "com.vistring.trace.Test"
            ) && pathMatcher.isMatch(
                target = "com.vistring.trace.Test.ttt"
            )
        )
        assert(
            !pathMatcher.isMatch(
                target = "com.vistring.trace.demo"
            ) && !pathMatcher.isMatch(
                target = "com.vistring.trace.demo.ttt"
            )
        )
    }

    @Test
    fun testDescriptorParse() {
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