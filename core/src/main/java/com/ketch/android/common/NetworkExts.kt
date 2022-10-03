package com.ketch.android.common

import com.google.gson.Gson
import com.ketch.android.api.response.ApiError
import com.ketch.android.api.response.ErrorResponse
import com.ketch.android.api.response.ErrorResult
import com.ketch.android.api.response.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import retrofit2.HttpException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

internal fun <T> Flow<Result<T>>.handleErrors(): Flow<Result<T>> =
    catch { e ->
        emit(
            when (e) {
                is SocketTimeoutException -> Result.Error(ErrorResult.Offline)
                is UnknownHostException -> Result.Error(ErrorResult.ServerNotAvailable)
                is HttpException -> {
                    val errorResponse = convertErrorBody(e)
                    Result.Error(ErrorResult.HttpError(errorResponse))
                }
                else -> {
                    Result.Error(ErrorResult.OtherError(e))
                }
            }
        )
    }

private fun convertErrorBody(httpException: HttpException): ApiError {
    val code = httpException.code()

    val errorResponse =
        httpException.response()?.errorBody()?.string()?.let { Gson().fromJson(it, ErrorResponse::class.java) }

    return errorResponse?.apiError ?: ApiError(
        "", "UnknownError", code
    )
}
