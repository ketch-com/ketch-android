package com.ketch.android.api

import androidx.annotation.VisibleForTesting
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Class for handling {@link Retrofit} and auto-generated {@link ApiService} instances that do all network related routine
 */
class KetchApiClient {

    private lateinit var retrofit: Retrofit
    private lateinit var apiService: ApiService

    init {
        initApiService(INITIAL_BASE_URL)
    }

    /**
     * Recreate {@link Retrofit} instance and generate new {@link ApiService} object with it based on a provided {@param baseUrl}
     */
    private fun initApiService(baseUrl: String) {
        retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        apiService = retrofit.create(ApiService::class.java)
    }

    /**
     * Create new {@link ApiService} object if new {@param baseUrl} is set
     */
    @JvmOverloads
    fun getApiService(baseUrl: String? = null): ApiService {
        if (retrofit.baseUrl().toString() == baseUrl) {
            return apiService
        }
        initApiService(baseUrl ?: INITIAL_BASE_URL)
        return apiService
    }

    @VisibleForTesting
    internal fun setApiService(baseUrl: String) {
        initApiService(baseUrl)
    }

    companion object {
        // Base hardcoded URL for retrieving initial data (bootstrap configuration)
        private const val INITIAL_BASE_URL = "https://cdn.b10s.io/supercargo/config/1/"
    }
}
