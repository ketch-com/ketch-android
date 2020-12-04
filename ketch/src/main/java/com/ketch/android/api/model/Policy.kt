package com.ketch.android.api.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class PolicyDocument(
    val code: String?,
    val version: Long?,
    val url: String?
) : Parcelable
