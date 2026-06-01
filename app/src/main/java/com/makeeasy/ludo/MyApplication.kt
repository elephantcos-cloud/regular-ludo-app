package com.makeeasy.snakeludo

import android.app.Application
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.initialization.InitializationStatus
import com.makeeasy.ludo.Admob.AOManager

class MyApplication : Application() {
    private var aoManager: AOManager? = null

    override fun onCreate() {
        super.onCreate()
        MobileAds.initialize(this) { initializationStatus: InitializationStatus ->
            val statusMap = initializationStatus.adapterStatusMap
            for (adapterClass in statusMap.keys) {
                val status = checkNotNull(statusMap[adapterClass])
                aoManager = AOManager(this)
            }
        }
    }
}
