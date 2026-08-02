package com.mobile.ads

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.mobile.data.SettingsRepository

/**
 * Drop-in banner ad widget. Only place this on a screen listed in
 * [AdMobConfig.BANNER_ALLOWED_ROUTES] — today that's the Dashboard (HomeScreen), Reports
 * (AnalyticsScreen), and Settings screens, and nowhere else. In particular, never add this
 * to PIN entry, add/edit transaction, transfer, or payment screens — those must stay
 * completely ad-free.
 *
 * Collapses to zero height (renders nothing) if the ad fails to load, instead of leaving
 * a blank gray placeholder box in the layout.
 */
@Composable
fun BannerAdView(modifier: Modifier = Modifier) {
    // Step 1: track load failure as Compose state, so a failed load can hide this widget
    // entirely on recomposition instead of leaving an empty ad slot in the UI.
    var loadFailed by remember { mutableStateOf(false) }

    if (loadFailed) return

    // Respect an ad-free grant earned by watching a rewarded ad (Settings screen).
    val adFreeUntil by SettingsRepository.adFreeUntilMillis.collectAsState()
    if (System.currentTimeMillis() < adFreeUntil) return

    AndroidView(
        modifier = modifier.fillMaxWidth().height(50.dp),
        factory = { ctx ->
            // Step 2: build the platform AdView once. AndroidView reuses this same
            // instance across recompositions — it does not get recreated on every frame.
            AdView(ctx).apply {
                setAdSize(AdSize.BANNER)
                adUnitId = AdMobConfig.bannerAdUnitId
                adListener = object : AdListener() {
                    override fun onAdFailedToLoad(error: LoadAdError) {
                        // Step 3: no fill / no network / etc. — never crash, just collapse
                        // the widget so it doesn't leave a dead space in the screen layout.
                        loadFailed = true
                    }
                }
                // Step 4: kick off the actual (asynchronous) ad request.
                loadAd(AdRequest.Builder().build())
            }
        },
        // Step 5: release the native AdView's resources once this composable leaves
        // composition (e.g. the user navigates away from the screen).
        onRelease = { adView -> adView.destroy() }
    )
}
