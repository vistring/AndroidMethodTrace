package com.vistring.trace

import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.FieldVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.commons.AdviceAdapter
import java.io.InputStream
import java.util.concurrent.atomic.AtomicInteger

object AsmUtil {

    private const val METHOD_TRACKER_CLASS_NAME = "com.vistring.trace.MethodTracker"

    private val CLASS_NAME_IGNORE_LIST = listOf(
        METHOD_TRACKER_CLASS_NAME,
        "com.vistring.trace.MethodInfo",
        // 这个会在 MethodTracker 的 start 方法中调用, 会导致死循环,
        "kotlin.jvm.internal.Intrinsics",
    )

    private const val ASM_API = Opcodes.ASM9

    // 方法唯一标记的 flag, 使用的时候需要先自增再获取
    private val methodFlag = AtomicInteger()

    fun transform(
        costTimeThreshold: Long,
        enableLog: Boolean = false,
        pathMatcher: PathMatcher,
        // xxx/xxx/xxx.class
        name: String,
        classFileInputStream: InputStream,
    ): ByteArray {

        // 原来 class 的字节数组
        val originClassBytes = classFileInputStream.readBytes()

        val className = name
            .removeSuffix(suffix = ".class")
            .replace("/", ".")

        // 如果是 tracker 类, 需要更改 COST_TIME_THRESHOLD 常量的值
        if (METHOD_TRACKER_CLASS_NAME == className) {
            return kotlin.runCatching {

                val classReader = ClassReader(
                    originClassBytes,
                )
                val classWriter = ClassWriter(ClassWriter.COMPUTE_MAXS)

                val classVisitor = object : ClassVisitor(ASM_API, classWriter) {

                    override fun visitField(
                        access: Int,
                        name: String?,
                        descriptor: String?,
                        signature: String?,
                        value: Any?,
                    ): FieldVisitor {
                        return if ("COST_TIME_THRESHOLD" == name) {
                            return super.visitField(access, name, descriptor, signature, costTimeThreshold)
                        } else {
                            super.visitField(access, name, descriptor, signature, value)
                        }
                    }

                }
                classReader.accept(classVisitor, ClassReader.EXPAND_FRAMES)
                classWriter.toByteArray()
            }.apply {
                if (enableLog && this.isFailure) {
                    println("$VSMethodTracePlugin transform fail: $className")
                }
            }.getOrNull() ?: originClassBytes
        }

        // 此包下是 trace 耗时统计的模块, 不需要处理
        return if (CLASS_NAME_IGNORE_LIST.any { it == className }) {
            originClassBytes
        } else if (pathMatcher.isMatch(className = className)) {
            if (enableLog) {
                println("$VSMethodTracePlugin transform successful: $className")
            }
            return kotlin.runCatching {
                val classReader = ClassReader(
                    originClassBytes,
                )
                val classWriter = ClassWriter(ClassWriter.COMPUTE_MAXS)

                val classVisitor = object : ClassVisitor(ASM_API, classWriter) {

                    override fun visitMethod(
                        access: Int,
                        name: String?,
                        descriptor: String?,
                        signature: String?,
                        exceptions: Array<out String>?,
                    ): MethodVisitor {
                        val originMethodVisitor =
                            super.visitMethod(access, name, descriptor, signature, exceptions)
                        // 判断是否是构造函数
                        if ("<init>" == name || "<clinit>" == name) {
                            return originMethodVisitor
                        }
                        // 判断是否是 Annotation
                        if (access and Opcodes.ACC_ANNOTATION != 0) {
                            return originMethodVisitor
                        }
                        // 拿到方法的唯一标识
                        val methodFlag = methodFlag.incrementAndGet()
                        return object : AdviceAdapter(
                            ASM_API,
                            originMethodVisitor,
                            access,
                            name,
                            descriptor,
                        ) {

                            override fun onMethodEnter() {
                                super.onMethodEnter()
                                visitLdcInsn(methodFlag)
                                visitLdcInsn("$className.$name")
                                visitMethodInsn(
                                    Opcodes.INVOKESTATIC,
                                    "com/vistring/trace/MethodTracker",
                                    "start",
                                    // "()V",
                                    "(ILjava/lang/String;)V",
                                    false,
                                )
                            }

                            // 同一个方法可能会多次被调用
                            override fun onMethodExit(opcode: Int) {
                                super.onMethodExit(opcode)
                                visitLdcInsn(methodFlag)
                                visitLdcInsn("$className.$name")
                                visitMethodInsn(
                                    Opcodes.INVOKESTATIC,
                                    "com/vistring/trace/MethodTracker",
                                    "end",
                                    // "()V",
                                    "(ILjava/lang/String;)V",
                                    false,
                                )
                            }

                        }
                    }
                }
                classReader.accept(classVisitor, ClassReader.EXPAND_FRAMES)
                classWriter.toByteArray()
            }.apply {
                if (enableLog && this.isFailure) {
                    println("$VSMethodTracePlugin transform fail: $className")
                }
            }.getOrNull() ?: originClassBytes
        } else {
            originClassBytes
        }
    }

    fun resetMethodFlag() {
        methodFlag.set(0)
    }

}