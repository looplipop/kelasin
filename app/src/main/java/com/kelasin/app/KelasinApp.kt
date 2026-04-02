package com.kelasin.app

import android.app.Application

class KelasinApp : Application() {
    // Singleton instance untuk akses global
    companion object {
        lateinit var instance: KelasinApp
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }
}
