package com.ketch.android.api

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import retrofit2.Response

/**
 * Extension function for transforming {@link Response} received via {@link Retrofit} into client-friendly {@link Result}
 * Should be used for the {@link Flow} right after {@link ApiService} methods were called
 * @return a flow of Results
 */
internal inline fun <T, A : Any> Flow<Response<T>>.mapWithResult(crossinline f: (T) -> A): Flow<Result<RequestError, A>> =
    map { response ->
        if (response.isSuccessful) {
            val data = response.body()
            if (data != null) {
                Result.succeed(f(data))
            } else {
                Result.fail(NoResultError)
            }
        } else {
            Result.fail(HttpError(response.code(), response.message().orEmpty()))
        }
    }

/**
 * Extension function for mapping successful {@link Result} in the {@link Flow} by applying {@param f} transform function
 * In case of {@link Error} it will be passed down the flow
 * @return a flow of transformed Results
 */
inline fun <T : Any, A : Any> Flow<Result<RequestError, T>>.flatMapSuccess(
    crossinline f: (T) -> Flow<Result<RequestError, A>>
): Flow<Result<RequestError, A>> =
    flatMapLatest { result ->
        if (result is Result.Success) {
            f(result.value)
        } else {
            flow {
                emit(Result.fail((result as Result.Error).error))
            }
        }
    }
