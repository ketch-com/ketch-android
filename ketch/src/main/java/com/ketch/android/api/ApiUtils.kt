package com.ketch.android.api

import com.ketch.android.api.model.ApiConsentStatus
import com.ketch.android.model.ConsentStatus

/**
 * Util function for transforming {@link ConsentStatus} data from server representation into client-friendly one
 */
fun ApiConsentStatus.toDomain() =
    ConsentStatus(
        allowed?.toBoolean(),
        legalBasisCode
    )

/**
 * Util function for transforming {@link ConsentStatus} data from client-friendly representation into server one
 */
fun ConsentStatus.toApi() =
    ApiConsentStatus(
        allowed.toString(),
        legalBasisCode
    )

object ApiUtils {

    /**
     * Util function for transforming identities Map<String, String> into the form required by the server
     */
    fun constructIdentities(
        organizationCode: String,
        identities: Map<String, String>
    ): List<String> =
        identities.map { (code, value) ->
            "srn:::::$organizationCode:id/$code/$value"
        }
}
