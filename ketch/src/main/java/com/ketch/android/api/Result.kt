package com.ketch.android.api

import io.grpc.Status

/**
 * Classes for wrapping successful and failed responses from the server
 */
sealed class Result<out E : Any, out T : Any> {
    data class Success<out E : Any, out T : Any>(val value: T) : Result<E, T>()
    data class Error<out E : Any, out T : Any>(val error: E) : Result<E, T>()

    companion object {
        fun <A : Any> succeed(data: A): Result<Nothing, A> = Result.Success(data)

        fun <E : Any> fail(error: E): Result<E, Nothing> = Result.Error(error)
    }
}

/**
 * Classes for wrapping different kind of errors that could be received by the client
 */
sealed class RequestError

class StatusError(internal val code: Status.Code, val description: String?) : RequestError()
class OtherError(val exception: Throwable): RequestError()