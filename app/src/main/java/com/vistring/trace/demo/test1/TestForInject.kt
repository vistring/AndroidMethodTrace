package com.vistring.trace.demo.test1


class TestForInject {

    /*fun test11111(
        name: String,
        arr: IntArray,
        arr1: Array<String>,
    ): Array<Array<Pair<String, Int>>> {
        TODO()
    }*/

    /*private fun test0(): Int {
        return 1
    }

    fun test1(): Int {
        try {
            return this.test0()
        } finally {
            println("1123")
        }
    }

    fun test2(
        name: String, age: Int,
    ): String {
        return "name: $name, age: $age"
    }

    fun test3(
        name: String, age: Int,
    ): IntArray {
        return intArrayOf(1,3,4)
    }

    fun test4(
        name: String, age: Int,
    ): Array<Int> {
        return arrayOf(1,3,4)
    }*/

    // ([I)[I
    fun test5(
        arr: IntArray,
    ): IntArray {
        println(arr.last())
        return arr
    }

    // ([I)[I
    fun test55(
        arr: IntArray,
    ): IntArray {
        return test5(
            arr = arr,
        )
    }

    // ([I)[I
    /*fun test6(
        arr: Array<IntArray>,
    ): Array<IntArray> {
        return arr
    }*/

}