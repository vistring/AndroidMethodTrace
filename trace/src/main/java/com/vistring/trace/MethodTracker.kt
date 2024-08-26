package com.vistring.trace

import android.os.Looper
import android.util.Log
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
@Keep
object MethodTracker {

    const val TAG = "VSMethodTracker"

    private val methodStack = ThreadLocal<Stack<MethodInfo>>()

    @JvmStatic
    fun start(name: String) {
        methodStack.getOrSet { Stack<MethodInfo>() }.add(
            MethodInfo(
                methodName = name,
                isMainThread = Looper.getMainLooper() == Looper.myLooper(),
                startTime = System.currentTimeMillis(),
            )
        )
    }

    @JvmStatic
    fun end(name: String) {
        methodStack.get()?.let { stack ->
            val currentMethodInfo = stack.pop()
            val previousMethodInfo = if (stack.isEmpty()) null else stack.peek()
            val currentTime = System.currentTimeMillis()
            // 方法总耗时
            val methodTotalCost = currentTime - currentMethodInfo.startTime
            // 方法耗时, 减去了统计出来的子方法的耗时. 如果子方法没被统计到, 那么耗时也会表现在这里
            val methodCost = methodTotalCost - currentMethodInfo.subMethodCostTotalTime
            previousMethodInfo?.run {
                this.subMethodCostTotalTime += methodCost + currentMethodInfo.subMethodCostTotalTime
            }
            if (currentMethodInfo.isMainThread && methodCost > 100) {
                Log.d(
                    TAG,
                    "methodTotalCost = $methodTotalCost, methodCost = $methodCost",
                )
                var isPrint = false
                Thread
                    .currentThread()
                    .stackTrace
                    // .take(8)
                    .forEach { stackTraceElement ->
                        if (isPrint) {
                            Log.d(
                                TAG,
                                "\t\t$stackTraceElement",
                            )
                        }
                        if (MethodTracker::class.qualifiedName == stackTraceElement.className && "end" == stackTraceElement.methodName) {
                            isPrint = true
                        }
                    }
            }
        }

    }

}

