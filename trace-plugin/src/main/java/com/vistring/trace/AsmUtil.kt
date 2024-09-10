package com.vistring.trace

import org.objectweb.asm.AnnotationVisitor
import org.objectweb.asm.Attribute
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.TypePath
import org.objectweb.asm.commons.AdviceAdapter
import java.io.InputStream
import java.util.concurrent.atomic.AtomicInteger


object AsmUtil {

    private const val METHOD_TRACKER_CLASS_NAME = "com.vistring.trace.MethodTracker"
    private const val METHOD_TRACE_INFO_CLASS_NAME = "com.vistring.trace.MethodTraceInfo"

    private val CLASS_NAME_IGNORE_LIST = listOf(
        METHOD_TRACKER_CLASS_NAME,
        METHOD_TRACE_INFO_CLASS_NAME,
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

        val classNameOrigin = name
            .removeSuffix(suffix = ".class")

        val className = classNameOrigin
            .replace("/", ".")

        // 如果是 tracker 类, 需要更改 COST_TIME_THRESHOLD 常量的值
        if (METHOD_TRACKER_CLASS_NAME == className) {
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
                        exceptions: Array<out String>?
                    ): MethodVisitor? {
                        val originMethodVisitor =
                            super.visitMethod(access, name, descriptor, signature, exceptions)
                        if ("getCostTimeThread" == name) {
                            return object : AdviceAdapter(
                                ASM_API, originMethodVisitor,
                                access, name, descriptor,
                            ) {
                                // 因为 getCostTimeThread 就一个这个指令
                                // 把 值换成想要的
                                override fun visitLdcInsn(value: Any?) {
                                    super.visitLdcInsn(costTimeThreshold)
                                }
                            }
                        }
                        return originMethodVisitor
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
                println()
                println("${VSMethodTracePlugin.TAG}: className ===================================== $className")
            }
            return kotlin.runCatching {
                val classReader = ClassReader(
                    originClassBytes,
                )
                val classWriter =
                    ClassWriter(ClassWriter.COMPUTE_MAXS or ClassWriter.COMPUTE_FRAMES)

                val classVisitor = object : ClassVisitor(ASM_API, classWriter) {

                    override fun visitMethod(
                        access: Int,
                        name: String?,
                        descriptor: String?,
                        signature: String?,
                        exceptions: Array<out String>?,
                    ): MethodVisitor {

                        /*val isLog =
                            className == "com.vistring.trace.demo.ui.theme.ThemeKt\$VSMethodTraceTheme\$1"*/
                        val isLog = true

                        if (isLog) {
                            println("===================================== name = $name, descriptor = $descriptor")
                        }

                        val isStaticMethod = (access and Opcodes.ACC_STATIC) != 0

                        val isInject = !descriptor.isNullOrBlank() &&
                                "<init>" != name &&
                                "<clinit>" != name &&
                                (access and Opcodes.ACC_NATIVE == 0) &&
                                (access and Opcodes.ACC_ANNOTATION == 0)

                        if (isLog) {
                            println("isInject: $isInject")
                        }

                        val nameForTrace = "$name\$forTrace"

                        val targetMethodVisitor = if (isInject) {
                            super.visitMethod(
                                access,
                                nameForTrace,
                                descriptor,
                                signature,
                                exceptions
                            )
                        } else {
                            super.visitMethod(access, name, descriptor, signature, exceptions)
                        }

                        val proxyMethodVisitor = if (isInject) {
                            super.visitMethod(access, name, descriptor, signature, exceptions)
                        } else {
                            null
                        }

                        if (descriptor.isNullOrBlank()) {
                            return targetMethodVisitor
                        }

                        // 拿到方法的唯一标识
                        val methodFlag = methodFlag.incrementAndGet()
                        return object : MethodVisitor(ASM_API, targetMethodVisitor) {

                            override fun visitAnnotationDefault(): AnnotationVisitor {
                                proxyMethodVisitor?.visitAnnotationDefault()
                                return super.visitAnnotationDefault()
                            }

                            override fun visitAnnotation(
                                descriptor: String?,
                                visible: Boolean
                            ): AnnotationVisitor {
                                proxyMethodVisitor?.visitAnnotation(descriptor, visible)
                                return super.visitAnnotation(descriptor, visible)
                            }

                            override fun visitParameterAnnotation(
                                parameter: Int,
                                descriptor: String?,
                                visible: Boolean
                            ): AnnotationVisitor {
                                proxyMethodVisitor?.visitParameterAnnotation(
                                    parameter,
                                    descriptor,
                                    visible
                                )
                                return super.visitParameterAnnotation(
                                    parameter,
                                    descriptor,
                                    visible
                                )
                            }

                            override fun visitAnnotableParameterCount(
                                parameterCount: Int,
                                visible: Boolean
                            ) {
                                proxyMethodVisitor?.visitAnnotableParameterCount(
                                    parameterCount,
                                    visible
                                )
                                super.visitAnnotableParameterCount(parameterCount, visible)
                            }

                            override fun visitTypeAnnotation(
                                typeRef: Int,
                                typePath: TypePath?,
                                descriptor: String?,
                                visible: Boolean
                            ): AnnotationVisitor {
                                proxyMethodVisitor?.visitTypeAnnotation(
                                    typeRef,
                                    typePath,
                                    descriptor,
                                    visible
                                )
                                return super.visitTypeAnnotation(
                                    typeRef,
                                    typePath,
                                    descriptor,
                                    visible
                                )
                            }

                            override fun visitParameter(name: String?, access: Int) {
                                proxyMethodVisitor?.visitParameter(name, access)
                                super.visitParameter(name, access)
                            }

                            override fun visitAttribute(attribute: Attribute?) {
                                proxyMethodVisitor?.visitAttribute(attribute)
                                super.visitAttribute(attribute)
                            }

                            override fun visitCode() {
                                proxyMethodVisitor?.let { targetMethodVisitor ->
                                    // 比如：(Ljava/lang/String;I)V
                                    // 解析出返回值字节码类型
                                    val (parameterList, returnType) = DescriptorParser.parseMethodDescriptor(
                                        descriptor = descriptor,
                                    ).apply {
                                        if (isLog) {
                                            println("parameterList: $first")
                                            println("returnType: $second")
                                        }
                                    }

                                    val hasReturn = returnType != DescriptorParser.VOID

                                    // 初始的方法参数个数, 如果是 static 方法那就少一个
                                    val initParameterCount =
                                        parameterList.size + if (isStaticMethod) {
                                            0
                                        } else {
                                            1
                                        }

                                    if (isLog) {
                                        println("initParameterCount: $initParameterCount")
                                    }

                                    var maxStackCount = initParameterCount
                                    // 如果有返回值
                                    if (hasReturn) {
                                        maxStackCount++
                                    }

                                    // 肯定有异常, 所以也要 ++
                                    maxStackCount++

                                    if (isLog) {
                                        println("maxStackCount: $maxStackCount")
                                    }

                                    // 倒数两个用来存储返回值和异常

                                    val returnValueStoreIndex = maxStackCount - 2
                                    val exceptionValueStoreIndex = maxStackCount - 1

                                    if (isLog) {
                                        println("returnValueStoreIndex: $returnValueStoreIndex, exceptionValueStoreIndex: $exceptionValueStoreIndex")
                                    }

                                    targetMethodVisitor.visitCode()

                                    val realMethodLabelStart = Label()
                                    val realMethodLabelEnd = Label()

                                    val methodStartTraceStartLabel = Label()
                                    val methodStartTraceEndLabel = Label()

                                    // 没发生异常的时候的 end trace 的开始和结束
                                    val methodEndTraceStartLabelNormal = Label()
                                    val methodEndTraceEndLabelNormal = Label()

                                    // 发生异常的时候的 end trace 的开始和结束
                                    val methodEndTraceStartLabelException = Label()
                                    val methodEndTraceEndLabelException = Label()

                                    val catchLabel = Label()

                                    val lastStartLabel = Label()

                                    // 写入一个异常表
                                    targetMethodVisitor.visitTryCatchBlock(
                                        realMethodLabelStart,
                                        realMethodLabelEnd,
                                        catchLabel,
                                        null,
                                    )

                                    // try 的代码
                                    run {

                                        // 调用 MethodTracker.start 方法
                                        run {
                                            targetMethodVisitor.visitLabel(
                                                methodStartTraceStartLabel
                                            )
                                            targetMethodVisitor.visitLdcInsn(methodFlag)
                                            targetMethodVisitor.visitLdcInsn("$className.$name")
                                            targetMethodVisitor.visitMethodInsn(
                                                Opcodes.INVOKESTATIC,
                                                "com/vistring/trace/MethodTracker",
                                                "start",
                                                // "()V",
                                                "(ILjava/lang/String;)V",
                                                false,
                                            )
                                            targetMethodVisitor.visitLabel(methodStartTraceEndLabel)
                                            targetMethodVisitor.visitInsn(Opcodes.NOP)
                                        }

                                        // 调用用户写的方法, 可能有返回值
                                        run {
                                            targetMethodVisitor.visitLabel(realMethodLabelStart)
                                            if (!isStaticMethod) { // 如果不是静态方法, 就加载 this
                                                targetMethodVisitor.visitVarInsn(Opcodes.ALOAD, 0)
                                            }
                                            val parameterIndexUses = if (isStaticMethod) {
                                                0
                                            } else {
                                                1
                                            }
                                            parameterList.forEachIndexed { index, methodParameter ->
                                                when (methodParameter) {
                                                    DescriptorParser.BYTE -> {
                                                        targetMethodVisitor.visitVarInsn(
                                                            Opcodes.ILOAD,
                                                            index + parameterIndexUses
                                                        )
                                                    }

                                                    DescriptorParser.CHAR -> {
                                                        targetMethodVisitor.visitVarInsn(
                                                            Opcodes.ILOAD,
                                                            index + parameterIndexUses
                                                        )
                                                    }

                                                    DescriptorParser.DOUBLE -> {
                                                        targetMethodVisitor.visitVarInsn(
                                                            Opcodes.DLOAD,
                                                            index + parameterIndexUses
                                                        )
                                                    }

                                                    DescriptorParser.FLOAT -> {
                                                        targetMethodVisitor.visitVarInsn(
                                                            Opcodes.FLOAD,
                                                            index + parameterIndexUses
                                                        )
                                                    }

                                                    DescriptorParser.INT -> {
                                                        targetMethodVisitor.visitVarInsn(
                                                            Opcodes.ILOAD,
                                                            index + parameterIndexUses
                                                        )
                                                    }

                                                    DescriptorParser.LONG -> {
                                                        targetMethodVisitor.visitVarInsn(
                                                            Opcodes.LLOAD,
                                                            index + parameterIndexUses
                                                        )
                                                    }

                                                    DescriptorParser.SHORT -> {
                                                        targetMethodVisitor.visitVarInsn(
                                                            Opcodes.ILOAD,
                                                            index + parameterIndexUses
                                                        )
                                                    }

                                                    DescriptorParser.BOOLEAN -> {
                                                        targetMethodVisitor.visitVarInsn(
                                                            Opcodes.ILOAD,
                                                            index + parameterIndexUses
                                                        )
                                                    }

                                                    else -> {
                                                        targetMethodVisitor.visitVarInsn(
                                                            Opcodes.ALOAD,
                                                            index + parameterIndexUses
                                                        )
                                                    }
                                                }
                                            }

                                            if (isStaticMethod) {
                                                targetMethodVisitor.visitMethodInsn(
                                                    Opcodes.INVOKESTATIC,
                                                    classNameOrigin,
                                                    nameForTrace,
                                                    descriptor,
                                                    false
                                                )
                                            } else {
                                                targetMethodVisitor.visitMethodInsn(
                                                    Opcodes.INVOKEVIRTUAL,
                                                    classNameOrigin,
                                                    nameForTrace,
                                                    descriptor,
                                                    false
                                                )
                                            }

                                            // 如果有返回值, 先存储返回值
                                            if (hasReturn) {
                                                targetMethodVisitor.visitVarInsn(
                                                    Opcodes.ASTORE,
                                                    returnValueStoreIndex,
                                                )
                                            }
                                            /*targetMethodVisitor.visitLabel(realMethodLabelEnd)
                                            targetMethodVisitor.visitInsn(Opcodes.NOP)*/
                                        }

                                        // 调用 MethodTracker.end 方法
                                        run {
                                            targetMethodVisitor.visitLabel(
                                                methodEndTraceStartLabelNormal,
                                            )
                                            targetMethodVisitor.visitLdcInsn(methodFlag)
                                            targetMethodVisitor.visitLdcInsn("$className.$name")
                                            targetMethodVisitor.visitMethodInsn(
                                                Opcodes.INVOKESTATIC,
                                                "com/vistring/trace/MethodTracker",
                                                "end",
                                                // "()V",
                                                "(ILjava/lang/String;)V",
                                                false,
                                            )
                                            targetMethodVisitor.visitLabel(
                                                methodEndTraceEndLabelNormal
                                            )
                                            targetMethodVisitor.visitInsn(Opcodes.NOP)
                                        }

                                        // 表示临时表量表中的和进入方法的时候一样
                                        /*targetMethodVisitor.visitFrame(
                                            Opcodes.F_SAME,
                                            0,
                                            null,
                                            0,
                                            null,
                                        )*/

                                        // 如果没有返回值
                                        if (hasReturn) {
                                            // 加载出返回值
                                            targetMethodVisitor.visitVarInsn(
                                                Opcodes.ALOAD,
                                                returnValueStoreIndex,
                                            )
                                            when (returnType) {
                                                DescriptorParser.BYTE -> {
                                                    targetMethodVisitor.visitInsn(Opcodes.IRETURN)
                                                }

                                                DescriptorParser.CHAR -> {
                                                    targetMethodVisitor.visitInsn(Opcodes.IRETURN)
                                                }

                                                DescriptorParser.DOUBLE -> {
                                                    targetMethodVisitor.visitInsn(Opcodes.DRETURN)
                                                }

                                                DescriptorParser.FLOAT -> {
                                                    targetMethodVisitor.visitInsn(Opcodes.FRETURN)
                                                }

                                                DescriptorParser.INT -> {
                                                    targetMethodVisitor.visitInsn(Opcodes.IRETURN)
                                                }

                                                DescriptorParser.LONG -> {
                                                    targetMethodVisitor.visitInsn(Opcodes.LRETURN)
                                                }

                                                DescriptorParser.SHORT -> {
                                                    targetMethodVisitor.visitInsn(Opcodes.IRETURN)
                                                }

                                                DescriptorParser.BOOLEAN -> {
                                                    targetMethodVisitor.visitInsn(Opcodes.IRETURN)
                                                }

                                                else -> {
                                                    targetMethodVisitor.visitInsn(Opcodes.ARETURN)
                                                }
                                            }
                                        } else {
                                            targetMethodVisitor.visitInsn(Opcodes.RETURN)
                                        }
                                    }

                                    // catch 相关的代码
                                    run {
                                        // 先把异常存起来
                                        targetMethodVisitor.visitLabel(catchLabel)
                                        targetMethodVisitor.visitVarInsn(
                                            Opcodes.ASTORE,
                                            exceptionValueStoreIndex,
                                        )

                                        // 调用 MethodTracker.end 方法
                                        run {
                                            targetMethodVisitor.visitLabel(
                                                methodEndTraceStartLabelException
                                            )
                                            targetMethodVisitor.visitLdcInsn(methodFlag)
                                            targetMethodVisitor.visitLdcInsn("$className.$name")
                                            targetMethodVisitor.visitMethodInsn(
                                                Opcodes.INVOKESTATIC,
                                                "com/vistring/trace/MethodTracker",
                                                "end",
                                                // "()V",
                                                "(ILjava/lang/String;)V",
                                                false,
                                            )
                                            targetMethodVisitor.visitLabel(
                                                methodEndTraceEndLabelException
                                            )
                                            targetMethodVisitor.visitInsn(Opcodes.NOP)
                                        }
                                        // 抛出异常
                                        run {
                                            targetMethodVisitor.visitVarInsn(
                                                Opcodes.ALOAD,
                                                exceptionValueStoreIndex,
                                            )
                                            targetMethodVisitor.visitInsn(Opcodes.ATHROW)
                                        }
                                    }

                                    targetMethodVisitor.visitLabel(lastStartLabel)
                                    targetMethodVisitor.visitInsn(Opcodes.NOP)

                                    targetMethodVisitor.visitMaxs(
                                        initParameterCount
                                            // 因为插桩最少需要两个数据卡槽, methodFlag 和 methodName
                                            .coerceAtLeast(minimumValue = 2),
                                        maxStackCount,
                                    )
                                }
                                super.visitCode()
                            }

                            override fun visitEnd() {
                                proxyMethodVisitor?.visitEnd()
                                super.visitEnd()
                            }

                        }

                        /*return object : AdviceAdapter(
                            ASM_API,
                            targetMethodVisitor,
                            access,
                            name,
                            descriptor,
                        ) {

                            override fun onMethodEnter() {
                                super.onMethodEnter()
                                *//*visitTryCatchBlock(
                                    startLabel, endLabel, targetLabel, null,
                                )
                                isMethodExitByException = false
                                visitLdcInsn(methodFlag)
                                visitLdcInsn("$className.$name")
                                visitMethodInsn(
                                    Opcodes.INVOKESTATIC,
                                    "com/vistring/trace/MethodTracker",
                                    "start",
                                    // "()V",
                                    "(ILjava/lang/String;)V",
                                    false,
                                )*//*
                            }

                            // 屏蔽掉了多个 throw 的情况
                            override fun onMethodExit(opcode: Int) {
                                super.onMethodExit(opcode)
                                *//*if (!isMethodExitByException) {
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
                                if(opcode == Opcodes.ATHROW) {
                                    isMethodExitByException = true
                                }*//*
                            }

                        }*/
                    }
                }
                classReader.accept(classVisitor, ClassReader.EXPAND_FRAMES)
                classWriter.toByteArray()
            }.apply {
                if (enableLog && this.isFailure) {
                    println("$VSMethodTracePlugin transform fail: $className, ${this.exceptionOrNull()?.message}")
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