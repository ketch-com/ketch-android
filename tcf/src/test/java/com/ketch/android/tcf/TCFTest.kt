package com.ketch.android.tcf

import com.iabtcf.decoder.TCString
import com.iabtcf.encoder.TCStringEncoder
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals

class TCFTest {

    // TCStringV2 [getVersion()=2, getCreated()=2022-10-17T13:44:54Z, getLastUpdated()=2022-10-17T13:44:54Z, getCmpId()=2, getCmpVersion()=1, getConsentScreen()=0, getConsentLanguage()=EN, getVendorListVersion()=128, getTcfPolicyVersion()=2, isServiceSpecific()=true, getUseNonStandardStacks()=false, getSpecialFeatureOptIns()={}, getPurposesConsent()={1}, getPurposesLITransparency()={}, getPurposeOneTreatment()=false, getPublisherCC()=AA, getVendorConsent()={1000, 1001, 1002, 1003, 1004}, getVendorLegitimateInterest()={1003, 1004}, getPublisherRestrictions()=[], getDisclosedVendors()={}, getAllowedVendors()={}, getPubPurposesConsent()={}, getPubPurposesLITransparency()={}, getCustomPurposesConsent()={}, getCustomPurposesLITransparency()={}]

    @Test
    fun `TCF Test`() {
        val tcfString = TCString.decode(TCF_ENCODED_STRING)
        val encodedTcfString = TCStringEncoder.newBuilder(tcfString).encode()

        assertEquals(TCF_ENCODED_STRING, encodedTcfString)
    }

    companion object {
        private const val TCF_ENCODED_STRING =
            "COvFyGBOvFyGBAbAAAENAPCAAOAAAAAAAAAAAEEUACCKAAA.IFoEUQQgAIQwgIwQABAEAAAAOIAACAIAAAAQAIAgEAACEAAAAAgAQBAAAAAAAGBAAgAAAAAAAFAAECAAAgAAQARAEQAAAAAJAAIAAgAAAYQEAAAQmAgBC3ZAYzUw"
    }
}