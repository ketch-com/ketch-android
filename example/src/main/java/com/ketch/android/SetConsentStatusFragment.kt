package com.ketch.android

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.ketch.android.api.MigrationOption
import com.ketch.android.api.Result
import com.ketch.android.api.model.Configuration
import com.ketch.android.model.ConsentStatus
import kotlinx.android.synthetic.main.fragment_set_consent_status.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class SetConsentStatusFragment : BaseFragment() {

    private var repositoryProvider: RepositoryProvider? = null
    private var config: Configuration? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is RepositoryProvider) {
            repositoryProvider = context
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_set_consent_status, container, false);
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupActionBar("Usage. Set Consent Status", true)

        var config: Configuration? = null
        arguments?.getString(CONFIG_JSON)?.let {
            config = Gson().fromJson(it, Configuration::class.java)
        }

        setConsentStatus.setOnClickListener {
            if (identityKeyText.text.toString().isNotBlank() && config != null) {
                job = CoroutineScope(Dispatchers.Main).launch {
                    val consents: Map<String, ConsentStatus> =
                        mapOf(
                            analyticsConsent to analyticsConsentSwitch,
                            dataSalesConsent to dataSalesConsentSwitch,
                            identityManagementConsent to identityManagementConsentSwitch
                        )
                            .filter { it.key.isChecked }
                            .map { (consent, switch) ->
                                consent.text.toString() to ConsentStatus(
                                    allowed = switch.isChecked,
                                    legalBasisCode = config!!.purposes?.find { it.code == consent.text.toString() }?.legalBasisCode
                                )
                            }
                            .toMap()
                    repositoryProvider?.getRepository()?.updateConsentStatus(
                        configuration = config!!,
                        identities = mapOf(identityKeyText.text.toString() to "testValue"),
                        consents = consents,
                        migrationOption = MigrationOption.MIGRATE_ALWAYS
                    )
                        ?.collect { result ->
                            when (result) {
                                is Result.Success -> {
                                    setConsentStatusResult.text =
                                        GsonBuilder().setPrettyPrinting().create()
                                            .toJson(JsonObject())
                                }
                                is Result.Error -> {
                                    setConsentStatusResult.text =
                                        GsonBuilder().setPrettyPrinting().create()
                                            .toJson(result.error)
                                }
                            }
                        }
                }
            }
        }
    }

    companion object {
        const val CONFIG_JSON = "configJson"

        fun newInstance(configuration: Configuration): SetConsentStatusFragment =
            SetConsentStatusFragment().apply {
                arguments = Bundle().apply {
                    putString(CONFIG_JSON, Gson().toJson(configuration))
                }
            }
    }
}
