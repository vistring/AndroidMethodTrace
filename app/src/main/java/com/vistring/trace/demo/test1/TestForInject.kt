package com.vistring.trace.demo.test1

private fun testStatic1() {
}

private fun testStatic2(
    name: String, age: Int,
): String {
    return "name: $name, age: $age"
}

class TestForInject {

    fun test1() {
    }

    fun test2(
        name: String, age: Int,
    ): String {
        return "name: $name, age: $age"
    }

}