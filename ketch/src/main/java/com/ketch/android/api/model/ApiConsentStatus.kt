package com.ketch.android.api.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

/**
 * Model of Consent Status data in a form for sending to the server
 */
@Parcelize
data class ApiConsentStatus(
    val allowed: String?,
    val legalBasisCode: String?
) : Parcelable
