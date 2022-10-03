package com.ketch.android.api.response

import com.google.gson.annotations.SerializedName

data class ErrorResponse(
    @SerializedName("error") val apiError: ApiError
)

data class ApiError(
    @SerializedName("code") val code: String,
    @SerializedName("message") val errMessage: String?,
    @SerializedName("status") val status: Int,
)

sealed class ErrorResult {
    data class HttpError(val error: ApiError) : ErrorResult()
    object Offline : ErrorResult()
    object ServerNotAvailable : ErrorResult()
    data class OtherError(val throwable: Throwable) : ErrorResult()
}

sealed class Result<T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error<T>(val error: ErrorResult) : Result<T>()
    class Loading<T> : Result<T>()
}