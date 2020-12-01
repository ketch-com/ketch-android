package com.ketch.android.api.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class ApplicationInfo(
    val code: String?,
    val name: String?,
    val platform: String?
) : Parcelable
