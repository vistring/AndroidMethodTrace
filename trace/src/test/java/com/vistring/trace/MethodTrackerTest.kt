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
            name = "1",
        )
        MethodTracker.end(1, "1")
        assertTrue(
            MethodTracker.peek() == null,
        )
    }

    @Test
    fun testCallEndFunctionMultipleTimes() {
        MethodTracker.start(
            methodFlag = 1,
            name = "1",
        )
        MethodTracker.start(
            methodFlag = 2,
            name = "2",
        )
        MethodTracker.end(
            methodFlag = 2,
            name = "2",
        )
        MethodTracker.end(
            methodFlag = 2,
            name = "2",
        )
        assertTrue(
            MethodTracker.peek() == null,
        )
    }

    @Test
    fun testSubMethodThrowException1() {
        MethodTracker.start(
            1, "1",
        )
        MethodTracker.start(
            2, "2",
        )
        // 模拟 2 崩溃, 2 没有调用 end 方法
        MethodTracker.end(1, "1")
        assertTrue(
            MethodTracker.peek() == null,
        )
    }

    @Test
    fun testSubMethodThrowException2() {
        MethodTracker.start(
            1,
            "1",
        )
        MethodTracker.start(
            2,
            "2",
        )
        MethodTracker.start(
            3,
            "3",
        )
        MethodTracker.end(
            3,
            "3",
        )
        MethodTracker.start(
            4,
            "4",
        )
        MethodTracker.end(
            2,
            "2",
        )
        assertTrue(
            MethodTracker.peek()?.methodFlag == 1,
        )
    }

    @Test
    fun testTraceMethod() {
        /*val mockMethodTracker = mock<MethodTracker> {
            on { costTimeThread } doReturn 100
        }*/
        /*val mockMethodTracker = Mockito.mockStatic(MethodTracker::class.java)
        mockMethodTracker.`when`<Long> { MethodTracker.costTimeThread }.thenReturn(100)
        MethodTracker.start(1, "costMethod")
        Thread.sleep(150)
        MethodTracker.end(1, "costMethod")
        mockMethodTracker.verify { MethodTracker.logTraceInfo(1, any(), any()) }
        mockMethodTracker.close()*/
    }

}