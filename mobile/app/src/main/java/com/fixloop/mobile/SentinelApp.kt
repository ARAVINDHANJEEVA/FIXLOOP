package com.fixloop.mobile

import android.app.Application

class SentinelApp : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: SentinelApp
            private set
    }
}
