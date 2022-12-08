package com.ketch.android.api

import com.ketch.android.api.request.GetConsentRequest
import com.ketch.android.api.request.InvokeRightsRequest
import com.ketch.android.api.request.UpdateConsentRequest
import com.ketch.android.api.response.Consent
import com.ketch.android.api.response.FullConfiguration
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

internal interface KetchApi {
    @GET(EndPoints.CONFIG)
    suspend fun getFullConfiguration(
        @Path("organization") organization: String,
        @Path("property") property: String,
    ): FullConfiguration

    @GET(EndPoints.FULL_CONFIG)
    suspend fun getFullConfiguration(
        @Path("organization") organization: String,
        @Path("property") property: String,
        @Path("environment") environment: String,
        @Path("hash") hash: Long,
        @Path("jurisdiction") jurisdiction: String,
        @Path("language") language: String
    ): FullConfiguration

    @POST(EndPoints.GET_CONSENT)
    suspend fun getConsent(
        @Path("organization") organization: String,
        @Body body: GetConsentRequest
    ): Consent

    @POST(EndPoints.UPDATE_CONSENT)
    suspend fun updateConsent(
        @Path("organization") organization: String,
        @Body body: UpdateConsentRequest
    )

    @POST(EndPoints.INVOKE_RIGHTS)
    suspend fun invokeRights(
        @Path("organization") organization: String,
        @Body body: InvokeRightsRequest
    )
}
