package com.ketch.android.plugin

import com.ketch.android.api.response.Consent
import com.ketch.android.api.response.FullConfiguration

/**
Abstract class Plugin
@param listener - encoded string listener

class CustomPlugin(listener: (encodedString: String?, applied: Boolean) -> Unit) : Plugin(listener) {

    override fun isApplied(): Boolean =
        configuration?.regulations?.contains(REGULATION) == true

    override fun consentChanged(consent: Consent) {
        ...
        listener.invoke(encodedString, applied)
    }

    override fun hashCode(): Int {
        return REGULATION.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return REGULATION.equals(other)
    }

    companion object {
        private const val REGULATION = "<regulation code>"
    }
}
**/
abstract class Plugin(protected val listener: (encodedString: String?, applied: Boolean) -> Unit) {
    protected var configuration: FullConfiguration? = null

    /**
     * Returns true if the configuration contains a regulation for this plugin
     */
    abstract fun isApplied(): Boolean

    /**
     * Method configLoaded. The Ketch calls this method when the full configuration has been loaded
     * @param configuration - the full configuration
     */
    fun configLoaded(configuration: FullConfiguration) {
        this.configuration = configuration
    }

    /**
     * Method consentChanged. The Ketch calls this method when the consent has been changed
     * @param consent - the current consent
     */
    abstract fun consentChanged(consent: Consent)
}