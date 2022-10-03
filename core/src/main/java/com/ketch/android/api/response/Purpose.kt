package com.ketch.android.api.response

import com.google.gson.annotations.SerializedName

data class Purpose(
    @SerializedName("code") val code: String,
    @SerializedName("name") val name: String?,
    @SerializedName("description") val description: String?,
    @SerializedName("legalBasisCode") val legalBasisCode: String,
    @SerializedName("requiresOptIn") val requiresOptIn: Boolean?,
    @SerializedName("allowsOptOut") val allowsOptOut: Boolean?,
    @SerializedName("cookies") val cookies: List<Cookie>?,
    @SerializedName("categories") val categories: List<PurposeCategory>?,
    @SerializedName("requiresDisplay") val requiresDisplay: Boolean?,
    @SerializedName("requiresPrivacyPolicy") val requiresPrivacyPolicy: Boolean?,
    @SerializedName("tcfType") val tcfType: String?,
    @SerializedName("tcfID") val tcfID: String?,
    @SerializedName("canonicalPurposeCode") val canonicalPurposeCode: String?,
    @SerializedName("legalBasisName") val legalBasisName: String?,
    @SerializedName("legalBasisDescription") val legalBasisDescription: String?,

    // the data subject types for which the purpose is relevant. If this list is empty then the purpose applies to all data subject types
    @SerializedName("dataSubjectTypeCodes") val dataSubjectTypeCodes: List<String>?
)
