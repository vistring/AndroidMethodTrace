package com.vistring.trace

import com.vistring.trace.RenameForTraceClassVisitor.Companion.RENAME_FOR_SUFFIX
import org.objectweb.asm.AnnotationVisitor
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.commons.AdviceAdapter
import java.io.File
import java.io.InputStream
import java.io.RandomAccessFile
import java.util.Collections
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.max

private const val EnableDetailLog = false

/**
 * 用来创建完整的代理对象的方法. 下面的例子就是此方法来生成的
 * ```Kotlin
 * private final void sleep10() {
 *    MethodTracker.start(24, "com.vistring.trace.demo.test.TestDelay.sleep10")
 *    try {
 *        this.sleep10$forTrace()
 *    } finally {
 *        MethodTracker.end(24, "com.vistring.trace.demo.test.TestDelay.sleep10")
 *    }
 * }
 */
private fun MethodVisitor.createProxyMethod(
    enableLog: Boolean,
    slashClassName: String,
    dotClassName: String,
    methodFirstLineNumber: Int?,
    methodAccess: Int,
    // 比如：(Ljava/lang/String;I)V
    methodDescriptor: String,
    methodFlag: Int,
    methodName: String?,
    nameForTrace: String
) {

    val enableDetailLog = enableLog && false

    val targetMethodVisitor = this
    val isStaticMethod = (methodAccess and Opcodes.ACC_STATIC) != 0
    // 比如：(Ljava/lang/String;I)V
    // 解析出返回值字节码类型
    val (parameterList, returnType) = DescriptorParser.parseMethodDescriptor(
        descriptor = methodDescriptor,
    ).apply {
        if (enableDetailLog) {
            println(
                "createProxyMethod -----> $slashClassName.$methodName, methodDescriptor: $methodDescriptor"
            )
            println("parameterList: $first")
            println("returnType: $second")
        }
    }

    val hasReturn = returnType != DescriptorParser.VOID

    // 插桩的方法的参数个数. 目前是两个, 一个是方法标记, 一个是方法名
    // 后续应该方法名会被舍弃.
    val instrumentationMethodParameterCount = 2

    // 初始的方法参数个数, 如果是 static 方法那就少一个
    val initStackCount = parameterList.map {
        when (it) {
            DescriptorParser.LONG, DescriptorParser.DOUBLE -> {
                2
            }

            else -> 1
        }
    }.sum() + if (isStaticMethod) {
        0
    } else {
        1
    }

    if (enableDetailLog) {
        println("initStackCount: $initStackCount")
    }

    var maxStackCount = initStackCount

    // 如果有返回值
    if (hasReturn) {
        maxStackCount++
    }

    // 因为我这里有 try catch, 肯定有异常的处理, 所以也要 ++
    maxStackCount++

    if (enableDetailLog) {
        println("maxStackCount: $maxStackCount")
    }

    // 倒数两个用来存储返回值和异常

    val returnValueStoreIndex = maxStackCount - 2
    val exceptionValueStoreIndex = maxStackCount - 1

    if (enableDetailLog) {
        println("returnValueStoreIndex: $returnValueStoreIndex, exceptionValueStoreIndex: $exceptionValueStoreIndex")
    }

    targetMethodVisitor.visitCode()

    // 真实的方法的调用的开始和结束
    val realMethodLabelStart = Label()
    val realMethodLabelEnd = Label()

    // trace start 方法的开始和结束标签
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
            methodFirstLineNumber?.let { lineNumber ->
                targetMethodVisitor.visitLineNumber(
                    max(
                        a = 1,
                        (lineNumber - 1),
                    ),
                    methodStartTraceStartLabel,
                )
            }
            targetMethodVisitor.visitLdcInsn(methodFlag)
            // targetMethodVisitor.visitLdcInsn("$dotClassName.$methodName")
            targetMethodVisitor.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                "com/vistring/trace/MethodTracker",
                "start",
                // "()V",
                "(I)V",
                // "(ILjava/lang/String;)V",
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
            // 如果不是静态的会被用掉一个用来存储 this
            val parameterIndexUses = if (isStaticMethod) {
                0
            } else {
                1
            }
            // 在循环的过程中, 因为 Long 和 Double 需要占用两个数据卡槽, 所以需要一个偏移量
            var indexOffset = 0
            parameterList.forEachIndexed { index, methodParameter ->
                when (methodParameter) {
                    DescriptorParser.BYTE -> {
                        targetMethodVisitor.visitVarInsn(
                            Opcodes.ILOAD,
                            index + parameterIndexUses + indexOffset
                        )
                    }

                    DescriptorParser.CHAR -> {
                        targetMethodVisitor.visitVarInsn(
                            Opcodes.ILOAD,
                            index + parameterIndexUses + indexOffset
                        )
                    }

                    DescriptorParser.DOUBLE -> {
                        targetMethodVisitor.visitVarInsn(
                            Opcodes.DLOAD,
                            index + parameterIndexUses + indexOffset
                        )
                        indexOffset++
                    }

                    DescriptorParser.FLOAT -> {
                        targetMethodVisitor.visitVarInsn(
                            Opcodes.FLOAD,
                            index + parameterIndexUses + indexOffset
                        )
                    }

                    DescriptorParser.INT -> {
                        targetMethodVisitor.visitVarInsn(
                            Opcodes.ILOAD,
                            index + parameterIndexUses + indexOffset
                        )
                    }

                    DescriptorParser.LONG -> {
                        targetMethodVisitor.visitVarInsn(
                            Opcodes.LLOAD,
                            index + parameterIndexUses + indexOffset
                        )
                        indexOffset++
                    }

                    DescriptorParser.SHORT -> {
                        targetMethodVisitor.visitVarInsn(
                            Opcodes.ILOAD,
                            index + parameterIndexUses + indexOffset
                        )
                    }

                    DescriptorParser.BOOLEAN -> {
                        targetMethodVisitor.visitVarInsn(
                            Opcodes.ILOAD,
                            index + parameterIndexUses + indexOffset
                        )
                    }

                    else -> {
                        targetMethodVisitor.visitVarInsn(
                            Opcodes.ALOAD,
                            index + parameterIndexUses + indexOffset
                        )
                    }
                }
            }

            if (isStaticMethod) {
                targetMethodVisitor.visitMethodInsn(
                    Opcodes.INVOKESTATIC,
                    slashClassName,
                    nameForTrace,
                    methodDescriptor,
                    false
                )
            } else {
                targetMethodVisitor.visitMethodInsn(
                    Opcodes.INVOKEVIRTUAL,
                    slashClassName,
                    nameForTrace,
                    methodDescriptor,
                    false
                )
            }

            // 如果有返回值, 先存储返回值
            if (hasReturn) {
                targetMethodVisitor.visitVarInsn(
                    when (returnType) {
                        DescriptorParser.BYTE -> Opcodes.ISTORE
                        DescriptorParser.CHAR -> Opcodes.ISTORE
                        DescriptorParser.DOUBLE -> Opcodes.DSTORE
                        DescriptorParser.FLOAT -> Opcodes.FSTORE
                        DescriptorParser.INT -> Opcodes.ISTORE
                        DescriptorParser.LONG -> Opcodes.LSTORE
                        DescriptorParser.SHORT -> Opcodes.ISTORE
                        DescriptorParser.BOOLEAN -> Opcodes.ISTORE
                        else -> Opcodes.ASTORE
                    },
                    returnValueStoreIndex,
                )
            }
            targetMethodVisitor.visitLabel(realMethodLabelEnd)
            targetMethodVisitor.visitInsn(Opcodes.NOP)
        }

        // 调用 MethodTracker.end 方法
        run {
            targetMethodVisitor.visitLabel(
                methodEndTraceStartLabelNormal,
            )
            targetMethodVisitor.visitLdcInsn(methodFlag)
            // targetMethodVisitor.visitLdcInsn("$dotClassName.$methodName")
            targetMethodVisitor.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                "com/vistring/trace/MethodTracker",
                "end",
                // "()V",
                "(I)V",
                // "(ILjava/lang/String;)V",
                false,
            )
            targetMethodVisitor.visitLabel(
                methodEndTraceEndLabelNormal
            )
            targetMethodVisitor.visitInsn(Opcodes.NOP)
        }

        // 表示临时表量表中的和进入方法的时候一样
        targetMethodVisitor.visitFrame(
            Opcodes.F_SAME,
            0,
            null,
            0,
            null,
        )

        // 如果没有返回值
        if (hasReturn) {
            // 加载出返回值
            targetMethodVisitor.visitVarInsn(
                when (returnType) {
                    DescriptorParser.BYTE -> Opcodes.ILOAD
                    DescriptorParser.CHAR -> Opcodes.ILOAD
                    DescriptorParser.DOUBLE -> Opcodes.DLOAD
                    DescriptorParser.FLOAT -> Opcodes.FLOAD
                    DescriptorParser.INT -> Opcodes.ILOAD
                    DescriptorParser.LONG -> Opcodes.LLOAD
                    DescriptorParser.SHORT -> Opcodes.ILOAD
                    DescriptorParser.BOOLEAN -> Opcodes.ILOAD
                    else -> Opcodes.ALOAD
                },
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
            // targetMethodVisitor.visitLdcInsn("$dotClassName.$methodName")
            targetMethodVisitor.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                "com/vistring/trace/MethodTracker",
                "end",
                // "()V",
                "(I)V",
                // "(ILjava/lang/String;)V",
                false,
            )
            targetMethodVisitor.visitLabel(
                methodEndTraceEndLabelException
            )
            targetMethodVisitor.visitInsn(Opcodes.NOP)
        }

        // 抛出异常
        targetMethodVisitor.visitVarInsn(
            Opcodes.ALOAD,
            exceptionValueStoreIndex,
        )
        targetMethodVisitor.visitInsn(Opcodes.ATHROW)
    }

    targetMethodVisitor.visitLabel(lastStartLabel)
    targetMethodVisitor.visitInsn(Opcodes.NOP)

    val finalMaxStack = initStackCount
        // 因为插桩最少需要两个数据卡槽, methodFlag 和 methodName
        // methodFlag 是 int 是 1 个数据卡槽, methodName 是 String 是 1 个数据卡槽
        .coerceAtLeast(minimumValue = instrumentationMethodParameterCount)
    val maxLocals = maxStackCount

    if (enableDetailLog) {
        println("finalMaxStack: $finalMaxStack, maxLocals: $maxLocals")
        println()
    }

    targetMethodVisitor.visitMaxs(
        finalMaxStack,
        maxLocals,
    )

}

