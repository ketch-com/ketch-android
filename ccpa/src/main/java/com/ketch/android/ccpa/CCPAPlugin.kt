package com.ketch.android.ccpa

import android.util.Log
import com.ketch.android.api.response.Consent
import com.ketch.android.plugin.Plugin

/**
 * Represents CCPA Plugin for encoding the user consent into CCPA String based on the [] protocol.
 * @param listener - listener returns the encoded CCPA string and applied values
 *
 *     val preferenceService = PreferenceService(this)
 *
 *     val ccpaPlugin = CCPAPlugin { encodedString, applied ->
 *         preferenceService.saveUSPrivacyString(encodedString, applied)
 *     }.apply {
 *         notice = true
 *         lspa = true
 *     }
 *
 *     ketch.addPlugin(ccpaPlugin)
 */
class CCPAPlugin(listener: (encodedString: String?, applied: Boolean) -> Unit) : Plugin(listener) {

    /**
     * Status of notice
     */
    var notice: Boolean = false

    /**
     * Status of LSPA
     */
    var lspa: Boolean = false

    /**
     * Returns true if the configuration contains a regulation for this plugin
     */
    override fun isApplied(): Boolean =
        configuration?.regulations?.contains(CCPACA) == true

    /**
     * Method consentChanged. The Ketch calls this method when the consent has been changed
     * @param consent - the current consent
     */
    override fun consentChanged(consent: Consent) {
        if (configuration == null) {
            Log.w(TAG, "Configuration is not loaded")
            Log.i(TAG, "CCPA: ${CCPAString.DEFAULT_STRING}")
            listener.invoke(CCPAString.DEFAULT_STRING, false)
            return
        }

        val applied = isApplied()

        if (!applied) {
            Log.d(TAG, "CCPA is not applied")
            Log.i(TAG, "CCPA: ${CCPAString.DEFAULT_STRING}")
            listener.invoke(CCPAString.DEFAULT_STRING, applied)
            return
        }

        val encodedString = CCPAString.encode(configuration!!, consent, notice, lspa)
        Log.i(TAG, "CCPA: $encodedString")
        listener.invoke(encodedString, applied)
    }

    override fun hashCode(): Int {
        return CCPACA.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return CCPACA.equals(other)
    }

    companion object {
        private val TAG = CCPAPlugin::class.java.simpleName
        private const val CCPACA = "ccpaca"
    }
}
