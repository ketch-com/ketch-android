package com.ketch.android.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Consent(
    val purpose: String,
    val legalBasis: String,
    val allowed: Boolean
) : Parcelable