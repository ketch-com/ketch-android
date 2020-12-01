package com.ketch.android.api.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

/**
 * Model for wrapping all data that needs to be sent to the server as a HTTP POST 'InvokeRights' request body
 */
@Parcelize
data class InvokeRightsBody(
    val applicationCode: String,
    val applicationEnvironmentCode: String?,
    val identities: List<String>,
    val policyScopeCode: String?,
    val rightsEmail: String?,
    val rightCodes: List<String>
) : Parcelable
