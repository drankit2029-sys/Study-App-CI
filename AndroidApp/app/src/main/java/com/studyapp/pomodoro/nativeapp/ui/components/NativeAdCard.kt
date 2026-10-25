package com.studyapp.pomodoro.nativeapp.ui.components

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView
import com.studyapp.pomodoro.nativeapp.R
import com.studyapp.pomodoro.nativeapp.ui.theme.Paper
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private const val TEST_NATIVE_AD_UNIT_ID = "ca-app-pub-3940256099942544/2247696110"
private const val NATIVE_AD_DESCRIPTION = "Sponsored native ad"
private const val LOG_TAG = "NativeAdCard"
private const val LOAD_ATTEMPTS = 3
private const val LOAD_TIMEOUT_MILLIS = 8_000L

@Composable
fun NativeAdCard(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var nativeAd by remember { mutableStateOf<NativeAd?>(null) }

    LaunchedEffect(Unit) {
        repeat(LOAD_ATTEMPTS) { attempt ->
            val loaded = loadNativeAd(context)
            if (loaded != null) {
                nativeAd = loaded
                return@LaunchedEffect
            }
            if (attempt < LOAD_ATTEMPTS - 1) {
                delay(900L * (attempt + 1))
            }
        }
    }
    DisposableEffect(Unit) {
        onDispose { nativeAd?.destroy() }
    }

    val ad = nativeAd ?: return
    key(ad) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .softRaised(Paper, 24.dp, 7.dp)
                .semantics { contentDescription = NATIVE_AD_DESCRIPTION }
                .padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            AndroidView(
                modifier = Modifier.fillMaxWidth(),
                factory = { viewContext ->
                    val adView = LayoutInflater.from(viewContext)
                        .inflate(R.layout.native_ad_card, null, false) as NativeAdView
                    bindNativeAd(adView, ad)
                    adView
                },
            )
        }
    }
}

private suspend fun loadNativeAd(context: Context): NativeAd? {
    return runCatching {
        withTimeout(LOAD_TIMEOUT_MILLIS) {
            suspendCancellableCoroutine { continuation ->
                val adLoader = AdLoader.Builder(context, TEST_NATIVE_AD_UNIT_ID)
                    .forNativeAd { ad ->
                        if (continuation.isActive) {
                            continuation.resume(ad)
                        } else {
                            ad.destroy()
                        }
                    }
                    .withAdListener(object : AdListener() {
                        override fun onAdFailedToLoad(error: LoadAdError) {
                            if (continuation.isActive) {
                                continuation.resumeWithException(
                                    IllegalStateException(error.message),
                                )
                            }
                        }
                    })
                    .withNativeAdOptions(
                        NativeAdOptions.Builder()
                            .setAdChoicesPlacement(NativeAdOptions.ADCHOICES_TOP_RIGHT)
                            .setMediaAspectRatio(
                                NativeAdOptions.NATIVE_MEDIA_ASPECT_RATIO_ANY,
                            )
                            .build(),
                    )
                    .build()
                adLoader.loadAd(AdRequest.Builder().build())
            }
        }
    }.onFailure { error ->
        Log.w(LOG_TAG, "Native ad request failed: ${error.message}")
    }.getOrNull()
}

private fun bindNativeAd(adView: NativeAdView, ad: NativeAd) {
    val headlineView = adView.findViewById<TextView>(R.id.ad_headline)
    val bodyView = adView.findViewById<TextView>(R.id.ad_body)
    val advertiserView = adView.findViewById<TextView>(R.id.ad_advertiser)
    val iconView = adView.findViewById<ImageView>(R.id.ad_app_icon)
    val mediaView = adView.findViewById<MediaView>(R.id.ad_media)
    val callToActionView = adView.findViewById<Button>(R.id.ad_call_to_action)

    headlineView.text = ad.headline
    adView.headlineView = headlineView

    val body = ad.body
    if (body.isNullOrBlank()) {
        bodyView.visibility = View.GONE
    } else {
        bodyView.text = body
        bodyView.visibility = View.VISIBLE
        adView.bodyView = bodyView
    }

    val attribution = listOfNotNull(
        ad.advertiser?.takeIf { it.isNotBlank() },
        ad.store?.takeIf { it.isNotBlank() },
        ad.price?.takeIf { it.isNotBlank() },
    ).joinToString(" · ")
    if (attribution.isBlank()) {
        advertiserView.visibility = View.GONE
    } else {
        advertiserView.text = attribution
        advertiserView.visibility = View.VISIBLE
        adView.advertiserView = advertiserView
    }

    val icon = ad.icon?.drawable
    if (icon == null) {
        iconView.visibility = View.GONE
    } else {
        iconView.setImageDrawable(icon)
        iconView.visibility = View.VISIBLE
        adView.iconView = iconView
    }

    val mediaContent = ad.mediaContent
    if (mediaContent == null) {
        mediaView.visibility = View.GONE
    } else {
        mediaView.setImageScaleType(ImageView.ScaleType.CENTER_CROP)
        mediaView.mediaContent = mediaContent
        mediaView.visibility = View.VISIBLE
        adView.mediaView = mediaView
    }

    val callToAction = ad.callToAction
    if (callToAction.isNullOrBlank()) {
        callToActionView.visibility = View.GONE
    } else {
        callToActionView.text = callToAction
        callToActionView.visibility = View.VISIBLE
        adView.callToActionView = callToActionView
    }

    adView.setNativeAd(ad)
}
