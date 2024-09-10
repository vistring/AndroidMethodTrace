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

    // (Ljava/lang/String;I)V
    fun parseMethodDescriptor(descriptor: String): Pair<List<String>, String> {
        val parameterTypes = mutableListOf<String>()
        var currentType = ""
        var index = 0
        while (index < descriptor.length) {
            when (descriptor[index]) {
                '(' -> {
                    index++
                }
                ')' -> break
                'L' -> {
                    val endIndex = descriptor.indexOf(';', index)
                    currentType = descriptor.substring(index, endIndex)
                    parameterTypes.add(currentType)
                    index = endIndex + 1
                }

                'B', 'C', 'D', 'F', 'I', 'J', 'S', 'Z' -> {
                    currentType = when (descriptor[index]) {
                        'B' -> BYTE
                        'C' -> CHAR
                        'D' -> DOUBLE
                        'F' -> FLOAT
                        'I' -> INT
                        'J' -> LONG
                        'S' -> SHORT
                        'Z' -> BOOLEAN
                        else -> error("Unknown type")
                    }
                    parameterTypes.add(currentType)
                    index++
                }

                else -> index++
            }
        }
        val returnTypeStartIndex = descriptor.indexOf(')') + 1
        val returnType = if (returnTypeStartIndex < descriptor.length) {
            when (descriptor[returnTypeStartIndex]) {
                'V' -> VOID
                'L' -> {
                    val endIndex = descriptor.indexOf(';', returnTypeStartIndex)
                    descriptor.substring(returnTypeStartIndex + 1, endIndex + 1)
                }

                'B', 'C', 'D', 'F', 'I', 'J', 'S', 'Z' -> when (descriptor[returnTypeStartIndex]) {
                    'B' -> BYTE
                    'C' -> CHAR
                    'D' -> DOUBLE
                    'F' -> FLOAT
                    'I' -> INT
                    'J' -> LONG
                    'S' -> SHORT
                    'Z' -> BOOLEAN
                    else -> error("Unknown type")
                }

                else -> error("Unknown type")
            }
        } else {
            error("Unknown type")
        }
        return parameterTypes to returnType
    }

}