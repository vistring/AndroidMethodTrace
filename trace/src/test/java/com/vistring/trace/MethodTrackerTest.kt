package com.vistring.trace

import android.os.Looper
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test
import org.mockito.Mockito
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class MethodTrackerTest {

    companion object {

        @JvmStatic
        private lateinit var mainLooper: Looper

        @BeforeClass
        @JvmStatic
        fun setUp() {
            mainLooper = mock<Looper> {
            }
            Mockito.mockStatic(
                Looper::class.java
            ).apply {
                `when`<Looper> {
                    Looper.getMainLooper()
                } doReturn mainLooper
                `when`<Looper> {
                    Looper.myLooper()
                } doReturn mainLooper
            }
        }

    }

    @Test
    fun testNormalMethodTrace() {
        MethodTracker.start(
            methodFlag = 1,
        )
        MethodTracker.start(
            methodFlag = 1,
        )
        MethodTracker.end(
            methodFlag = 1,
        )
        MethodTracker.end(
            methodFlag = 1,
        )
        assertTrue(
            MethodTracker.peek() == null,
        )
    }

    @Test
    fun testCrashWhenEndCalledMoreThenStart() {
        MethodTracker.start(
            methodFlag = 1,
        )
        MethodTracker.start(
            methodFlag = 1,
        )
        MethodTracker.end(
            methodFlag = 1,
        )
        MethodTracker.end(
            methodFlag = 1,
        )
        assertTrue(
            kotlin.runCatching {
                MethodTracker.end(
                    methodFlag = 1,
                )
            }.isFailure
        )
    }

    /**
     * 测试下插桩本身耗时多少
     */
    @Test
    fun testAsmTimeCost() {
        val uselessFlag = Int.MAX_VALUE
        val startTime = System.currentTimeMillis()
        val loopCount = 100000
        repeat(times = loopCount) {
            MethodTracker.start(
                methodFlag = uselessFlag,
            )
            MethodTracker.end(
                methodFlag = uselessFlag,
            )
        }
        val endTime = System.currentTimeMillis()
        val cost = endTime - startTime
        println("cost = $cost, average = ${cost / loopCount.toFloat()}")
    }

}