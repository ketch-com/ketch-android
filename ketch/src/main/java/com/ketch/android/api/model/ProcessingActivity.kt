package com.ketch.android.api.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class ConfigPurpose(
    val code: String?,
    val name: String?,
    val description: String?,
    val legalBasisCode: String?,
    val requiresPrivacyPolicy: Boolean?,
    val requiresOptIn: Boolean?,
    val requiresOptOut: Boolean?
) : Parcelable

@Parcelize
data class Purpose(
    val purpose: String,
    val legalBasis: String,
    val allowed: Boolean
): Parcelable