package com.ketch.android.model

data class UserData(
    val email: String?
)

data class UserDataV2(
    val first: String?,
    val last: String?,
    val country: String?,
    val region: String?,
    val email: String?
)