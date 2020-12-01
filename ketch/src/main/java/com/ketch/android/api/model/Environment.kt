package com.ketch.android.api.model

import android.os.Parcelable
import android.util.Base64
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Environment(
    val code: String?,
    val pattern: String?,
    val hash: String?
) : Parcelable {

    fun matchPattern(env: String): Boolean =
        env.contains(Base64.decode(pattern, Base64.DEFAULT).toString())
}
