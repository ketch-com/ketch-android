package com.ketch.android.usecase

import com.ketch.android.api.Repository
import com.ketch.android.api.request.InvokeRightsRequest
import com.ketch.android.api.request.User
import com.ketch.android.api.response.Result
import com.ketch.android.common.handleErrors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext

internal class RightsUseCase(
    private val repository: Repository
) {
    suspend fun invokeRights(
        organization: String,
        controller: String?,
        property: String,
        environment: String,
        jurisdiction: String,
        invokedAt: Long?,
        identities: Map<String, String>,
        right: String?,
        user: User
    ): Flow<Result<Unit>> = flow {
        val result = withContext(Dispatchers.IO) {
            repository.invokeRights(
                organization = organization,
                request = InvokeRightsRequest(
                    controller = controller,
                    property = property,
                    environment = environment,
                    jurisdiction = jurisdiction,
                    invokedAt = invokedAt,
                    identities = identities,
                    right = right,
                    user = user
                )
            )
        }
        emit(result)
    }
        .map {
            Result.Success(it)
        }
        .handleErrors()
        .onStart {
            emit(Result.Loading())
        }
}