/**
 * 这个 ClassVisitor 用来插桩. 对原有方法进行改进
 * 目前用到的思路主要是两步:
 * 1. 原有方法名增加 $forTrace 后缀, 这样就变成了 xxx$forTrace 方法
 * 2. 我自己生成一个原有的方法名的方法. 内部会调用 xxx$forTrace 方法. 然后用 try finally 进行包裹
 *
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
 */
private class InstrumentationClassVisitor(
    val asmApi: Int,
    val randomAccessFile: RandomAccessFile,
    val nextClassVisitor: ClassVisitor,
    val enableLog: Boolean,
    val methodAnnoPathMatcher: PathMatcher,
    val slashClassName: String,
    val dotClassName: String,
    val methodFlagCounter: AtomicInteger,
    // 插桩成功的方法会收集在这里. key 是方法的唯一标记, value 是方法的唯一标记
    val instrumentSuccessfulMethodList: MutableMap<String, Int>,
) : ClassVisitor(asmApi, nextClassVisitor) // 占位
{

    private var isInterface: Boolean = false

    override fun visit(
        version: Int,
        access: Int,
        name: String?,
        signature: String?,
        superName: String?,
        interfaces: Array<out String>?
    ) {
        isInterface = access and Opcodes.ACC_INTERFACE != 0
        super.visit(version, access, name, signature, superName, interfaces)
    }

    override fun visitMethod(
        access: Int,
        name: String,
        descriptor: String?,
        signature: String?,
        exceptions: Array<out String>?,
    ): MethodVisitor? {

        val isLog = enableLog && EnableDetailLog

        // 是否可以插桩
        val canInstrument = !isInterface &&
                !descriptor.isNullOrBlank() &&
                "<init>" != name &&
                "<clinit>" != name &&
                (access and Opcodes.ACC_NATIVE == 0) &&
                (access and Opcodes.ACC_ABSTRACT == 0) &&
                (access and Opcodes.ACC_ANNOTATION == 0)

        if (isLog) {
            println() // 换行
            println("InstrumentationClassVisitor.visitMethod -----> $slashClassName.$name, canInstrument: $canInstrument")
        }

        return if (canInstrument) {

            val methodFlag = methodFlagCounter.incrementAndGet()

            MethodMapping.writeToLine(
                randomAccessFile = randomAccessFile,
                lineNumber = methodFlag,
                methodSignature = "$dotClassName.$name:${descriptor.orEmpty()}",
            )

            val nameForTrace = "$name$RENAME_FOR_SUFFIX$methodFlag"
            val proxyMethodVisitor = nextClassVisitor.visitMethod(
                access,
                name,
                descriptor,
                signature,
                exceptions,
            )
            return object : MethodVisitor(asmApi, proxyMethodVisitor) {

                private var isCalledVisitCode: Boolean = false
                private var firstLineNumber: Int? = null

                private val methodAnnoMatchResultList = mutableListOf<Boolean>()
                private var canInstrumentFromMethodAnno = false

                override fun visitAnnotation(
                    descriptor: String?,
                    visible: Boolean
                ): AnnotationVisitor {
                    // descriptor: Lorg/jetbrains/annotations/Nullable;
                    val annoDotPath =
                        descriptor?.removePrefix("L")?.removeSuffix(";")?.replace("/", ".")
                    methodAnnoMatchResultList.add(
                        element = methodAnnoPathMatcher.isMatch(
                            target = annoDotPath,
                        ),
                    )
                    if (isLog) {
                        println("InstrumentationClassVisitor.visitAnnotation descriptor = $descriptor, visible = $visible")
                    }
                    return super.visitAnnotation(descriptor, visible)
                }

                override fun visitLineNumber(line: Int, start: Label) {
                    if (isCalledVisitCode && firstLineNumber == null) {
                        firstLineNumber = line
                    }
                    super.visitLineNumber(line, start)
                }

                override fun visitCode() {
                    // 从方法的注解上判断这个方法是否需要插桩
                    canInstrumentFromMethodAnno = methodAnnoMatchResultList.all { it }
                    if (isLog) {
                        println("InstrumentationClassVisitor.visitMethod.visitCode called, canInstrumentFromMethodAnno = $canInstrumentFromMethodAnno")
                    }
                    if (canInstrumentFromMethodAnno) {
                        // visitCode 压着不调用先.
                        mv = null
                    }
                    super.visitCode()
                    isCalledVisitCode = true
                }

                override fun visitEnd() {
                    if (canInstrumentFromMethodAnno) {
                        if (isLog) {
                            println("InstrumentationClassVisitor.visitMethod.visitEnd 准备进行插桩, name = $name, descriptor = $descriptor")
                        }
                        proxyMethodVisitor.visitCode()
                        proxyMethodVisitor.createProxyMethod(
                            enableLog = isLog,
                            slashClassName = slashClassName,
                            dotClassName = dotClassName,
                            methodFirstLineNumber = firstLineNumber,
                            methodAccess = access,
                            methodDescriptor = descriptor!!,
                            methodFlag = methodFlag,
                            methodName = name,
                            nameForTrace = nameForTrace,
                        )
                    }
                    mv = proxyMethodVisitor
                    // proxyMethodVisitor.visitEnd()
                    super.visitEnd()
                    if (canInstrumentFromMethodAnno) {
                        // 记录插桩成功的方法信息
                        instrumentSuccessfulMethodList[
                            "${slashClassName}.$name<$descriptor>"
                        ] = methodFlag
                    }
                    methodAnnoMatchResultList.clear()
                    canInstrumentFromMethodAnno = false
                    isCalledVisitCode = false
                    firstLineNumber = null
                }

            }
        } else {
            super.visitMethod(access, name, descriptor, signature, exceptions)
        }

    }

}

