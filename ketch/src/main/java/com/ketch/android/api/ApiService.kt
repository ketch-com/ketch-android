package com.ketch.android.api

import com.ketch.android.api.model.BootstrapConfiguration
import com.ketch.android.api.model.Configuration
import com.ketch.android.api.model.Geolocation
import com.ketch.android.api.model.GetConsentStatusBody
import com.ketch.android.api.model.GetConsentStatusResponse
import com.ketch.android.api.model.InvokeRightsBody
import com.ketch.android.api.model.UpdateConsentStatusBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface ApiService {

    @GET("{organizationCode}/{applicationCode}/boot.json")
    suspend fun getBootstrapConfiguration(
        @Path("organizationCode") organizationCode: String,
        @Path("applicationCode") applicationCode: String
    ): Response<BootstrapConfiguration>

    /**
     * {config.services.astrolabe}
     */
    @GET("{ip}")
    suspend fun getGeolocationWithIp(
        @Path("ip") ip: String
    ): Response<Geolocation>

    /**
     * {config.services.astrolabe}
     */
    @GET(".")
    suspend fun getGeolocation(): Response<Geolocation>

    /**
     * {config.services.supercargo}
     */
    @GET("{organizationCode}/{applicationCode}/{environmentCode}/{environmentHash}/{policyScopeCode}/{languageCode}/config.json")
    suspend fun getFullConfiguration(
        @Path("organizationCode") organizationCode: String,
        @Path("applicationCode") applicationCode: String,
        @Path("environmentCode") environmentCode: String?,
        @Path("environmentHash") environmentHash: String?,
        @Path("policyScopeCode") policyScopeCode: String?,
        @Path("languageCode") languageCode: String
    ): Response<Configuration>

    /**
     * {config.services.wheelhouse}
     */
    @POST("consent/{organizationCode}/get")
    suspend fun getConsentStatus(
        @Path("organizationCode") organizationCode: String,
        @Body getConsentStatusBody: GetConsentStatusBody
    ): Response<GetConsentStatusResponse>

    /**
     * {config.services.wheelhouse}
     */
    @POST("consent/{organizationCode}/update")
    suspend fun updateConsentStatus(
        @Path("organizationCode") organizationCode: String,
        @Body updateConsentStatusBody: UpdateConsentStatusBody
    ): Response<Unit>

    /**
     * {config.services.gangplank}
     */
    @POST("rights/{organizationCode}/invoke")
    suspend fun invokeRights(
        @Path("organizationCode") organizationCode: String,
        @Body invokeRightsBody: InvokeRightsBody
    ): Response<Unit>
}
