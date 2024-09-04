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
    fun test1() {
        MethodTracker.start(
            1, "1",
        )
        MethodTracker.end(1, "1")
        assertTrue(
            MethodTracker.peek() == null,
        )
    }

    @Test
    fun test2() {
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
    fun test3() {
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

}