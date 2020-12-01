package com.ketch.android.location

import android.content.Context
import android.location.Address
import android.location.Geocoder
import com.ketch.android.api.model.LocationCode
import java.util.*

object LocationUtils {

    /**
     * Util function for getting country code and region code {@link LocationCode} with Android Geocoder
     * In order to make this work following permissions must be declared in AndroidManifest.xml:
     * android.permission.INTERNET
     * android.permission.ACCESS_FINE_LOCATION
     */
    @JvmOverloads
    internal fun getLocationCode(
        lat: Double,
        lng: Double,
        context: Context,
        locale: Locale? = Locale.getDefault()
    ): LocationCode? {
        val geocoder = Geocoder(context, locale)
        val addresses: List<Address> = geocoder.getFromLocation(lat, lng, 1)
        val address: Address = addresses[0]
        return LocationCode(
            countryCode = address.countryCode,
            regionCode = address.adminArea
        )
    }
}
