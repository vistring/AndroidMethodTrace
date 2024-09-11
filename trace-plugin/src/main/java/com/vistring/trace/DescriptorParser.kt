package com.vistring.trace

// TODO: 这里的算法有问题
object DescriptorParser {

    // 其实上面几个也是字节码的类型本身. 只不过用一个常量代替会比较好
    // 其他类型就是字符串本身, 比如：[I 或者 Ljava/lang/String

    const val VOID = "V" // 这个只会在返回值中出现, 也就是 (IIJ)V 这种情况

    const val BYTE = "B"
    const val CHAR = "C"
    const val FLOAT = "F"
    const val INT = "I"
    const val SHORT = "S"
    const val BOOLEAN = "Z"

    // 这两个是需要消耗 8 字节的
    const val DOUBLE = "D"
    const val LONG = "J"

    /**
     * (Ljava/lang/String;I)V
     * ([I)[I
     * (ZZLkotlin/jvm/functions/Function2;Landroidx/compose/runtime/Composer;II)V
     * (Ljava/lang/String;[I[Ljava/lang/String;)V
     */
    fun parseMethodDescriptor(descriptor: String): Pair<List<String>, String> {
        val leftParenthesisIndex = descriptor.indexOf('(')
        val rightParenthesisIndex = descriptor.indexOf(')')
        require(
            value = leftParenthesisIndex != -1 && rightParenthesisIndex != -1 && leftParenthesisIndex < rightParenthesisIndex,
        )
        val parameterListStr = descriptor.substring(leftParenthesisIndex + 1, rightParenthesisIndex)
        var index = 0
        var arrStartIndex = -1
        val parameterTypes = mutableListOf<String>()
        while (index < parameterListStr.length) {
            when (val c = parameterListStr[index]) {
                '[' -> {
                    if (arrStartIndex == -1) {
                        arrStartIndex = index
                    }
                    index++
                }
                'L' -> {
                    val endIndex = parameterListStr.indexOf(';', index)
                    require(endIndex != -1)
                    val startIndex = if (arrStartIndex == -1) {
                        index
                    } else {
                        arrStartIndex
                    }
                    parameterTypes.add(
                        element = parameterListStr.substring(startIndex, endIndex + 1),
                    )
                    index = endIndex + 1
                    arrStartIndex = -1
                }
                else -> {
                    when (c) {
                        'B', 'C', 'D', 'F', 'I', 'J', 'S', 'Z' -> {
                            val startIndex = if (arrStartIndex == -1) {
                                index
                            } else {
                                arrStartIndex
                            }
                            parameterTypes.add(
                                element = parameterListStr.substring(startIndex, index + 1),
                            )
                            index++
                            arrStartIndex = -1
                        }

                        else -> error("Unknown type: $descriptor")
                    }
                }
            }
        }
        val returnType = descriptor.substring(rightParenthesisIndex + 1).trim()
        return parameterTypes to returnType
    }

}