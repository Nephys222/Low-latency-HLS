package com.example.mediatest5

import android.app.Application

class MyApp :Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: MyApp
    }
}