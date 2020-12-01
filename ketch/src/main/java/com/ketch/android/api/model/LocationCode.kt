package com.ketch.android.api.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

/**
 * Model that wraps location information if the form of a code
 */
@Parcelize
data class LocationCode(
    val countryCode: String?,
    val regionCode: String?
) : Parcelable {

    /**
     * Get formatted location code or null
     */
    fun getGeolocationCode(): String? =
        if (countryCode == UNITED_STATES_CODE) {
            "$countryCode-$regionCode"
        } else {
            countryCode
        }

    companion object {
        private const val UNITED_STATES_CODE = "US"
    }
}
