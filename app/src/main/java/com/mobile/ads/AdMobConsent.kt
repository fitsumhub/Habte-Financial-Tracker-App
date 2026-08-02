package com.mobile.ads

import android.app.Activity
import android.util.Log
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform

/**
 * Google's User Messaging Platform (UMP) — gathers GDPR/CCPA-style ad consent before any
 * ad is requested. This isn't optional polish: AdMob policy expects every publisher to run
 * this before serving ads, and skipping it risks ad serving being restricted at review, not
 * just a compliance gap for EEA/UK users specifically. Must be given the chance to resolve
 * before MobileAds.initialize() runs (see AdMobService.initialize).
 */
object AdMobConsent {
    private const val TAG = "AdMobConsent"

    /**
     * Requests/updates consent info and shows Google's consent form if one is required for
     * this user — outside regions that require it, [ConsentInformation] resolves this
     * near-instantly with nothing to show. [onComplete] always fires exactly once, whether
     * or not a form was shown or the request failed (e.g. offline), so ad initialization is
     * never blocked on it — consistent with every other ad path in this app degrading to
     * "no ad" rather than stalling anything.
     */
    fun gatherConsent(activity: Activity, onComplete: () -> Unit) {
        val params = ConsentRequestParameters.Builder().build()
        val consentInformation = UserMessagingPlatform.getConsentInformation(activity)

        consentInformation.requestConsentInfoUpdate(
            activity,
            params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
                    if (formError != null) {
                        Log.w(TAG, "Consent form error (${formError.errorCode}): ${formError.message}")
                    }
                    onComplete()
                }
            },
            { requestError ->
                Log.w(TAG, "Consent info update failed (${requestError.errorCode}): ${requestError.message}")
                onComplete()
            }
        )
    }
}
