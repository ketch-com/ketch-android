package com.ketch.android.api.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Organization(
    val name: String,
    val code: String?
) : Parcelable
