package com.vistring.trace

import android.os.Looper
import androidx.annotation.Keep
import java.util.Stack
import kotlin.concurrent.getOrSet

@Keep
private data class MethodInfo(
    val methodName: String,
    val isMainThread: Boolean,
    var startTime: Long = 0,
    var subMethodCostTotalTime: Long = 0,
)

/**
 * 方法耗时追踪
 */
object MethodTracker {

    private val startTimeDeque = ThreadLocal<Stack<MethodInfo>>()

    @JvmStatic
    fun start(name: String) {
        startTimeDeque.getOrSet { Stack<MethodInfo>() }.add(
            MethodInfo(
                methodName = name,
                isMainThread = Looper.getMainLooper() == Looper.myLooper(),
                startTime = System.currentTimeMillis(),
            )
        )
    }

    @JvmStatic
    fun end(name: String) {
        startTimeDeque.get()?.let { stack ->
            val currentMethodInfo = stack.pop()
            val previousMethodInfo = if (stack.isEmpty()) null else stack.peek()
            val currentTime = System.currentTimeMillis()
            val totalCost = currentTime - currentMethodInfo.startTime
            val cost = totalCost - currentMethodInfo.subMethodCostTotalTime
            previousMethodInfo?.run {
                this.subMethodCostTotalTime += cost + currentMethodInfo.subMethodCostTotalTime
            }
            if (currentMethodInfo.isMainThread && cost > 100) {
                println("hhh=========== name = $name, totalCost = $totalCost, cost = $cost")
                var isPrint = false
                Thread
                    .currentThread()
                    .stackTrace
                    // .take(8)
                    .forEach { stackTraceElement ->
                        if (isPrint) {
                            println("hhh=========== \t\t$stackTraceElement")
                        }
                        if (MethodTracker::class.qualifiedName == stackTraceElement.className && "end" == stackTraceElement.methodName) {
                            isPrint = true
                        }
                    }
            }
        }

    }

}

