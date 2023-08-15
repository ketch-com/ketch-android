package com.ketch.android

import android.util.Log
import com.google.gson.Gson
import com.ketch.android.api.request.MigrationOption
import com.ketch.android.api.request.PurposeAllowedLegalBasis
import com.ketch.android.api.request.PurposeLegalBasis
import com.ketch.android.api.request.User
import com.ketch.android.api.response.Consent
import com.ketch.android.api.response.ErrorResult
import com.ketch.android.api.response.FullConfiguration
import com.ketch.android.api.response.Result
import com.ketch.android.plugin.Plugin
import com.ketch.android.usecase.ConsentUseCase
import com.ketch.android.usecase.OrganizationConfigUseCase
import com.ketch.android.usecase.RightsUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Main Ketch SDK class
 **/
class Ketch internal constructor(
    private val organization: String,
    private val property: String,
    private val environment: String,
    private val controller: String?,
    private val identities: Map<String, String>,
    private val organizationConfigUseCase: OrganizationConfigUseCase,
    private val consentUseCase: ConsentUseCase,
    private val rightsUseCase: RightsUseCase,
) {
    private val plugins = mutableSetOf<Plugin>()

    /**
     * Adds a plugin object inherited of [com.ketch.android.plugin.Plugin] to the collection
     */
    fun addPlugin(plugin: Plugin): Boolean = plugins.add(plugin)

    /**
     * Adds plugin objects inherited of [com.ketch.android.plugin.Plugin] to the collection
     */
    fun addPlugins(vararg plugin: Plugin): Boolean = plugins.addAll(plugin)

    /**
     * Removes a previously added [com.ketch.android.plugin.Plugin] from the collection
     */
    fun removePlugin(plugin: Plugin): Boolean = plugins.remove(plugin)

    /**
     * Removes all previously added plugins from the collection
     */
    fun removeAllPlugins() {
        plugins.clear()
    }

    /**
     * Checks if the [com.ketch.android.plugin.Plugin] is contained in the collection.
     */
    fun containsPlugin(plugin: Plugin): Boolean = plugins.contains(plugin)

    private val _notifications = Channel<KetchNotification>(Channel.UNLIMITED)

    /**
     * Returns a channel with the request status as a hot flow.
     */
    val notifications: Flow<KetchNotification> = _notifications.receiveAsFlow()

    private val _loading = MutableStateFlow(false)

    /**
     * Represents the loading state flow
     *
     * collectState(ketch.loading) {
     *    binding.progressBar.isVisible = it
     * }
     */
    val loading: StateFlow<Boolean> = _loading

    private val _configuration = MutableStateFlow<FullConfiguration?>(null)

    /**
     * Represents the [com.ketch.android.api.response.FullConfiguration] state flow
     *
     * collectState(ketch.configuration) {
     *    ...
     * }
     */
    val configuration: StateFlow<FullConfiguration?> = _configuration

    private val _consent = MutableStateFlow<Consent?>(null)

    /**
     * Represents the [com.ketch.android.api.response.Consent] state flow
     *
     * collectState(ketch.consent) {
     *    ...
     * }
     */
    val consent: StateFlow<Consent?> = _consent

    private val scope = CoroutineScope(Dispatchers.Default)

    init {
        configuration.filterNotNull()
            .onEach {
                configLoaded(it)
            }
            .launchIn(scope)

        consent.filterNotNull()
            .onEach {
                consentChanged(it)
            }
            .launchIn(scope)
    }

    /**
     * Loads the [com.ketch.android.api.response.FullConfiguration] for the default parameters.
     */
    fun loadConfiguration() {
        scope.launch {
            organizationConfigUseCase.getFullConfiguration(organization, property, Locale.getDefault().language)
                .collect {
                    _loading.value = it is Result.Loading

                    when (it) {
                        is Result.Success -> {
                            Log.d(TAG, Gson().toJson(it.data))
                            _configuration.value = it.data
                            _notifications.trySend(KetchNotification.KetchNotificationSuccess.LoadConfigurationSuccess())
                        }
                        is Result.Error -> {
                            Log.d(TAG, "loadConfiguration: failed")
                            _notifications.trySend(KetchNotification.KetchNotificationError.LoadConfigurationError(it.error))
                        }
                        else -> {}
                    }
                }
        }
    }

    /**
     * Loads the [com.ketch.android.api.response.FullConfiguration] for the specified jurisdiction.
     * @param jurisdiction - your specific jurisdiction
     */
    fun loadConfiguration(
        jurisdiction: String
    ) {
        val hash: Long = System.currentTimeMillis()
        val language = Locale.getDefault().toLanguageTag()

        scope.launch {
            organizationConfigUseCase.getFullConfiguration(
                organization, property, environment, hash, jurisdiction, language
            ).collect {
                _loading.value = it is Result.Loading

                when (it) {
                    is Result.Success -> {
                        Log.d(TAG, Gson().toJson(it.data))
                        _configuration.value = it.data
                        _notifications.trySend(KetchNotification.KetchNotificationSuccess.LoadConfigurationSuccess())
                    }
                    is Result.Error -> {
                        Log.d(TAG, "loadConfiguration: failed")
                        _notifications.trySend(KetchNotification.KetchNotificationError.LoadConfigurationError(it.error))
                    }
                    else -> {}
                }
            }
        }
    }

    /**
     * Loads the current state of the user's [com.ketch.android.api.response.Consent] flags.
     */
    fun loadConsent() {
        val jurisdiction = configuration.value?.jurisdiction?.code ?: return
        val purposes: Map<String, PurposeLegalBasis> = configuration.value?.purposes?.map {
            it.code to PurposeLegalBasis(
                it.legalBasisCode
            )
        }?.toMap() ?: return

        scope.launch {
            consentUseCase.getConsent(
                organization, controller, property, environment, jurisdiction, identities, purposes
            ).collect {
                _loading.value = it is Result.Loading

                when (it) {
                    is Result.Success -> {
                        Log.d(TAG, Gson().toJson(it.data))
                        _consent.value = it.data
                        _notifications.trySend(KetchNotification.KetchNotificationSuccess.LoadConsentSuccess())
                    }
                    is Result.Error -> {
                        Log.d(TAG, "getConsent: failed")
                        _notifications.trySend(KetchNotification.KetchNotificationError.LoadConsentError(it.error))
                    }
                    else -> {}
                }
            }
        }
    }

    /**
     * Updates the user's [com.ketch.android.api.response.Consent] flags.
     * @param - Map<purpose code, [com.ketch.android.api.request.PurposeAllowedLegalBasis]>
     */
    fun updateConsent(
        purposes: Map<String, PurposeAllowedLegalBasis>,
        vendors: List<String>?
    ) {
        val jurisdiction = configuration.value?.jurisdiction?.code ?: return

        scope.launch {
            consentUseCase.updateConsent(
                organization,
                controller,
                property,
                environment,
                jurisdiction,
                identities,
                purposes,
                MigrationOption.MIGRATE_DEFAULT,
                vendors
            ).collect {
                _loading.value = it is Result.Loading

                when (it) {
                    is Result.Success -> {
                        Log.d(TAG, "updateConsent: success")
                        _consent.value = Consent(
                            purposes = purposes.map {
                                it.key to it.value.allowed
                            }.toMap(),
                            vendors = vendors
                        )
                        _notifications.trySend(KetchNotification.KetchNotificationSuccess.UpdateConsentSuccess())
                    }
                    is Result.Error -> {
                        Log.d(TAG, "updateConsent: failed")
                        _notifications.trySend(KetchNotification.KetchNotificationError.UpdateConsentError(it.error))
                    }
                    else -> {}
                }
            }
        }
    }

    /**
     * Invokes the specified rights.
     * @param right - code of [com.ketch.android.api.response.Right]
     * @param user - the current [com.ketch.android.api.request.User]
     */
    fun invokeRights(
        right: String,
        user: User
    ) {
        val jurisdiction = configuration.value?.jurisdiction?.code ?: return
        val invokedAt = System.currentTimeMillis()

        scope.launch {
            rightsUseCase.invokeRights(
                organization, controller, property, environment, jurisdiction, invokedAt, identities, right, user
            ).collect {
                _loading.value = it is Result.Loading

                when (it) {
                    is Result.Success -> {
                        Log.d(TAG, "invokeRights: success")
                        _notifications.trySend(KetchNotification.KetchNotificationSuccess.InvokeRightsSuccess())
                    }
                    is Result.Error -> {
                        Log.d(TAG, "invokeRights: failed")
                        _notifications.trySend(KetchNotification.KetchNotificationError.InvokeRightsError(it.error))
                    }
                    else -> {}
                }
            }
        }
    }

    private fun configLoaded(configuration: FullConfiguration) {
        plugins.onEach {
            it.configLoaded(configuration)
        }
    }

    private fun consentChanged(consent: Consent) {
        plugins.onEach {
            it.consentChanged(consent)
        }
    }

    companion object {
        private val TAG = Ketch::class.java.simpleName
    }
}

