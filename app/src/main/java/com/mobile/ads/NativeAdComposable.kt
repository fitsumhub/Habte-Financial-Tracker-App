package com.mobile.ads

import android.content.Context
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView

/**
 * Reusable native ad slot. AdMob requires a native ad's assets (headline, body, icon,
 * media, call-to-action) to be hosted inside a real [NativeAdView] — that's what actually
 * registers impressions/clicks with the SDK, a plain Compose layout can't render a
 * NativeAd's content directly. This builds that view hierarchy in code (no extra XML
 * layout resource needed) and keeps it in sync with the loaded ad.
 *
 * Not wired into a specific screen yet — the feature request only specified placement
 * rules for banner ads. Use this wherever a feed-style native placement is wanted, subject
 * to the same rule as everywhere else: never on a financial-action or authentication screen.
 */
@Composable
fun NativeAdComposable(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var nativeAd by remember { mutableStateOf<NativeAd?>(null) }

    // Load once when this enters composition; always destroy the ad when it leaves —
    // NativeAd holds native (non-GC'd) resources that must be released explicitly.
    DisposableEffect(Unit) {
        AdMobService.loadNativeAd(context = context, onLoaded = { ad -> nativeAd = ad })
        onDispose { nativeAd?.destroy() }
    }

    val ad = nativeAd ?: return

    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { ctx -> buildNativeAdView(ctx) },
        update = { adView -> bindNativeAd(adView, ad) }
    )
}

/** Builds the (empty) native ad view hierarchy once: icon + headline/body, media, call-to-action. */
private fun buildNativeAdView(context: Context): NativeAdView {
    val density = context.resources.displayMetrics.density
    fun dp(value: Int) = (value * density).toInt()

    val headline = TextView(context).apply {
        textSize = 16f
        setTypeface(typeface, Typeface.BOLD)
    }
    val body = TextView(context).apply { textSize = 13f }
    val icon = ImageView(context).apply {
        layoutParams = LinearLayout.LayoutParams(dp(40), dp(40)).apply { marginEnd = dp(12) }
    }
    val callToAction = Button(context)
    val media = MediaView(context)

    val textColumn = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        addView(headline)
        addView(body)
    }
    val header = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(12), dp(12), dp(12), dp(8))
        addView(icon)
        addView(textColumn)
    }
    val root = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        addView(header)
        addView(media)
        addView(callToAction, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { setMargins(dp(12), dp(8), dp(12), dp(12)) })
    }

    return NativeAdView(context).apply {
        addView(root)
        headlineView = headline
        bodyView = body
        iconView = icon
        mediaView = media
        callToActionView = callToAction
    }
}

/** Populates the view hierarchy from [nativeAd]'s assets, then registers it with the SDK. */
private fun bindNativeAd(adView: NativeAdView, nativeAd: NativeAd) {
    (adView.headlineView as? TextView)?.text = nativeAd.headline

    (adView.bodyView as? TextView)?.let {
        it.text = nativeAd.body
        it.visibility = if (nativeAd.body.isNullOrBlank()) View.GONE else View.VISIBLE
    }

    (adView.iconView as? ImageView)?.let { iconView ->
        val icon = nativeAd.icon
        if (icon != null) {
            iconView.setImageDrawable(icon.drawable)
            iconView.visibility = View.VISIBLE
        } else {
            iconView.visibility = View.GONE
        }
    }

    (adView.callToActionView as? Button)?.let {
        it.text = nativeAd.callToAction
        it.visibility = if (nativeAd.callToAction.isNullOrBlank()) View.GONE else View.VISIBLE
    }

    adView.mediaView?.mediaContent = nativeAd.mediaContent

    // This call is what actually registers the ad with the SDK for impression/click
    // tracking — everything above just populates the visuals.
    adView.setNativeAd(nativeAd)
}
