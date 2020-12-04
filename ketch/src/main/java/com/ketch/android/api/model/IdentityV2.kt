package com.ketch.android.api.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import transponder.Transponder

@Parcelize
data class IdentityV2 (
    val space: String,
    val value: String
): Parcelable