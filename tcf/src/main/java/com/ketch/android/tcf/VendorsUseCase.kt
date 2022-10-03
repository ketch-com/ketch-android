package com.ketch.android.tcf

import android.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.ketch.android.BuildConfig
import com.ketch.android.api.response.ErrorResult
import com.ketch.android.api.response.GvlVendors
import com.ketch.android.api.response.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

internal class VendorsUseCase() {
    private val retrofit: Retrofit
        get() {
            val gson: Gson = GsonBuilder()
                .setLenient()
                .create()

            val builder: Retrofit.Builder = Retrofit.Builder()
                .baseUrl(BASE_URL)
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
            return builder.build()
        }

    private val api: KetchApi = retrofit.create(KetchApi::class.java)

    private val repository: Repository = Repository(api)

    suspend fun getVendors(): Flow<Result<GvlVendors>> = flow {
        val result = withContext(Dispatchers.IO) {
            repository.getVendors()
        }
        emit(result)
    }
        .map {
            Result.Success(it)
        }
        .handleErrors()

    private fun Flow<Result<GvlVendors>>.handleErrors(): Flow<Result<GvlVendors>> =
        catch { e ->
            emit(Result.Error(ErrorResult.OtherError(e)))
        }

    companion object {
        private val TAG = VendorsUseCase::class.java.simpleName

        private const val BASE_URL = "https://global.ketchcdn.com/web/v2/"
    }
}