/**
 * [instrumentSuccessfulMethodSet] 中记录的方法都是需要 Rename 的
 * 假设有一个类, 实际做的是输出 123
 * ```Kotlin
 * class Test {
 *    fun test1() {
 *      println("123")
 *    }
 * }
 * ```
 * 在插桩后, 会变成下面的插桩后的代码. 那此时真实的 test1 方法已经被修改.
 * ```Kotlin
 * class Test {
 *    fun test1() {
 *      try {
 *          MethodTracker.start()
 *          // 这里是真实的方法, 会被重命名
 *          this.test1$forTrace()
 *      } finally {
 *          MethodTracker.end()
 *      }
 *    }
 * }
 * ```
 * 所以需要重新读取 class 文件, 对原有方法进行一个重命名, 加上 [RenameForTraceClassVisitor.RENAME_FOR_SUFFIX] 后缀
 * 这样子操作之后, 最后生成的代码就是下面这样子的. 会多出一个 xxx$forTrace 方法是本身待插桩方法的实现
 * ```Kotlin
 * class Test {
 *    fun test1() {
 *      try {
 *          MethodTracker.start()
 *          // 这里是真实的方法, 会被重命名
 *          this.test1$forTrace()
 *      } finally {
 *          MethodTracker.end()
 *      }
 *    }
 *    fun test1$forTrace() {
 *      println("123")
 *    }
 * }
 * ```
 */
