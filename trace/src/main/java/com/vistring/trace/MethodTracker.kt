package com.vistring.trace

import android.os.Looper
import android.util.Log
import androidx.annotation.Keep
import java.util.Stack
import kotlin.concurrent.getOrSet

@Keep
private data class MethodInfo(
    val methodFlag: Int,
    val methodName: String,
    val isMainThread: Boolean,
    var startTime: Long = 0,
    // 修改发生在下一层的方法上
    var subMethodTotalTime: Long = 0,
    // 修改发生在下一层的方法上
    var subMethodTotalUnReportTime: Long = 0,
)

/**
 * 方法耗时追踪
 */
@Keep
object MethodTracker {

    private const val TAG = "VSMethodTracker"

    private val methodStack = ThreadLocal<Stack<MethodInfo>>()

    /**
     * 耗时的时间判断, Gradle 插件配置的地方可以配置
     * [Long.MAX_VALUE] 表示不生效
     */
    private const val COST_TIME_THRESHOLD = Long.MAX_VALUE

    // 不是给用户调用的, 是给插件生成的代码调用的
    @JvmStatic
    fun start(
        methodFlag: Int,
        name: String,
    ) {
        methodStack.getOrSet { Stack<MethodInfo>() }.add(
            MethodInfo(
                methodFlag = methodFlag,
                methodName = name,
                isMainThread = Looper.getMainLooper() == Looper.myLooper(),
                startTime = System.currentTimeMillis(),
            )
        )
    }

    /**
     * 不是给用户调用的, 是给插件生成的代码调用的
     * 对一个方法插桩的时候, [start] 方法只会调用一次, 这个不用怀疑
     * 但是 [end] 方法可能会调用多次!!! 但是只会采用第一次的.
     * 虽然可能导致一些方法的耗时统计不准确, 但是不会导致崩溃, 也不会影响其他方法的耗时统计
     */
    @JvmStatic
    fun end(
        methodFlag: Int,
        name: String,
    ) {
        methodStack.get()?.let { stack ->
            val currentMethodInfo = stack.peek()
            if (currentMethodInfo.methodFlag == methodFlag) {
                stack.pop()
                val previousMethodInfo = if (stack.isEmpty()) null else stack.peek()
                val currentTime = System.currentTimeMillis()
                // 方法总耗时
                val methodTotalCost = currentTime - currentMethodInfo.startTime
                // 方法耗时, 减去了统计出来的子方法的耗时. 如果子方法没被统计到, 那么耗时也会表现在这里
                val methodCost = methodTotalCost - currentMethodInfo.subMethodTotalTime
                previousMethodInfo?.run {
                    this.subMethodTotalTime += methodCost + currentMethodInfo.subMethodTotalTime
                }
                if (currentMethodInfo.isMainThread &&
                    (methodCost + currentMethodInfo.subMethodTotalUnReportTime) > COST_TIME_THRESHOLD
                ) {
                    Log.d(
                        TAG,
                        "methodFlag = $methodFlag, methodTotalCost = $methodTotalCost, methodCost = $methodCost",
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
                } else {
                    previousMethodInfo?.run {
                        this.subMethodTotalUnReportTime += methodCost + currentMethodInfo.subMethodTotalUnReportTime
                    }
                }
            }
        }
    }

}

