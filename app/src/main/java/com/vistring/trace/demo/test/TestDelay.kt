package com.vistring.trace.demo.test

object TestDelay {

    private fun testMethod1() {
        Thread.sleep(100)
    }

    fun testMethod2() {
        testMethod1()
        Thread.sleep(200)
    }

}