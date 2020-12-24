package com.ketch.android.api.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class IdentityType(
    val type: String?,
    val variable: String?
) : Parcelable
