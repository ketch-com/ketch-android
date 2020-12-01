package com.ketch.android.api.model

import android.os.Parcelable
import com.ketch.android.api.toApi
import com.ketch.android.model.ConsentStatus
import kotlinx.android.parcel.Parcelize

/**
 * Model for wrapping all data that needs to be sent to the server as a HTTP POST 'UpdateConsentStatus' request body
 */
@Parcelize
data class UpdateConsentStatusBody(
    val applicationCode: String,
    val applicationEnvironmentCode: String?,
    val identities: List<String>,
    val policyScopeCode: String?,
    val purposes: Map<String, ApiConsentStatus>,
    val migrationOption: Int
) : Parcelable {

    companion object {

        /**
         * Transform activities Map<String, ConsentStatus> into form that could be set into {@link UpdateConsentStatusBody}
         */
        fun constructPurposes(activities: Map<String, ConsentStatus>): Map<String, ApiConsentStatus> =
            activities.map { (key, consentStatus) ->
                key to consentStatus.toApi()
            }.toMap()
    }
}