private class RenameForTraceClassVisitor(
    val asmApi: Int,
    val nextClassVisitor: ClassVisitor,
    val enableLog: Boolean,
    val slashClassName: String,
    val instrumentSuccessfulMethodSet: Map<String, Int>,
) : ClassVisitor(asmApi) // 占位
{

    companion object {
        const val RENAME_FOR_SUFFIX = "\$forTrace"
    }

    override fun visitMethod(
        access: Int,
        name: String,
        descriptor: String?,
        signature: String?,
        exceptions: Array<out String>?,
    ): MethodVisitor? {

        val isLog = enableLog && EnableDetailLog

        if (isLog) {
            println() // 换行
            println("RenameForTraceClassVisitor.visitMethod, name = $name, descriptor = $descriptor")
        }

        val methodFlag = instrumentSuccessfulMethodSet["${slashClassName}.$name<$descriptor>"]

        // 是否可以插桩
        val canInstrument = methodFlag != null

        val nameForTrace = "$name$RENAME_FOR_SUFFIX$methodFlag"

        if (isLog) {
            if (canInstrument) {
                println("RenameForTraceClassVisitor.needRename: $name ----> $nameForTrace")
            }
        }

        return if (canInstrument) {
            nextClassVisitor.visitMethod(
                access,
                nameForTrace,
                descriptor,
                signature,
                exceptions
            )
        } else {
            null
        }

    }

}

