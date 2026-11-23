package com.app.taptap.ads

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.app.taptap.BuildConfig
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
) : Application.ActivityLifecycleCallbacks, DefaultLifecycleObserver {

    private val application: Application = context.applicationContext as Application
    private var appOpenAd: AppOpenAd? = null
    private var isLoadingAd = false
    private var isShowingAd = false
    private var loadTime: Long = 0
    private var currentActivity: Activity? = null

    private var lastAppOpenShownTime: Long = 0
    private val MIN_SHOW_INTERVAL_MS = 15 * 60 * 1000L // 15-minute frequency cap for App Open Ads
    private val MIN_FULL_SCREEN_COOLDOWN_MS = 30 * 1000L // 30s cooldown after any full-screen ad

    companion object {
        private const val TEST_APP_OPEN_ID = "ca-app-pub-3940256099942544/9257395921"
    }

    init {
        application.registerActivityLifecycleCallbacks(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    private fun getAdUnitId(): String {
        return if (BuildConfig.DEBUG) {
            TEST_APP_OPEN_ID
        } else {
            application.getString(R.string.app_open_ad_unit_id)
        }
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
        val adUnitId = getAdUnitId()

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
        // To comply with Google AdMob Policy ("More ads or paid promotional material than publisher-content"),
        // App Open Ads must NEVER be shown on utility or secondary screens (Help, HighScore, GameActivity).
        // Only allow showing App Open Ads on the main entry screen (MainActivity).
        if (activity.javaClass.simpleName != "MainActivity") {
            return
        }

        if (isShowingAd) {
            return
        }

        val currentTime = System.currentTimeMillis()
        // Frequency cap check
        if (currentTime - lastAppOpenShownTime < MIN_SHOW_INTERVAL_MS) {
            return
        }

        // Cooldown check against any recent full-screen ad (Interstitial/Rewarded)
        if (currentTime - AdManager.lastFullScreenAdShownTimestamp < MIN_FULL_SCREEN_COOLDOWN_MS) {
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
                lastAppOpenShownTime = System.currentTimeMillis()
                AdManager.lastFullScreenAdShownTimestamp = System.currentTimeMillis()
                analyticsManager.logAdStatus("app_open", "displayed")
            }
        }

        isShowingAd = true
        appOpenAd?.show(activity)
    }

    /**
     * Triggered ONLY when the application moves to the foreground (app launch / return from background),
     * preventing App Open Ads from displaying during internal activity transitions.
     */
    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        currentActivity?.let { activity ->
            if (!isShowingAd) {
                showAdIfAvailable(activity)
            }
        }
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}

    override fun onActivityStarted(activity: Activity) {
        currentActivity = activity
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
