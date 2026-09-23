package com.app.taptap.ads

import android.app.Activity
import android.content.Context
import android.view.ViewGroup
import android.widget.FrameLayout
import com.app.taptap.R
import com.app.taptap.analytics.AnalyticsManager
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAdLoadCallback
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

    /**
     * Initializes Google Mobile Ads SDK.
     */
    fun initialize(context: Context) {
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
        adView.adUnitId = activity.getString(R.string.banner_ad_unit_id)
        
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
        val adUnitId = context.getString(R.string.interstitial_ad_unit_id)

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
        val adUnitId = context.getString(R.string.rewarded_ad_unit_id)

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

            rewardedAd?.show(activity) { rewardItem ->
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
        val adUnitId = context.getString(R.string.rewarded_interstitial_ad_unit_id)

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

            rewardedInterstitialAd?.show(activity) { rewardItem ->
                rewardEarned = true
                analyticsManager.logAdStatus("rewarded_interstitial", "earned_reward")
            }
        } else {
            preloadRewardedInterstitialAd(activity)
            onAdClosed()
        }
    }
}
