package com.ketch.android.api.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Deployment(
    val code: String?,
    val version: Int?
) : Parcelable
