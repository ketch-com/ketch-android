package com.ketch.android.api.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class PolicyDocument(
    val code: String?,
    val version: Int?,
    val url: String?
) : Parcelable
