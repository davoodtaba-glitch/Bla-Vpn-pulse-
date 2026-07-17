package com.xraypulse.app

import android.app.Application
import com.xraypulse.app.core.xray.XrayController

class XrayPulseApp : Application() {
    override fun onCreate() {
        super.onCreate()
        XrayController.initEnv(this)
    }
}
