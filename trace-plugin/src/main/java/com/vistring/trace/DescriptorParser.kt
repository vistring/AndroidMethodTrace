package com.vistring.trace

object DescriptorParser {

    const val VOID = "void"

    const val BYTE = "byte"
    const val CHAR = "char"
    const val DOUBLE = "double"
    const val FLOAT = "float"
    const val INT = "int"
    const val LONG = "long"
    const val SHORT = "short"
    const val BOOLEAN = "boolean"
    // 其他类型就是字符串本身, 比如：[I 或者 Ljava/lang/String

    // (Ljava/lang/String;I)V
    // ([I)[I
    fun parseMethodDescriptor(descriptor: String): Pair<List<String>, String> {
        val leftParenthesisIndex = descriptor.indexOf('(')
        val rightParenthesisIndex = descriptor.indexOf(')')
        require(
            value = leftParenthesisIndex != -1 && rightParenthesisIndex != -1 && leftParenthesisIndex < rightParenthesisIndex,
        )
        val parameterTypes = descriptor
            .substring(leftParenthesisIndex + 1, rightParenthesisIndex)
            .split(
                ";",
                ignoreCase = false,
                limit = 0,
            )
            .map {
                when {
                    it == "B" -> BYTE
                    it == "C" -> CHAR
                    it == "D" -> DOUBLE
                    it == "F" -> FLOAT
                    it == "I" -> INT
                    it == "J" -> LONG
                    it == "S" -> SHORT
                    it == "Z" -> BOOLEAN
                    it.startsWith("L") -> it.substring(1)
                    it.startsWith("[") -> it
                    else -> error("Unknown type: $descriptor")
                }
            }
        val returnTypeStr = descriptor.substring(rightParenthesisIndex + 1).trim()
        val returnType = when(returnTypeStr) {
            "V" -> VOID
            "B" -> BYTE
            "C" -> CHAR
            "D" -> DOUBLE
            "F" -> FLOAT
            "I" -> INT
            "J" -> LONG
            "S" -> SHORT
            "Z" -> BOOLEAN
            else -> {
                if (returnTypeStr.startsWith("[")) {
                    returnTypeStr
                } else if (returnTypeStr.startsWith("L")) {
                    returnTypeStr.substring(1)
                } else {
                    error("Unknown type: $descriptor")
                }
            }
        }
        return parameterTypes to returnType
    }

}