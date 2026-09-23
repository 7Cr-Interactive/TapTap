package com.app.taptap.ads

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import com.app.taptap.R
import com.app.taptap.analytics.AnalyticsManager
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppOpenAdManager @Inject constructor(
    @ApplicationContext context: Context,
    private val analyticsManager: AnalyticsManager
) : Application.ActivityLifecycleCallbacks {

    private val application: Application = context.applicationContext as Application
    private var appOpenAd: AppOpenAd? = null
    private var isLoadingAd = false
    private var isShowingAd = false
    private var loadTime: Long = 0
    private var currentActivity: Activity? = null

    init {
        application.registerActivityLifecycleCallbacks(this)
    }

    /**
     * Preloads an App Open Ad if not already loaded or loading.
     */
    fun fetchAd() {
        if (isAdAvailable() || isLoadingAd) {
            return
        }

        isLoadingAd = true
        val request = AdRequest.Builder().build()
        val adUnitId = application.getString(R.string.app_open_ad_unit_id)

        AppOpenAd.load(
            application,
            adUnitId,
            request,
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    appOpenAd = ad
                    isLoadingAd = false
                    loadTime = Date().time
                    analyticsManager.logAdStatus("app_open", "loaded")
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    appOpenAd = null
                    isLoadingAd = false
                    analyticsManager.logAdStatus("app_open", "failed", loadAdError.code)
                }
            }
        )
    }

    /**
     * Checks if ad exists and was loaded within 4 hours.
     */
    private fun isAdAvailable(): Boolean {
        val numHours = 4L
        val dateDifference = Date().time - loadTime
        val numMilliSecondsPerHour = 3600000L
        return appOpenAd != null && dateDifference < numMilliSecondsPerHour * numHours
    }

    /**
     * Displays the App Open Ad if available.
     */
    fun showAdIfAvailable(activity: Activity) {
        // Do not show App Open Ad during active gameplay to avoid user frustration
        if (activity.javaClass.simpleName == "GameActivity") {
            return
        }

        if (isShowingAd) {
            return
        }

        if (!isAdAvailable()) {
            fetchAd()
            return
        }

        appOpenAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                appOpenAd = null
                isShowingAd = false
                fetchAd()
                analyticsManager.logAdStatus("app_open", "dismissed")
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                appOpenAd = null
                isShowingAd = false
                fetchAd()
                analyticsManager.logAdStatus("app_open", "show_failed", adError.code)
            }

            override fun onAdShowedFullScreenContent() {
                isShowingAd = true
                analyticsManager.logAdStatus("app_open", "displayed")
            }
        }

        isShowingAd = true
        appOpenAd?.show(activity)
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}

    override fun onActivityStarted(activity: Activity) {
        currentActivity = activity
        if (!isShowingAd) {
            showAdIfAvailable(activity)
        }
    }

    override fun onActivityResumed(activity: Activity) {
        currentActivity = activity
    }

    override fun onActivityPaused(activity: Activity) {}

    override fun onActivityStopped(activity: Activity) {}

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

    override fun onActivityDestroyed(activity: Activity) {
        if (currentActivity == activity) {
            currentActivity = null
        }
    }
}
