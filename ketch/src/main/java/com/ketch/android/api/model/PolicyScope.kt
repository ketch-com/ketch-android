package com.ketch.android.api.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

/**
 * Model that wraps the data of policy scope received from the server
 */
@Parcelize
data class PolicyScope(
    val defaultScopeCode: String?,
    val scopes: Map<String, String>?,
    val code: String?
) : Parcelable {

    /**
     * Choose one of the policy scope codes according to provided location code or null
     */
    fun getRecommendedScope(locationCode: String?): String? =
        scopes?.get(locationCode) ?: defaultScopeCode
}
