package com.ketch.android.api

internal class EndPoints {
    companion object {
        const val CONFIG = "config/{organization}/{property}/config.json"
        const val FULL_CONFIG = "config/{organization}/{property}/{environment}/{hash}/{jurisdiction}/{language}/config.json"
        const val GET_CONSENT = "consent/{organization}/get"
        const val UPDATE_CONSENT = "consent/{organization}/update"
        const val INVOKE_RIGHTS = "rights/{organization}/invoke"
    }
}