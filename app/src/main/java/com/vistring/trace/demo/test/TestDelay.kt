package com.vistring.trace.demo.test

object TestDelay {

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

}