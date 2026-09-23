package com.app.taptap

import android.app.Application
import com.app.taptap.ads.AdManager
import com.app.taptap.ads.AppOpenAdManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class TapTapApplication : Application() {

    @Inject
    lateinit var adManager: AdManager

    @Inject
    lateinit var appOpenAdManager: AppOpenAdManager

    override fun onCreate() {
        super.onCreate()
        adManager.initialize(this)
        appOpenAdManager.fetchAd()
    }
}
