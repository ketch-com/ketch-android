package com.ketch.android.tcf

import com.ketch.android.api.response.GvlVendors
import retrofit2.http.GET

internal interface KetchApi {
    @GET(VENDORS)
    suspend fun getVendors(): GvlVendors

    companion object {
        const val VENDORS = "gvl/vendor-list.json"
    }
}
