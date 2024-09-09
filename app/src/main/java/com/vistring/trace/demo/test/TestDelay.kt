package com.vistring.trace.demo.test

object TestDelay {

    private fun sleep10() {
        Thread.sleep(10)
    }

    private fun sleep80() {
        Thread.sleep(80)
    }

    private fun sleep120() {
        Thread.sleep(120)
    }

    fun testMethod() {
        sleep80()
        sleep80()
        sleep120()
    }

    fun testTryCatchMethod() {
        sleep80()
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

    fun testRecursion(count: Int = 11) {
        if(count == 0) {
            return
        }
        sleep10()
        testRecursion(
            count = count - 1
        )
    }

}