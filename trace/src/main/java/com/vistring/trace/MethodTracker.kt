package com.vistring.trace

import android.app.Application
import android.os.Looper
import androidx.annotation.Keep
import androidx.annotation.VisibleForTesting
import java.util.LinkedList
import java.util.Stack
import kotlin.concurrent.getOrSet

@Keep
data class MethodTraceInfo(
    val methodFlag: Int,
    val isMainThread: Boolean,
    val startTime: Long = 0,
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

    const val TAG = "VSMethodTracker"

    /**
     * 其实这个地方需要用 [Stack], 但是用 [LinkedList] 更好
     */
    private val methodLinkedList = ThreadLocal<LinkedList<MethodTraceInfo>>()

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
        val stringBuilder = StringBuilder()
        Thread
            .currentThread()
            .stackTrace
            .run {
                this.takeLast(n = this.size - 4)
            }.forEachIndexed { index, stackTraceElement ->
                if (index > 0) {
                    stringBuilder.append(";")
                }
                stringBuilder.append(stackTraceElement.toString())
            }
        MethodInfoUploader.addUploadTask(
            task = MethodInfoUploader.Task(
                methodFlag = methodFlag,
                methodCost = methodCost,
                methodTotalCost = methodTotalCost,
                stackTraceStr = stringBuilder.toString(),
            ),
        )
    }

    // 不是给用户调用的, 是给插件生成的代码调用的
    @JvmStatic // 主要是为了字节码那边能够比较方便的调用到
    fun start(
        methodFlag: Int,
    ) {
        methodLinkedList.getOrSet { LinkedList<MethodTraceInfo>() }.apply {
            this.add(
                MethodTraceInfo(
                    methodFlag = methodFlag,
                    isMainThread = Looper.getMainLooper() == Looper.myLooper(),
                    startTime = System.currentTimeMillis(),
                )
            )
        }
    }

    /**
     * 不是给用户调用的, 是给插件生成的代码调用的
     * 对一个方法插桩的时候, [start] 方法只会调用一次, 这个不用怀疑
     * 但是 [end] 方法可能会调用 0 到 1 次!!!, 因为字节码插桩的方案是：
     * 匹配到  return 和 throw 语句的时候, 就会插入 [end] 方法的调用, 如果异常没有捕获, 那么就不会调用 [end] 方法
     *
     * =========================================================
     *
     * 从 9.10 开始, 字节码插桩的方案进行了大升级, 保证了 start 和 end 方法的调用是成双成对出现的.
     * [com.vistring.trace.AsmUtil] 对要插桩的方法进行了重命名, 然后原来的方法中进行统一的 try finally 处理, 保证了 end 方法的调用
     * ```Kotlin
     * private final void sleep10$forTrace() {
     *    Thread.sleep(10L)
     * }
     *
     * private final void sleep10() {
     *    MethodTracker.start(24, "com.vistring.trace.demo.test.TestDelay.sleep10")
     *    try {
     *        this.sleep10$forTrace()
     *    } finally {
     *        MethodTracker.end(24, "com.vistring.trace.demo.test.TestDelay.sleep10")
     *    }
     * }
     * ```
     * 虽然可能导致一些方法的耗时统计不准确, 但是不会导致崩溃, 也不会影响其他方法的耗时统计
     */
    @JvmStatic // 主要是为了字节码那边能够比较方便的调用到
    fun end(
        methodFlag: Int,
    ) {
        methodLinkedList.get()?.let { linkedList ->
            val currentMethodInfo = linkedList.removeLast()
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

    fun init(
        application: Application,
        appName: String,
    ) {
        if (appName.isBlank()) {
            throw IllegalArgumentException("appName cannot be blank")
        }
        MethodInfoUploader.init(application = application)
    }

}

