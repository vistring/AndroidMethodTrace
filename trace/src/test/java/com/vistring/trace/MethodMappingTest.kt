package com.vistring.trace

import org.junit.Test
import java.io.File
import java.io.RandomAccessFile
import kotlin.random.Random

class MethodMappingTest {

    @Test
    fun testWriteToLine() {
        // 获取临时目录
        val tempDir = System.getProperty("java.io.tmpdir")
        val tempFile = File(tempDir, "methodTraceMapping.txt")
        val randomAccessFile = RandomAccessFile(tempFile, "rw")
        val lineCount = Random.nextInt(10, 100)
        val methodSignatureMap = buildMap {
            for (i in 1..lineCount) {
                put(
                    i,
                    "com.vistring.trace.Test.testWriteToLine$i",
                )
            }
        }
        methodSignatureMap.forEach { (lineNumber, methodSignature) ->
            MethodMapping.writeToLine(
                randomAccessFile = randomAccessFile,
                lineNumber = lineNumber,
                methodSignature = methodSignature,
            )
        }

        // 验证
        methodSignatureMap.forEach { (lineNumber, methodSignature) ->
            val methodSignatureInMapping = MethodMapping.getByLineNumber(
                randomAccessFile = randomAccessFile,
                lineNumber = lineNumber,
            )
            assert(
                value = methodSignature == methodSignatureInMapping,
                lazyMessage = {
                    "$methodSignature is not equal to $methodSignatureInMapping"
                },
            )
        }
        randomAccessFile.close()
        tempFile.delete()
    }

}