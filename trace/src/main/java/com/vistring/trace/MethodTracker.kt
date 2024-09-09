package com.vistring.trace

import android.os.Looper
import android.util.Log
import androidx.annotation.Keep
import androidx.annotation.VisibleForTesting
import java.util.LinkedList
import java.util.Stack
import kotlin.concurrent.getOrSet

@Keep
data class MethodTraceInfo(
    val methodHierarchy: Int,
    val methodFlag: Int,
    val methodName: String,
    val isMainThread: Boolean,
    var startTime: Long = 0,
    // sub 方法的总耗时, 包括了被追踪报告的和未被追踪报告的
    var subMethodTotalTime: Long = 0,
    // sub 方法的未被追踪报告的总耗时
    var subMethodTotalUnTraceTime: Long = 0,
)

/**
 * 方法耗时追踪
 */
@Keep
object MethodTracker {

    private const val TAG = "VSMethodTracker"

    /**
     * 其实这个地方需要用 [Stack], 但是用 [LinkedList] 更好
     */
    private val methodLinkedList = ThreadLocal<LinkedList<MethodTraceInfo>>()

    /**
     * 方法层级计数器
     */
    private val methodHierarchyCounter = ThreadLocal<Int>()

    /**
     * 耗时的时间判断, Gradle 插件配置的地方可以配置
     * 不能写成常量, 因为编译优化会导致插桩不生效
     * [Long.MAX_VALUE] 表示不生效
     */
    @VisibleForTesting
    val costTimeThread: Long
        get() = Long.MAX_VALUE

    @VisibleForTesting
    fun logTraceInfo(
        methodFlag: Int,
        methodCost: Long,
        methodTotalCost: Long,
    ) {
        Log.d(
            TAG,
            "methodFlag = $methodFlag, methodCost = $methodCost, methodTotalCost = $methodTotalCost",
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

    // 不是给用户调用的, 是给插件生成的代码调用的
    @JvmStatic // 主要是为了字节码那边能够比较方便的调用到
    fun start(
        methodFlag: Int,
        name: String,
    ) {
        methodLinkedList.getOrSet { LinkedList<MethodTraceInfo>() }.apply {
            val methodHierarchy = methodHierarchyCounter.getOrSet { 0 } + 1
            methodHierarchyCounter.set(methodHierarchy)
            this.add(
                MethodTraceInfo(
                    methodHierarchy = methodHierarchy,
                    methodFlag = methodFlag,
                    methodName = name,
                    isMainThread = Looper.getMainLooper() == Looper.myLooper(),
                    startTime = System.currentTimeMillis(),
                )
            )
        }
    }

    /**
     * 不是给用户调用的, 是给插件生成的代码调用的
     * 对一个方法插桩的时候, [start] 方法只会调用一次, 这个不用怀疑
     * 但是 [end] 方法可能会调用 0 到 1 次!!!,
     * [com.vistring.trace.AsmUtil] 中对多次 throw 的情况进行了处理, 只会让第一个 throw 调用 [end] 方法
     * 虽然可能导致一些方法的耗时统计不准确, 但是不会导致崩溃, 也不会影响其他方法的耗时统计
     */
    @JvmStatic // 主要是为了字节码那边能够比较方便的调用到
    fun end(
        methodFlag: Int,
        name: String,
    ) {
        methodLinkedList.get()?.let { linkedList ->
            // 一直找, 直到找到对应的方法, 因为 end 方法可能不会被调用, 但是 start 一定会被调用
            // 所以当 end 方法调用的时候, 一定要去掉栈上面的其他方法
            var currentMethodInfo: MethodTraceInfo?
            do {
                currentMethodInfo = linkedList.removeLastOrNull()
            } while (currentMethodInfo != null && currentMethodInfo.methodFlag != methodFlag)
            if (currentMethodInfo == null) {
                return
            }
            val currentTime = System.currentTimeMillis()
            // 方法总耗时
            val methodTotalCost = currentTime - currentMethodInfo.startTime
            // 方法耗时, 减去了统计出来的子方法的耗时. 如果子方法没被统计到, 那么耗时也会表现在这里
            val methodCost = methodTotalCost - currentMethodInfo.subMethodTotalTime
            // 上层的调用方法
            val previousMethodInfo = if (linkedList.isEmpty()) null else linkedList.last
            previousMethodInfo?.run {
                this.subMethodTotalTime += methodTotalCost // += 是因为上层方法可能调用多个方法
            }
            if (currentMethodInfo.isMainThread &&
                (methodCost + currentMethodInfo.subMethodTotalUnTraceTime) > costTimeThread
            ) {
                logTraceInfo(
                    methodFlag = methodFlag,
                    methodCost = methodCost,
                    methodTotalCost = methodTotalCost,
                )
            } else {
                previousMethodInfo?.run {
                    this.subMethodTotalUnTraceTime += methodCost + currentMethodInfo.subMethodTotalUnTraceTime
                }
            }
        }
    }

    /**
     * 查看当前栈顶的方法信息
     */
    fun peek(): MethodTraceInfo? {
        return methodLinkedList.get()?.lastOrNull()
    }

}

