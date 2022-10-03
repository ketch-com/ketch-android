package com.ketch.android.tcf

import android.util.Log
import com.iabtcf.encoder.TCStringEncoder
import com.ketch.android.api.response.Consent
import com.ketch.android.api.response.GvlVendors
import com.ketch.android.api.response.Result
import com.ketch.android.plugin.Plugin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.time.Instant

/**
 * Represents TCF Plugin for encoding the user consent into TCF String based on the [] protocol.
 * @param listener - listener returns the encoded TCF string and applied values
 *
 *     val preferenceService = PreferenceService(this)
 *
 *     val tcfPlugin = TCFPlugin { encodedString, applied ->
 *         preferenceService.saveTCFTCString(encodedString, applied)
 *     }
 *
 *     ketch.addPlugin(tcfPlugin)
 *
 */
class TCFPlugin(listener: (encodedString: String?, applied: Boolean) -> Unit) : Plugin(listener) {
    // CPhBZtcPhBZtcACABAENCACgAIAAAAAAAAAAH2QAYH0AfYB9kAGB9YH2AAAA.YAAAAAAAAAAA
    // TCStringV2 [getVersion()=2, getCreated()=2022-10-17T11:24:26.100Z, getLastUpdated()=2022-10-17T11:24:26.100Z, getCmpId()=2, getCmpVersion()=1, getConsentScreen()=0, getConsentLanguage()=EN, getVendorListVersion()=128, getTcfPolicyVersion()=2, isServiceSpecific()=true, getUseNonStandardStacks()=false, getSpecialFeatureOptIns()={}, getPurposesConsent()={1}, getPurposesLITransparency()={}, getPurposeOneTreatment()=false, getPublisherCC()=AA, getVendorConsent()={1000, 1001, 1002, 1003, 1004}, getVendorLegitimateInterest()={1003, 1004}, getPublisherRestrictions()=[], getDisclosedVendors()={}, getAllowedVendors()={}, getPubPurposesConsent()={}, getPubPurposesLITransparency()={}, getCustomPurposesConsent()={}, getCustomPurposesLITransparency()={}]

    private val vendors = MutableStateFlow<GvlVendors?>(null)

    private val scope = CoroutineScope(Dispatchers.Default)

    private val vendorsUseCase: VendorsUseCase by lazy {
        VendorsUseCase()
    }

    init {
        // load vendors
        loadVendors()
    }

    // Gets the GVL Vendors.
    private fun loadVendors() {
        scope.launch {
            vendorsUseCase.getVendors().collect {
                when (it) {
                    is Result.Success -> {
                        Log.d(TAG, "getVendors(): success")
                        vendors.value = it.data
                    }
                    is Result.Error -> {
                        Log.d(TAG, "getVendors(): failed")
                    }
                    else -> {}
                }
            }
        }
    }

    /**
     * Returns true if the configuration contains a regulation for this plugin
     */
    override fun isApplied(): Boolean =
        configuration?.regulations?.contains(GDPREU) == true

    /**
     * Method consentChanged. The Ketch calls this method when the consent has been changed
     * @param consent - the current consent
     */
    override fun consentChanged(consent: Consent) {
        if (configuration == null) {
            Log.w(TAG, "Configuration is not loaded")
            listener.invoke(null, false)
            return
        }

        val applied = isApplied()

        if (!applied) {
            Log.i(TAG, "TCF is not applied")
            listener.invoke(null, applied)
            return
        }

        if (vendors.value == null) {
            Log.w(TAG, "Vendors are not loaded")
            listener.invoke(null, applied)
            return
        }

        val instant = Instant.now()

        val tcfString = TCStringEncoder.newBuilder()
            .version(VERSION)
            .cmpId(CMP_ID)
            .cmpVersion(CMP_VERSION)
            .vendorListVersion(vendors.value!!.vendorListVersion)
            .created(instant)
            .lastUpdated(instant)
            .useNonStandardStacks(USE_NON_STANDART_STACKS)
            .isServiceSpecific(IS_SERVICE_SPECIFIC)
            .consentLanguage(configuration!!.language)

        val consentPurposes = consent.purposes?.filter {
            it.value.toBooleanStrictOrNull() == true
        }

        configuration?.purposes?.filter {
            it.tcfID?.isNotEmpty() == true && consentPurposes?.contains(it.code) == true
        }?.onEach {
            it.tcfID?.toIntOrNull()?.let { tcfID ->
                if (it.tcfType == TCF_PURPOSE_TYPE) {
                    if (it.legalBasisCode == CONSENT_OPTIN) {
                        tcfString.addPurposesConsent(tcfID)
                        tcfString.addPurposesLITransparency(tcfID)
                    }
                    if (it.legalBasisCode == LEGITIMATEINTEREST_OBJECTABLE) {
                        tcfString.addPurposesLITransparency(tcfID)
                    }
                }
                if (it.tcfType == TCF_SPECIAL_FEATURE_TYPE) {
                    tcfString.addSpecialFeatureOptIns(tcfID)
                }
            }
        }

        configuration?.vendors?.filter {
            consent.vendors?.contains(it.id) == true
        }?.onEach {
            it.id.toIntOrNull()?.let { id ->
                tcfString.addVendorConsent(id)
                tcfString.addVendorLegitimateInterest(id)
            }
        }

        val encodedString = tcfString.encode()

        Log.i(TAG, "TCF: $encodedString")
        listener.invoke(encodedString, applied)
    }

    override fun hashCode(): Int {
        return GDPREU.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return GDPREU.equals(other)
    }

    companion object {
        private val TAG = TCFPlugin::class.java.simpleName

        private const val GDPREU = "gdpreu"

        private const val VERSION = 2
        private const val CMP_ID = 2
        private const val CMP_VERSION = 1
        private const val USE_NON_STANDART_STACKS = false
        private const val IS_SERVICE_SPECIFIC = true

        private const val TCF_PURPOSE_TYPE = "purpose"
        private const val TCF_SPECIAL_FEATURE_TYPE = "specialFeature"

        private const val CONSENT_OPTIN = "consent_optin"
        private const val LEGITIMATEINTEREST_OBJECTABLE = "legitimateinterest_objectable"
    }
}
