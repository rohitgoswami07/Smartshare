package com.rohit.smartshare

import android.app.Application
import com.rohit.smartshare.data.AppContainer

class SmartShareApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppContainer.initialize(this)
    }
}