package com.dashdazz.light

import android.app.Application

class DashDazzApp : Application() {

    override fun onCreate() {
        super.onCreate()
        registerLifecycleReceivers(this)
    }
}