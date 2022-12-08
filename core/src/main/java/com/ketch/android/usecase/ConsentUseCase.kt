package com.ketch.android.usecase

import com.ketch.android.api.Repository
import com.ketch.android.api.request.GetConsentRequest
import com.ketch.android.api.request.MigrationOption
import com.ketch.android.api.request.PurposeAllowedLegalBasis
import com.ketch.android.api.request.PurposeLegalBasis
import com.ketch.android.api.request.UpdateConsentRequest
import com.ketch.android.api.response.Consent
import com.ketch.android.api.response.Result
import com.ketch.android.common.handleErrors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext

internal class ConsentUseCase(
    private val repository: Repository
) {
    suspend fun getConsent(
        organization: String,
        controller: String?,
        property: String,
        environment: String,
        jurisdiction: String,
        identities: Map<String, String>,
        purposes: Map<String, PurposeLegalBasis>
    ): Flow<Result<Consent>> = flow {
        val result = withContext(Dispatchers.IO) {
            repository.getConsent(
                organization = organization,
                request = GetConsentRequest(
                    controller = controller,
                    property = property,
                    environment = environment,
                    jurisdiction = jurisdiction,
                    identities = identities,
                    purposes = purposes
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

    suspend fun updateConsent(
        organization: String,
        controller: String?,
        property: String,
        environment: String,
        jurisdiction: String,
        identities: Map<String, String>,
        purposes: Map<String, PurposeAllowedLegalBasis>,
        migrationOption: MigrationOption?,
        vendors: List<String>?
    ): Flow<Result<Unit>> = flow {
        val result = withContext(Dispatchers.IO) {
            repository.updateConsent(
                organization = organization,
                request = UpdateConsentRequest(
                    controller = controller,
                    property = property,
                    environment = environment,
                    jurisdiction = jurisdiction,
                    identities = identities,
                    purposes = purposes,
                    migrationOption = migrationOption,
                    vendors = vendors
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