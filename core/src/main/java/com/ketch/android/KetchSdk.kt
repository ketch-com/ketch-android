package com.ketch.android

import android.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.ketch.android.api.KetchApi
import com.ketch.android.api.Repository
import com.ketch.android.common.Constants
import com.ketch.android.usecase.ConsentUseCase
import com.ketch.android.usecase.OrganizationConfigUseCase
import com.ketch.android.usecase.RightsUseCase
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Factory to create the Ketch singleton.
 *
 *         val ketch = KetchSdk.create(
 *             organization = ORGANIZATION,
 *             property = PROPERTY,
 *             environment = ENVIRONMENT,
 *             controller = CONTROLLER,
 *             identities = mapOf(ADVERTISING_ID_CODE to advertisingId)
 *         )
 **/
object KetchSdk {
    private val TAG = KetchSdk::class.java.simpleName

    private val api: KetchApi
        get() {
            val gson: Gson = GsonBuilder()
                .setLenient()
                .create()

            val builder: Retrofit.Builder = Retrofit.Builder()
                .baseUrl(Constants.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create(gson))

            val okHttpBuilder = OkHttpClient.Builder()
            okHttpBuilder.addInterceptor {
                val requestBuilder = it.request().newBuilder()
                requestBuilder.addHeader("accept", "application/json")
                it.proceed(requestBuilder.build())
            }

            if (BuildConfig.DEBUG) {
                okHttpBuilder
                    .addInterceptor(
                        HttpLoggingInterceptor { message ->
                            Log.d(TAG, message)
                        }
                            .setLevel(HttpLoggingInterceptor.Level.BODY)
                    )
            }
            builder.client(okHttpBuilder.build())
            val retrofit = builder.build()

            return retrofit.create(KetchApi::class.java)
        }

    /**
     * Creates the Ketch singleton
     *
     * @param organization - organization code
     * @param property - property code
     * @param environment - environment name
     * @param controller - controller name
     * @param identities - Map<identities code, identities name>
     */
    fun create(
        organization: String,
        property: String,
        environment: String,
        controller: String?,
        identities: Map<String, String>
    ): Ketch {
        val repository = Repository(api)
        val organizationConfigUseCase = OrganizationConfigUseCase(repository)
        val consentUseCase = ConsentUseCase(repository)
        val rightsUseCase = RightsUseCase(repository)

        return Ketch(
            organization = organization,
            property = property,
            environment = environment,
            controller = controller,
            identities = identities,
            organizationConfigUseCase = organizationConfigUseCase,
            consentUseCase = consentUseCase,
            rightsUseCase = rightsUseCase,
        )
    }
}