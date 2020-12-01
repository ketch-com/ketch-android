package com.ketch.android.api.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Right(
    val code: String?,
    val name: String?,
    val description: String?
) : Parcelable
