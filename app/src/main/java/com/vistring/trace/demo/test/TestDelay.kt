package com.vistring.trace.demo.test

object TestDelay {

    private fun testMethod1() {
        Thread.sleep(110)
    }

    fun testMethod2() {
        testMethod1()
        Thread.sleep(200)
        try {
            try {
                "ggg".toInt()
            } catch (ignore: Exception) {
                throw ignore
            }
        } catch (ignore: Exception) {
            throw ignore
        }
    }

}