/**
 * Represents Ketch response notification
 * @param error - [com.ketch.android.api.response.ErrorResult]
 */
sealed class KetchNotification() {
    /**
     * Represents Ketch response success notification
     */
    sealed class KetchNotificationSuccess() : KetchNotification() {
        /**
         * Success Notification in the full configuration loading
         */
        class LoadConfigurationSuccess() : KetchNotificationSuccess()

        /**
         * Success Notification in the consent loading
         */
        class LoadConsentSuccess() : KetchNotificationSuccess()

        /**
         * Success Notification in the consent updating
         */
        class UpdateConsentSuccess() : KetchNotificationSuccess()

        /**
         * Success Notification in the rights invoking
         */
        class InvokeRightsSuccess() : KetchNotificationSuccess()
    }

    /**
     * Represents Ketch response error notification
     * @param error - [com.ketch.android.api.response.ErrorResult]
     */
    sealed class KetchNotificationError(val error: ErrorResult) : KetchNotification() {

        /**
         * Error Notification in the full configuration loading
         */
        class LoadConfigurationError(error: ErrorResult) : KetchNotificationError(error)

        /**
         * Error Notification in the consent loading
         */
        class LoadConsentError(error: ErrorResult) : KetchNotificationError(error)

        /**
         * Error Notification in the consent updating
         */
        class UpdateConsentError(error: ErrorResult) : KetchNotificationError(error)

        /**
         * Error Notification in the rights invoking
         */
        class InvokeRightsError(error: ErrorResult) : KetchNotificationError(error)
    }
}
