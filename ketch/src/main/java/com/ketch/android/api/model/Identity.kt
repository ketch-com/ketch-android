package com.ketch.android.api.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Identity(
    val type: String?,
    val variable: String?
) : Parcelable