/**
 * 对 Asm 的基本使用进行封装.
 *
 * @see [InstrumentationClassVisitor]
 */
@Throws(Exception::class)
private fun transformClassBytecode(
    asmApi: Int,
    randomAccessFile: RandomAccessFile,
    enableLog: Boolean,
    methodAnnoPathMatcher: PathMatcher,
    originClassBytes: ByteArray,
    // xxx/xxx/xxx
    slashClassName: String,
    // xxx.xxx.xxx
    dotClassName: String,
    methodFlagCounter: AtomicInteger,
): ByteArray {
    // 最原始的字节码
    val originClassReader = ClassReader(
        originClassBytes,
    )
    val outputClassWriter =
        ClassWriter(ClassWriter.COMPUTE_MAXS or ClassWriter.COMPUTE_FRAMES)

    // xxx/xxx/xxx.(IIJ)V --> 123
    // key 成功插桩的方法信息, value 是方法唯一标记
    val instrumentSuccessfulMethodList = mutableMapOf<String, Int>()

    // 尝试方法插桩的方法
    val instrumentationClassVisitor = InstrumentationClassVisitor(
        asmApi = asmApi,
        randomAccessFile = randomAccessFile,
        nextClassVisitor = outputClassWriter,
        enableLog = enableLog,
        methodAnnoPathMatcher = methodAnnoPathMatcher,
        slashClassName = slashClassName,
        dotClassName = dotClassName,
        methodFlagCounter = methodFlagCounter,
        instrumentSuccessfulMethodList = instrumentSuccessfulMethodList,
    )
    originClassReader.accept(instrumentationClassVisitor, ClassReader.EXPAND_FRAMES)

    // println("instrumentSuccessfulMethodList = ${instrumentSuccessfulMethodList.joinToString(separator = "\n")}")

    // 把一些支持插桩的方法, 改成 xxx$forTrace 的方法
    val renameForTraceClassVisitor = RenameForTraceClassVisitor(
        asmApi = asmApi,
        enableLog = enableLog,
        nextClassVisitor = outputClassWriter,
        slashClassName = slashClassName,
        instrumentSuccessfulMethodSet = Collections.unmodifiableMap(instrumentSuccessfulMethodList),
    )
    originClassReader.accept(renameForTraceClassVisitor, ClassReader.EXPAND_FRAMES)

    return outputClassWriter.toByteArray()
}

