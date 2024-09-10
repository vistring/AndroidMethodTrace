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
        MethodTracker.start(
            methodFlag = 1,
            name = "1",
        )
        MethodTracker.end(
            methodFlag = 1,
            name = "1",
        )
        MethodTracker.end(
            methodFlag = 1,
            name = "1",
        )
        assertTrue(
            MethodTracker.peek() == null,
        )
    }

    @Test
    fun testCrashWhenEndCalledMoreThenStart() {
        MethodTracker.start(
            methodFlag = 1,
            name = "1",
        )
        MethodTracker.start(
            methodFlag = 1,
            name = "1",
        )
        MethodTracker.end(
            methodFlag = 1,
            name = "1",
        )
        MethodTracker.end(
            methodFlag = 1,
            name = "1",
        )
        assertTrue(
            kotlin.runCatching {
                MethodTracker.end(
                    methodFlag = 1,
                    name = "1",
                )
            }.isFailure
        )
    }

}