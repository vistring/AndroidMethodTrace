package com.vistring.trace.demo.test1


class TestForInject {

    private fun test0(): Int {
        return 1
    }

    fun test1(): Int {
        try {
            return this.test0()
        } finally {
            println("1123")
        }
    }

    /*fun test2(
        name: String, age: Int,
    ): String {
        return "name: $name, age: $age"
    }*/

}