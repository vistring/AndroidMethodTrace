package com.vistring.trace

import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.commons.AdviceAdapter
import java.io.InputStream

object AsmTestUtil {

    fun test(
        name: String,
        classFileInputStream: InputStream,
    ): ByteArray {
        val originClassBytes = classFileInputStream.readBytes()
        val className = name.removeSuffix(suffix = ".class").replace("/", ".")
        return if (className.startsWith("com.vistring.vlogger.android")) {
            println("vsMethodTrace.className = $className")
            val asmApi = Opcodes.ASM9
            return kotlin.runCatching {
                val classReader = ClassReader(
                    originClassBytes,
                )
                val classWriter = ClassWriter(ClassWriter.COMPUTE_MAXS)

                val classVisitor = object : ClassVisitor(asmApi, classWriter) {
                    override fun visit(
                        version: Int,
                        access: Int,
                        name: String?,
                        signature: String?,
                        superName: String?,
                        interfaces: Array<out String>?
                    ) {
                        super.visit(version, access, name, signature, superName, interfaces)
                        // println("访问到 class: $name, access = $access, isAnnotation = ${access and Opcodes.ACC_ANNOTATION}, signature = $signature， superName = $superName")
                    }

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
                        return object :
                            AdviceAdapter(asmApi, originMethodVisitor, access, name, descriptor) {

                            override fun onMethodEnter() {
                                super.onMethodEnter()
                                visitLdcInsn("$className.$name")
                                visitMethodInsn(
                                    Opcodes.INVOKESTATIC,
                                    "com/vistring/trace/MethodTracker",
                                    "start",
                                    // "()V",
                                    "(Ljava/lang/String;)V",
                                    false,
                                )
                            }

                            override fun onMethodExit(opcode: Int) {
                                super.onMethodExit(opcode)
                                visitLdcInsn("$className.$name")
                                visitMethodInsn(
                                    Opcodes.INVOKESTATIC,
                                    "com/vistring/trace/MethodTracker",
                                    "end",
                                    // "()V",
                                    "(Ljava/lang/String;)V",
                                    false,
                                )
                            }
                        }
                    }
                }
                classReader.accept(classVisitor, ClassReader.EXPAND_FRAMES)

                classWriter.toByteArray()
            }.apply {
                if (this.isFailure) {
                    this.exceptionOrNull()?.printStackTrace()
                }
            }.getOrNull()?: originClassBytes
        } else {
            originClassBytes
        }
    }

}