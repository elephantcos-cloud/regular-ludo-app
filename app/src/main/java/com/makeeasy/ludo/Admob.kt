package com.makeeasy.ludo

import android.app.Activity
import android.app.Application
import android.app.Application.ActivityLifecycleCallbacks
import android.os.Build
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup.OnHierarchyChangeListener
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RatingBar
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.appopen.AppOpenAd.AppOpenAdLoadCallback
import com.google.android.gms.ads.initialization.InitializationStatus
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView
import com.google.android.gms.ads.rewarded.RewardItem
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

class Admob {

    private val act: Activity
    private var rFaild = "Ad not loaded"
    private var iFaild = "Ad not loaded"
    private var mRewardedAd: RewardedAd? = null
    private var mInterstitialAd: InterstitialAd? = null

    interface BannerListener {
        fun onLoad()
        fun onClick()
        fun onFailed(reason: String?)
    }

    interface InterstitialListener {
        fun onComplete()
        fun onClick()
        fun onFailed(reason: String?)
    }

    interface RewardListener {
        fun onComplete()
        fun onClick()
        fun onFailed(reason: String?)
    }

    interface NativeListener {
        fun onLoad()
        fun onClick()
        fun onFailed(reason: String?)
    }

    constructor(act: Activity) {
        this.act = act
        MobileAds.initialize(act) { initializationStatus: InitializationStatus ->
            val statusMap = initializationStatus.adapterStatusMap
            for (adapterClass in statusMap.keys) {
                val status = checkNotNull(statusMap[adapterClass])
            }
        }
    }

    constructor(act: Activity, testMode: Boolean) {
        this.act = act
        MobileAds.initialize(act) { initializationStatus: InitializationStatus ->
            val statusMap = initializationStatus.adapterStatusMap
            for (adapterClass in statusMap.keys) {
                val status = checkNotNull(statusMap[adapterClass])
            }
            if (testMode) {
                loadInterstitial()
                loadReward()
            }
        }
    }

    fun setAdUnit(appopen: String,banner: String,interstitial: String,reward: String,rectangle: String) {
        if (appopen != "") {
            AppOpen = appopen
        }
        if (banner != "") {
            Banner = banner
        }
        if (interstitial != "") {
            Interstitial = interstitial
        }
        if (reward != "") {
            Rewarded = reward
        }
        if (rectangle != "") {
            Native = rectangle
        }
        loadInterstitial()
        loadReward()
    }

