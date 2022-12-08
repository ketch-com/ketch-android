package com.ketch.android.usecase

import com.ketch.android.api.Repository
import com.ketch.android.api.response.FullConfiguration
import com.ketch.android.api.response.Result
import com.ketch.android.common.handleErrors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext

internal class OrganizationConfigUseCase(
    private val repository: Repository
) {

    suspend fun getFullConfiguration(
        organization: String,
        property: String
    ): Flow<Result<FullConfiguration>> = flow {
        val result = withContext(Dispatchers.IO) {
            repository.getFullConfiguration(organization, property)
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

    suspend fun getFullConfiguration(
        organization: String,
        property: String,
        environment: String,
        hash: Long,
        jurisdiction: String,
        language: String
    ): Flow<Result<FullConfiguration>> = flow {
        val result = withContext(Dispatchers.IO) {
            repository.getFullConfiguration(organization, property, environment, hash, jurisdiction, language)
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