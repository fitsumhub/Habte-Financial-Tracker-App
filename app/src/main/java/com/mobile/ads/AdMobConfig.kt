package com.mobile.ads

/**
 * Central configuration for every AdMob ad unit used in the app, plus the rules for
 * where each ad format is (and isn't) allowed to appear. Nothing outside this file
 * should hardcode an ad unit ID — that keeps the swap to production IDs a one-file change.
 *
 * IMPORTANT — these are all Google's own published TEST ad unit IDs (see
 * https://developers.google.com/admob/android/test-ads). They always serve a clearly
 * labeled test creative and are safe to ship in a development build, but they earn no
 * real revenue, and shipping them to the Play Store is against AdMob policy.
 *
 * Pre-launch checklist (do all of these together, not one at a time):
 * 1. Create the app + ad units in the AdMob console (banner, interstitial, rewarded,
 *    native — only create the formats actually used, see AdMobService).
 * 2. Replace the 4 TEST_*_AD_UNIT_ID values below with the real ones.
 * 3. Flip `USE_TEST_ADS` to `false` — the `TODO()` calls above are a deliberate guardrail:
 *    if a real ID was missed, the app crashes loudly in testing instead of silently
 *    shipping a test ID (or a blank one) to production.
 * 4. Replace the `com.google.android.gms.ads.APPLICATION_ID` meta-data value in
 *    AndroidManifest.xml with the app's real AdMob App ID (also from the console).
 * 5. In the AdMob console's own "App content" / "Privacy & messaging" section, publish a
 *    GDPR/US consent message — AdMobConsent (see AdMobService.initialize) only shows
 *    whatever message is configured there; there's nothing further to change in code for
 *    that step, but ads silently won't request-with-consent correctly if no message has
 *    ever been published there.
 * 6. Double check the Play Console's "Ads" and "Target audience" declarations agree with
 *    `RequestConfiguration` in AdMobService (currently: general audience, not
 *    child-directed).
 */
object AdMobConfig {
    // ── Test ad unit IDs (Google's official constants — same for every developer) ──────
    const val TEST_BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"
    const val TEST_INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"
    const val TEST_REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"
    const val TEST_NATIVE_ADVANCED_AD_UNIT_ID = "ca-app-pub-3940256099942544/2247696110"

    // The app is still in development, so every ad surface points at the test IDs above.
    // Flip this to false (and fill in real IDs) as the very last step before a store release.
    private const val USE_TEST_ADS = true

    val bannerAdUnitId: String get() = if (USE_TEST_ADS) TEST_BANNER_AD_UNIT_ID else TODO("Set production banner ad unit ID")
    val interstitialAdUnitId: String get() = if (USE_TEST_ADS) TEST_INTERSTITIAL_AD_UNIT_ID else TODO("Set production interstitial ad unit ID")
    val rewardedAdUnitId: String get() = if (USE_TEST_ADS) TEST_REWARDED_AD_UNIT_ID else TODO("Set production rewarded ad unit ID")
    val nativeAdUnitId: String get() = if (USE_TEST_ADS) TEST_NATIVE_ADVANCED_AD_UNIT_ID else TODO("Set production native ad unit ID")

    /**
     * Screens allowed to show a banner ad. Everything not listed here — including every
     * money-movement or authentication screen (PIN entry, add/edit transaction, transfers,
     * payments) — must never call BannerAdView, full stop. Route names match the ones
     * used in AppNavigation.
     */
    val BANNER_ALLOWED_ROUTES: Set<String> = setOf("home", "analytics", "settings")

    /** True if [route] is one of the few screens allowed to render a banner ad. */
    fun isBannerAllowedOn(route: String): Boolean = route in BANNER_ALLOWED_ROUTES
}
