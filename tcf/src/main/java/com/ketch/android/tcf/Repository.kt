package com.ketch.android.tcf

import com.ketch.android.api.response.GvlVendors

internal class Repository(
    private val api: KetchApi,
) {
    suspend fun getVendors(): GvlVendors = api.getVendors()
}