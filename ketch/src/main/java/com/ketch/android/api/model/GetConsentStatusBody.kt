package com.ketch.android.api.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

/**
 * Model for wrapping all data that needs to be sent to the server as a HTTP POST 'GetConsentStatus' request body
 */
@Parcelize
data class GetConsentStatusBody(
    val applicationCode: String,
    val applicationEnvironmentCode: String?,
    val identities: List<String>,
    val purposes: Map<String, ApiConsentStatus>
) : Parcelable {

    companion object {

        /**
         * Transform activities Map<String, String> into form that could be set into {@link GetConsentStatusBody}
         */
        fun constructPurposes(activities: Map<String, String>): Map<String, ApiConsentStatus> =
            activities.map { (key, value) ->
                key to ApiConsentStatus(allowed = null, legalBasisCode = value)
            }.toMap()
    }
}
