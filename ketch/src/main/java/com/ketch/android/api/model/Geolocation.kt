package com.ketch.android.api.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Geolocation(
    val location: LocationCode?
) : Parcelable {

    fun getGeolocationCode(): String? = location?.getGeolocationCode()
}
