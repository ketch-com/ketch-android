package com.ketch.android.ccpa

import android.util.Log
import com.ketch.android.api.response.Consent
import com.ketch.android.api.response.FullConfiguration

internal object CCPAString {
    private val TAG = CCPAString::class.java.simpleName

    private const val API_VERSION = "1"
    const val DEFAULT_STRING = "$API_VERSION---"

    private const val ANALYTICS = "analytics"
    private const val BEHAVIORAL_ADVERTISING = "behavioral_advertising"
    private const val DATA_BROKING = "data_broking"

    fun encode(
        configuration: FullConfiguration,
        consent: Consent,
        notice: Boolean = true,
        lspa: Boolean = true
    ): String {

        if (configuration.canonicalPurposes.isNullOrEmpty()) {
            Log.d(TAG, "canonicalPurposes is null or empty")
            return DEFAULT_STRING
        }

        val noticeYesNo = YesNo.fromBoolean(notice)

        val analytics = configuration.canonicalPurposes!![ANALYTICS]
        val behavioralAdvertising = configuration.canonicalPurposes!![BEHAVIORAL_ADVERTISING]
        val dataBroking = configuration.canonicalPurposes!![DATA_BROKING]

        val analyticsPurposeCodes = analytics?.purposeCodes
        val behavioralAdvertisingPurposeCodes = behavioralAdvertising?.purposeCodes
        val dataBrokingPurposeCodes = dataBroking?.purposeCodes

        val analyticsEnabled = analyticsPurposeCodes?.size == getOptOutCount(analyticsPurposeCodes, consent)
        val behavioralAdvertisingEnabled =
            behavioralAdvertisingPurposeCodes?.size == getOptOutCount(behavioralAdvertisingPurposeCodes, consent)
        val dataBrokingEnabled = dataBrokingPurposeCodes?.size == getOptOutCount(dataBrokingPurposeCodes, consent)

        val optedOut = YesNo.fromBoolean(analyticsEnabled && behavioralAdvertisingEnabled && dataBrokingEnabled)

        // we expect the user to set the LSPA variable on their page if they are using that framework for CCPA compliance
        val lspaYesNo = YesNo.fromBoolean(lspa)

        // return uspString
        // v = version (int)
        // n = Notice Given (char)
        // o = OptedOut (char)
        // l = Lspact (char)
        return "$API_VERSION${noticeYesNo}${optedOut}${lspaYesNo}"
    }

    private fun getOptOutCount(purposeCodes: List<String>?, consent: Consent): Int =
        purposeCodes?.filter {
            consent.purposes?.get(it)?.toBoolean() == true
        }?.size ?: 0

    private enum class YesNo(private val char: Char) {
        YES('Y'),
        NO('N');

        override fun toString(): String {
            return this.char.toString()
        }

        companion object {
            fun fromBoolean(value: Boolean) =
                if (value) {
                    YES
                } else {
                    NO
                }
        }
    }
}
