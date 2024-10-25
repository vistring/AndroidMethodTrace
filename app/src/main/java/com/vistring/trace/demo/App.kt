package com.vistring.trace.demo

import android.app.Application
import com.vistring.trace.MethodTracker

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        MethodTracker.init(
            application = this,
            appName = "VSMethodTraceDemo",
        )
    }

}