/**
 * 字节码插桩的实现
 */
object BytecodeInstrumentation {

    private const val METHOD_TRACKER_CLASS_NAME = "com.vistring.trace.MethodTracker"
    private const val METHOD_TRACE_INFO_CLASS_NAME = "com.vistring.trace.MethodTraceInfo"
    private const val METHOD_INFO_UPLOADER_CLASS_NAME = "com.vistring.trace.MethodInfoUploader"

    private val CLASS_NAME_IGNORE_LIST = listOf(
        METHOD_TRACKER_CLASS_NAME,
        METHOD_TRACE_INFO_CLASS_NAME,
        METHOD_INFO_UPLOADER_CLASS_NAME,
        // 这个会在 MethodTracker 的 start 方法中调用, 会导致死循环,
        "kotlin.jvm.internal.Intrinsics",
    )

    private const val ASM_API = Opcodes.ASM9

    // 方法唯一标记的 flag, 使用的时候需要先自增再获取
    private val methodFlagCounter = AtomicInteger()

    /**
     * @param costTimeThreshold 耗时阈值
     * @return 返回尝试插桩后的字节数组, 如果插桩过程中失败, 将会返回原来的字节数组. 否则就是插桩过后的字节码会被返回
     */
    fun tryInstrument(
        resourcesFolder: File?,
        costTimeThreshold: Long,
        enableLog: Boolean = false,
        classPathMatcher: PathMatcher,
        methodAnnoPathMatcher: PathMatcher,
        // xxx/xxx/xxx.class
        classFullName: String,
        classFileInputStream: InputStream,
    ): ByteArray {

        // 原来 class 的字节数组
        val originClassBytes = classFileInputStream.readBytes()

        if (resourcesFolder == null) {
            return originClassBytes
        }

        /**
         * 内部的每一行都是一个具体方法的完整路径
         * 而第几行 index 为 key, 对应的 value 是 method 的完整路径
         */
        val methodMappingFile = File(resourcesFolder, "methodTraceMapping.txt").apply {
            if (!exists()) {
                this.delete()
                this.createNewFile()
            }
        }

        return RandomAccessFile(methodMappingFile, "rw")
            .use { randomAccessFile ->
                /*if ("xxx/xxx/xxx.class" != classFullName) {
                    return originClassBytes
                }*/

                // xxx/xxx/xxx
                val slashClassName = classFullName
                    .removeSuffix(suffix = ".class")

                // xxx.xxx.xxx
                val dotClassName = slashClassName
                    .replace("/", ".")

                // 如果是 tracker 类, 需要更改 COST_TIME_THRESHOLD 常量的值
                if (METHOD_TRACKER_CLASS_NAME == dotClassName) {
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
                                if ("getCostTimeThreshold" == name) {
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
                            println("$VSMethodTracePlugin transform $METHOD_TRACKER_CLASS_NAME fail: $dotClassName")
                        }
                    }.getOrNull() ?: originClassBytes
                }

                // 此包下是 trace 耗时统计的模块, 不需要处理
                if (CLASS_NAME_IGNORE_LIST.any { it == dotClassName }) {
                    originClassBytes
                } else if (classPathMatcher.isMatch(target = dotClassName)) {
                    if (enableLog) {
                        println()
                        println("${VSMethodTracePlugin.TAG}: transformClassBytecode start ===================================== $dotClassName")
                    }
                    kotlin.runCatching {
                        transformClassBytecode(
                            asmApi = ASM_API,
                            randomAccessFile = randomAccessFile,
                            enableLog = enableLog,
                            methodAnnoPathMatcher = methodAnnoPathMatcher,
                            originClassBytes = originClassBytes,
                            slashClassName = slashClassName,
                            dotClassName = dotClassName,
                            methodFlagCounter = methodFlagCounter,
                        )
                    }.apply {
                        if (enableLog && this.isFailure) {
                            println("$VSMethodTracePlugin transform fail: $dotClassName, ${this.exceptionOrNull()?.message}")
                            // this.exceptionOrNull()?.printStackTrace()
                        }
                    }.getOrNull() ?: originClassBytes
                } else {
                    originClassBytes
                }
            }

    }

    /**
     * 每次 build 的时候重置一下
     */
    fun resetMethodFlag() {
        methodFlagCounter.set(0)
    }

}