    private fun loadInterstitial() {
        InterstitialAd.load(act, Interstitial, AdRequest.Builder().build(), object : InterstitialAdLoadCallback() {
            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                super.onAdFailedToLoad(loadAdError)
                iFaild = loadAdError.message
                mInterstitialAd = null
                loadInterstitial()
            }
            override fun onAdLoaded(interstitialAd: InterstitialAd) {
                super.onAdLoaded(interstitialAd)
                mInterstitialAd = interstitialAd
            }
        })
    }

    private fun loadReward() {
        RewardedAd.load(act, Rewarded,AdRequest.Builder().build(),object : RewardedAdLoadCallback() {
            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                super.onAdFailedToLoad(loadAdError)
                rFaild = loadAdError.message
                mRewardedAd = null
                loadReward()
            }
            override fun onAdLoaded(rewardedAd: RewardedAd) {
                super.onAdLoaded(rewardedAd)
                mRewardedAd = rewardedAd
            }
        })
    }

    fun showInterstitial() {
        if (mInterstitialAd != null) {
            mInterstitialAd!!.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    super.onAdDismissedFullScreenContent()
                    mInterstitialAd = null
                    loadInterstitial()
                }
                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    super.onAdFailedToShowFullScreenContent(adError)
                    loadInterstitial()
                }
            }
            mInterstitialAd!!.show(act)
        }
    }

    fun showInterstitial(interstitialListener: InterstitialListener) {
        if (mInterstitialAd != null) {
            mInterstitialAd!!.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdClicked() {
                    interstitialListener.onClick()
                    super.onAdClicked()
                }
                override fun onAdDismissedFullScreenContent() {
                    super.onAdDismissedFullScreenContent()
                    mInterstitialAd = null
                    loadInterstitial()
                    interstitialListener.onComplete()
                }
                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    super.onAdFailedToShowFullScreenContent(adError)
                    loadInterstitial()
                    interstitialListener.onFailed(adError.message)
                }
            }
            mInterstitialAd!!.show(act)
        } else {
            interstitialListener.onFailed(iFaild)
        }
    }

    fun showRewarded() {
        if (mRewardedAd != null) {
            mRewardedAd!!.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    super.onAdDismissedFullScreenContent()
                    mRewardedAd = null
                    loadReward()
                }
                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    super.onAdFailedToShowFullScreenContent(adError)
                    loadReward()
                }
            }
            mRewardedAd!!.show(act) { rewardItem: RewardItem? -> }
        }
    }

    fun showRewarded(rewardListener: RewardListener) {
        if (mRewardedAd != null) {
            mRewardedAd!!.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdClicked() {
                    rewardListener.onClick()
                    super.onAdClicked()
                }
                override fun onAdDismissedFullScreenContent() {
                    super.onAdDismissedFullScreenContent()
                    mRewardedAd = null
                    loadReward()
                    rewardListener.onComplete()
                }
                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    super.onAdFailedToShowFullScreenContent(adError)
                    loadReward()
                    rewardListener.onFailed(adError.message)
                }
            }
            mRewardedAd!!.show(act) { rewardItem: RewardItem? -> }
        } else {
            rewardListener.onFailed(rFaild)
        }
    }

    fun loadNative(view: FrameLayout) {
        val v = LayoutInflater.from(view.context).inflate(R.layout.native_ad, null)
        val nativeAdView = v.findViewById<NativeAdView>(R.id.native_ad_view)
        val adLoader = AdLoader.Builder(act, Native).forNativeAd { nativeAd: NativeAd ->
            view.addView(v)
            populateNativeAdView(nativeAd, nativeAdView)
        }.withNativeAdOptions(NativeAdOptions.Builder().build()).build()
        adLoader.loadAd(AdRequest.Builder().build())
    }

    fun loadNative(view: FrameLayout, nativeListener: NativeListener) {
        val v = LayoutInflater.from(view.context).inflate(R.layout.native_ad, null)
        val nativeAdView = v.findViewById<NativeAdView>(R.id.native_ad_view)
        val adLoader = AdLoader.Builder(act, Native).forNativeAd { nativeAd: NativeAd ->
            view.addView(v)
            populateNativeAdView(nativeAd, nativeAdView)
        }.withAdListener(object : AdListener() {
            override fun onAdClicked() {
                super.onAdClicked()
                nativeListener.onClick()
            }
            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                super.onAdFailedToLoad(loadAdError)
                nativeListener.onFailed(loadAdError.message)
            }
            override fun onAdLoaded() {
                super.onAdLoaded()
                nativeListener.onLoad()
            }
        }).withNativeAdOptions(NativeAdOptions.Builder().build()).build()
        adLoader.loadAd(AdRequest.Builder().build())
    }

    fun loadBanner(view: LinearLayout, inlineAdaptive: Boolean) {
        val adView = AdView(act)
        view.addView(adView)
        adView.adUnitId = Banner
        adView.setAdSize(getAdSize(inlineAdaptive))
        adView.loadAd(AdRequest.Builder().build())
    }

    fun loadBanner(view: FrameLayout, inlineAdaptive: Boolean) {
        val adView = AdView(act)
        view.addView(adView)
        adView.adUnitId = Banner
        adView.setAdSize(getAdSize(inlineAdaptive))
        adView.loadAd(AdRequest.Builder().build())
    }

    fun loadBanner(view: LinearLayout, inlineAdaptive: Boolean, bannerListener: BannerListener) {
        val adView = AdView(act)
        view.addView(adView)
        adView.adUnitId = Banner
        adView.setAdSize(getAdSize(inlineAdaptive))
        adView.adListener = object : AdListener() {
            override fun onAdClicked() {
                bannerListener.onClick()
                super.onAdClicked()
            }
            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                bannerListener.onFailed(loadAdError.message)
                super.onAdFailedToLoad(loadAdError)
            }
            override fun onAdLoaded() {
                bannerListener.onLoad()
                super.onAdLoaded()
            }
        }
        adView.loadAd(AdRequest.Builder().build())
    }

    fun loadBanner(view: FrameLayout, inlineAdaptive: Boolean, bannerListener: BannerListener) {
        val adView = AdView(act)
        view.addView(adView)
        adView.adUnitId = Banner
        adView.setAdSize(getAdSize(inlineAdaptive))
        adView.adListener = object : AdListener() {
            override fun onAdClicked() {
                bannerListener.onClick()
                super.onAdClicked()
            }
            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                bannerListener.onFailed(loadAdError.message)
                super.onAdFailedToLoad(loadAdError)
            }
            override fun onAdLoaded() {
                bannerListener.onLoad()
                super.onAdLoaded()
            }
        }
        adView.loadAd(AdRequest.Builder().build())
    }

    private fun getAdSize(inlineAdaptive: Boolean): AdSize {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val windowMetrics = act.windowManager.currentWindowMetrics
            val bounds = windowMetrics.bounds
            val adWidthPixels = bounds.width().toFloat()
            val density = act.resources.displayMetrics.density
            val adWidth = (adWidthPixels / density).toInt()
            return if (inlineAdaptive) {
                AdSize.getCurrentOrientationInlineAdaptiveBannerAdSize(act, adWidth)
            } else {
                AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(act, adWidth)
            }
        } else {
            val displayMetrics = DisplayMetrics()
            act.windowManager.defaultDisplay.getMetrics(displayMetrics)
            val widthPixels = displayMetrics.widthPixels.toFloat()
            val density = displayMetrics.density
            val adWidth = (widthPixels / density).toInt()
            return if (inlineAdaptive) {
                AdSize.getCurrentOrientationInlineAdaptiveBannerAdSize(act, adWidth)
            } else {
                AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(act, adWidth)
            }
        }
    }

    private fun populateNativeAdView(nativeAd: NativeAd, nativeAdView: NativeAdView) {
        nativeAdView.mediaView = nativeAdView.findViewById(R.id.media_view)
        nativeAdView.callToActionView = nativeAdView.findViewById(R.id.cta)
        nativeAdView.headlineView = nativeAdView.findViewById(R.id.primary)
        nativeAdView.advertiserView = nativeAdView.findViewById(R.id.secondary)
        nativeAdView.starRatingView = nativeAdView.findViewById(R.id.rating_bar)
        nativeAdView.bodyView = nativeAdView.findViewById(R.id.body)
        nativeAdView.iconView = nativeAdView.findViewById(R.id.icon)
        (nativeAdView.headlineView as AppCompatTextView).text = nativeAd.headline
        (nativeAdView.bodyView as AppCompatTextView).text = nativeAd.body
        (nativeAdView.callToActionView as AppCompatButton).text = nativeAd.callToAction
        nativeAdView.mediaView!!.mediaContent = nativeAd.mediaContent
        nativeAdView.mediaView!!.setOnHierarchyChangeListener(object : OnHierarchyChangeListener {
            override fun onChildViewAdded(parent: View, child: View) {
                if (child is ImageView) {
                    child.adjustViewBounds = true
                    child.scaleType = ImageView.ScaleType.FIT_XY
                }
            }
            override fun onChildViewRemoved(parent: View, child: View) {}
        })
        if (nativeAd.icon == null) {
            nativeAdView.iconView!!.visibility = View.INVISIBLE
        } else {
            (nativeAdView.iconView as AppCompatImageView).setImageDrawable(nativeAd.icon!!.drawable)
            nativeAdView.iconView!!.visibility = View.VISIBLE
        }
        if (nativeAd.starRating == null) {
            nativeAdView.starRatingView!!.visibility = View.INVISIBLE
        } else {
            (nativeAdView.starRatingView as RatingBar).rating = nativeAd.starRating!!.toFloat()
            nativeAdView.starRatingView!!.visibility = View.VISIBLE
        }
        if (nativeAd.advertiser == null) {
            nativeAdView.advertiserView!!.visibility = View.INVISIBLE
        } else {
            (nativeAdView.advertiserView as AppCompatTextView).text = nativeAd.advertiser
            nativeAdView.advertiserView!!.visibility = View.VISIBLE
        }
        nativeAdView.setNativeAd(nativeAd)
    }

    class AOManager(private val application: Application) : ActivityLifecycleCallbacks, DefaultLifecycleObserver {
        private var isShowingAd = false
        private var currentActivity: Activity? = null
        private var appOpenAd: AppOpenAd? = null

        init {
            application.registerActivityLifecycleCallbacks(this)
            ProcessLifecycleOwner.get().lifecycle.addObserver(this)
            fetchAd()
        }

        override fun onStart(owner: LifecycleOwner) {
            super.onStart(owner)
            showAdIfAvailable()
        }

        private fun showAdIfAvailable() {
            if (!isShowingAd && appOpenAd != null) {
                appOpenAd!!.fullScreenContentCallback = object : FullScreenContentCallback() {
                    override fun onAdDismissedFullScreenContent() {
                        appOpenAd = null
                        isShowingAd = false
                        fetchAd()
                    }
                    override fun onAdShowedFullScreenContent() {
                        isShowingAd = true
                    }
                }
                appOpenAd!!.show(currentActivity!!)
            } else {
                fetchAd()
            }
        }

        private fun fetchAd() {
            val adRequest = AdRequest.Builder().build()
            AppOpenAd.load(application, AppOpen, adRequest, object : AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    appOpenAd = ad
                }
                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    appOpenAd = null
                }
            })
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
            currentActivity = null
        }
    }

    companion object {
        var AppOpen: String = "ca-app-pub-3940256099942544/9257395921"
        var Banner: String = "ca-app-pub-3940256099942544/6300978111"
        var Interstitial: String = "ca-app-pub-3940256099942544/1033173712"
        var Rewarded: String = "ca-app-pub-3940256099942544/5224354917"
        var Native: String = "ca-app-pub-3940256099942544/2247696110"
    }
}
