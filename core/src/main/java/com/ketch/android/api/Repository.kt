package com.ketch.android.api

import com.ketch.android.api.request.GetConsentRequest
import com.ketch.android.api.request.InvokeRightsRequest
import com.ketch.android.api.request.UpdateConsentRequest
import com.ketch.android.api.response.Consent
import com.ketch.android.api.response.FullConfiguration

internal class Repository(
    private val api: KetchApi,
) {
    suspend fun getFullConfiguration(
        organization: String,
        property: String
    ): FullConfiguration = api.getFullConfiguration(organization, property)

    suspend fun getFullConfiguration(
        organization: String,
        property: String,
        environment: String,
        hash: Long,
        jurisdiction: String,
        language: String
    ): FullConfiguration = api.getFullConfiguration(organization, property, environment, hash, jurisdiction, language)

    suspend fun getConsent(
        organization: String,
        request: GetConsentRequest
    ): Consent = api.getConsent(organization, request)

    suspend fun updateConsent(
        organization: String,
        request: UpdateConsentRequest
    ) {
        api.updateConsent(organization, request)
    }

    suspend fun invokeRights(
        organization: String,
        request: InvokeRightsRequest
    ) {
        api.invokeRights(organization, request)
    }
}