package com.app.taptap.ads

import android.app.Activity
import android.content.Context
import android.view.ViewGroup
import com.app.taptap.BuildConfig
import com.app.taptap.R
import com.app.taptap.analytics.AnalyticsManager
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAdLoadCallback
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdManager @Inject constructor(
    private val analyticsManager: AnalyticsManager
) {

    private var interstitialAd: InterstitialAd? = null
    private var rewardedAd: RewardedAd? = null
    private var rewardedInterstitialAd: RewardedInterstitialAd? = null

    private var lastInterstitialShowTime: Long = 0
    private val MIN_INTERSTITIAL_INTERVAL_MS = 30000L // 30s frequency cap for interstitials

    companion object {
        // Test Ad Unit IDs provided by Google AdMob for safe development & testing
        private const val TEST_BANNER_ID = "ca-app-pub-3940256099942544/6300978111"
        private const val TEST_INTERSTITIAL_ID = "ca-app-pub-3940256099942544/1033173712"
        private const val TEST_REWARDED_ID = "ca-app-pub-3940256099942544/5224354917"
        private const val TEST_REWARDED_INTERSTITIAL_ID = "ca-app-pub-3940256099942544/5354046379"
    }

    private fun getAdUnitId(context: Context, prodResId: Int, testAdUnitId: String): String {
        return if (BuildConfig.DEBUG) {
            testAdUnitId
        } else {
            context.getString(prodResId)
        }
    }

    /**
     * Gathers user consent via UMP SDK for GDPR / ePrivacy compliance, then initializes Mobile Ads SDK.
     */
    fun gatherConsentAndInitialize(activity: Activity, onConsentDone: () -> Unit = {}) {
        val params = ConsentRequestParameters.Builder().build()
        val consentInformation = UserMessagingPlatform.getConsentInformation(activity)

        consentInformation.requestConsentInfoUpdate(
            activity,
            params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { _ ->
                    if (consentInformation.canRequestAds()) {
                        initialize(activity)
                    }
                    onConsentDone()
                }
            },
            { _ ->
                if (consentInformation.canRequestAds()) {
                    initialize(activity)
                }
                onConsentDone()
            }
        )
    }

    /**
     * Initializes Google Mobile Ads SDK with COPPA / Content Rating policies configured.
     */
    fun initialize(context: Context) {
        val requestConfiguration = RequestConfiguration.Builder()
            .setMaxAdContentRating(RequestConfiguration.MAX_AD_CONTENT_RATING_G)
            .build()
        MobileAds.setRequestConfiguration(requestConfiguration)

        MobileAds.initialize(context) {
            preloadInterstitialAd(context)
            preloadRewardedAd(context)
            preloadRewardedInterstitialAd(context)
        }
    }

    // ==========================================
    // 1. ADAPTIVE BANNER ADS
    // ==========================================

    /**
     * Loads an Adaptive Banner Ad into the specified container view group.
     */
    fun loadAdaptiveBanner(activity: Activity, containerView: ViewGroup) {
        val adView = AdView(activity)
        adView.adUnitId = getAdUnitId(activity, R.string.banner_ad_unit_id, TEST_BANNER_ID)
        
        containerView.removeAllViews()
        containerView.addView(adView)

        val adSize = getAdSize(activity)
        adView.setAdSize(adSize)

        val adRequest = AdRequest.Builder().build()
        adView.adListener = object : AdListener() {
            override fun onAdLoaded() {
                analyticsManager.logAdStatus("banner", "loaded")
            }

            override fun onAdFailedToLoad(error: LoadAdError) {
                analyticsManager.logAdStatus("banner", "failed", error.code)
            }
        }
        adView.loadAd(adRequest)
    }

    private fun getAdSize(activity: Activity): AdSize {
        val displayMetrics = activity.resources.displayMetrics
        val adWidthPixels = displayMetrics.widthPixels
        val density = displayMetrics.density
        val adWidth = (adWidthPixels / density).toInt()
        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(activity, adWidth)
    }

    // ==========================================
    // 2. INTERSTITIAL ADS
    // ==========================================

    fun preloadInterstitialAd(context: Context) {
        if (interstitialAd != null) return

        val adRequest = AdRequest.Builder().build()
        val adUnitId = getAdUnitId(context, R.string.interstitial_ad_unit_id, TEST_INTERSTITIAL_ID)

        InterstitialAd.load(
            context,
            adUnitId,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    analyticsManager.logAdStatus("interstitial", "loaded")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                    analyticsManager.logAdStatus("interstitial", "failed", error.code)
                }
            }
        )
    }

    fun showInterstitialAd(activity: Activity, onAdClosed: () -> Unit) {
        val currentTime = System.currentTimeMillis()
        if (interstitialAd != null && (currentTime - lastInterstitialShowTime >= MIN_INTERSTITIAL_INTERVAL_MS)) {
            interstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    interstitialAd = null
                    lastInterstitialShowTime = System.currentTimeMillis()
                    preloadInterstitialAd(activity)
                    analyticsManager.logAdStatus("interstitial", "dismissed")
                    onAdClosed()
                }

                override fun onAdFailedToShowFullScreenContent(error: AdError) {
                    interstitialAd = null
                    preloadInterstitialAd(activity)
                    analyticsManager.logAdStatus("interstitial", "show_failed", error.code)
                    onAdClosed()
                }

                override fun onAdShowedFullScreenContent() {
                    analyticsManager.logAdStatus("interstitial", "displayed")
                }
            }
            interstitialAd?.show(activity)
        } else {
            preloadInterstitialAd(activity)
            onAdClosed()
        }
    }

    // ==========================================
    // 3. REWARDED ADS
    // ==========================================

    fun preloadRewardedAd(context: Context) {
        if (rewardedAd != null) return

        val adRequest = AdRequest.Builder().build()
        val adUnitId = getAdUnitId(context, R.string.rewarded_ad_unit_id, TEST_REWARDED_ID)

        RewardedAd.load(
            context,
            adUnitId,
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    analyticsManager.logAdStatus("rewarded", "loaded")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    rewardedAd = null
                    analyticsManager.logAdStatus("rewarded", "failed", error.code)
                }
            }
        )
    }

    fun isRewardedAdLoaded(): Boolean = rewardedAd != null

    fun showRewardedAd(
        activity: Activity,
        onUserEarnedReward: () -> Unit,
        onAdClosed: () -> Unit
    ) {
        if (rewardedAd != null) {
            var rewardEarned = false
            rewardedAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    rewardedAd = null
                    preloadRewardedAd(activity)
                    analyticsManager.logAdStatus("rewarded", "dismissed")
                    if (rewardEarned) {
                        onUserEarnedReward()
                    }
                    onAdClosed()
                }

                override fun onAdFailedToShowFullScreenContent(error: AdError) {
                    rewardedAd = null
                    preloadRewardedAd(activity)
                    analyticsManager.logAdStatus("rewarded", "show_failed", error.code)
                    onAdClosed()
                }

                override fun onAdShowedFullScreenContent() {
                    analyticsManager.logAdStatus("rewarded", "displayed")
                }
            }

            rewardedAd?.show(activity) { _ ->
                rewardEarned = true
                analyticsManager.logAdStatus("rewarded", "earned_reward")
            }
        } else {
            preloadRewardedAd(activity)
            onAdClosed()
        }
    }

    // ==========================================
    // 4. REWARDED INTERSTITIAL ADS
    // ==========================================

    fun preloadRewardedInterstitialAd(context: Context) {
        if (rewardedInterstitialAd != null) return

        val adRequest = AdRequest.Builder().build()
        val adUnitId = getAdUnitId(context, R.string.rewarded_interstitial_ad_unit_id, TEST_REWARDED_INTERSTITIAL_ID)

        RewardedInterstitialAd.load(
            context,
            adUnitId,
            adRequest,
            object : RewardedInterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedInterstitialAd) {
                    rewardedInterstitialAd = ad
                    analyticsManager.logAdStatus("rewarded_interstitial", "loaded")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    rewardedInterstitialAd = null
                    analyticsManager.logAdStatus("rewarded_interstitial", "failed", error.code)
                }
            }
        )
    }

    fun isRewardedInterstitialAdLoaded(): Boolean = rewardedInterstitialAd != null

    fun showRewardedInterstitialAd(
        activity: Activity,
        onUserEarnedReward: () -> Unit,
        onAdClosed: () -> Unit
    ) {
        if (rewardedInterstitialAd != null) {
            var rewardEarned = false
            rewardedInterstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    rewardedInterstitialAd = null
                    preloadRewardedInterstitialAd(activity)
                    analyticsManager.logAdStatus("rewarded_interstitial", "dismissed")
                    if (rewardEarned) {
                        onUserEarnedReward()
                    }
                    onAdClosed()
                }

                override fun onAdFailedToShowFullScreenContent(error: AdError) {
                    rewardedInterstitialAd = null
                    preloadRewardedInterstitialAd(activity)
                    analyticsManager.logAdStatus("rewarded_interstitial", "show_failed", error.code)
                    onAdClosed()
                }

                override fun onAdShowedFullScreenContent() {
                    analyticsManager.logAdStatus("rewarded_interstitial", "displayed")
                }
            }

            rewardedInterstitialAd?.show(activity) { _ ->
                rewardEarned = true
                analyticsManager.logAdStatus("rewarded_interstitial", "earned_reward")
            }
        } else {
            preloadRewardedInterstitialAd(activity)
            onAdClosed()
        }
    }
}
