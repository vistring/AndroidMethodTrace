package com.vistring.trace

import com.vistring.trace.MethodMapping.MAX_RECORD_LENGTH
import java.io.RandomAccessFile

/**
 * 为了管理 lineNumber 和 methodSignature 的关系，我们将它们写到一个文件中
 * 我们认为每一个 methodSignature 都是小于 [MAX_RECORD_LENGTH]
 */
object MethodMapping {

    const val MAX_RECORD_LENGTH = 1000

    fun getByLineNumber(
        randomAccessFile: RandomAccessFile,
        lineNumber: Int,
    ): String {
        randomAccessFile.seek((lineNumber - 1L) * MAX_RECORD_LENGTH)
        val bytes = ByteArray(MAX_RECORD_LENGTH)
        randomAccessFile.readFully(bytes, 0, MAX_RECORD_LENGTH)
        return bytes.decodeToString().trim()
    }

    fun writeToLine(
        randomAccessFile: RandomAccessFile,
        lineNumber: Int,
        methodSignature: String,
    ) {
        // -1 是因为最后一个字符是换行符
        if (methodSignature.length > MAX_RECORD_LENGTH - 1) {
            throw IllegalArgumentException("Method Signature is too long")
        }
        // 弄一个换行符是因为方便读取的时候可以直接读取到整行
        if (lineNumber > 1) {
            randomAccessFile.seek((lineNumber - 1L) * MAX_RECORD_LENGTH - 1)
            randomAccessFile.writeBytes("\n")
        }
        randomAccessFile.seek((lineNumber - 1L) * MAX_RECORD_LENGTH)
        randomAccessFile.writeBytes(
            methodSignature,
        )
        // 如果 methodSignature < MAX_RECORD_LENGTH - 1, 则补上空格
        if (methodSignature.length < MAX_RECORD_LENGTH - 1) {
            randomAccessFile.seek((lineNumber - 1L) * MAX_RECORD_LENGTH + methodSignature.length)
            randomAccessFile.writeBytes(" ".repeat(MAX_RECORD_LENGTH - methodSignature.length - 1))
        }

    }

}