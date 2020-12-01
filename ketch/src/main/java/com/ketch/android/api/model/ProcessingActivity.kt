package com.ketch.android.api.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Purpose(
    val code: String?,
    val name: String?,
    val description: String?,
    val legalBasisCode: String?,
    val requiresPrivacyPolicy: String?,
    val requiresOptIn: Boolean?,
    val requiresOptOut: Boolean?
) : Parcelable
