package com.ketch.android

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.ketch.android.api.Result
import com.ketch.android.api.model.Configuration
import kotlinx.android.synthetic.main.fragment_get_consent_status.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class GetConsentStatusFragment : BaseFragment() {

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
        return inflater.inflate(R.layout.fragment_get_consent_status, container, false);
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupActionBar("Usage. Get Consent Status", true)

        var config: Configuration? = null
        arguments?.getString(CONFIG_JSON)?.let {
            config = Gson().fromJson(it, Configuration::class.java)
        }

        getConsentStatus.setOnClickListener {
            if (identityKeyText.text.toString().isNotBlank() && config != null) {
                job = CoroutineScope(Dispatchers.Main).launch {
                    val purposes: Map<String, String> =
                        listOf(
                            analyticsConsent,
                            dataSalesConsent,
                            identityManagementConsent
                        ).filter { it.isChecked }
                            .map {
                                it.text.toString()
                            }
                            .map { consent ->
                                config!!.purposes?.find { it.code == consent }
                            }
                            .filterNotNull()
                            .map {
                                it.code!! to it.legalBasisCode!!
                            }.toMap()
                    repositoryProvider?.getRepository()?.getConsentStatus(
                        configuration = config!!,
                        identities = mapOf(identityKeyText.text.toString() to "testValue"),
                        purposes = purposes
                    )
                        ?.collect { result ->
                            when (result) {
                                is Result.Success -> {
                                    getConsentStatusResult.text =
                                        "${GsonBuilder().setPrettyPrinting().create().toJson(result.value)}\n"
                                }
                                is Result.Error -> {
                                    getConsentStatusResult.text =
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

        fun newInstance(configuration: Configuration): GetConsentStatusFragment =
            GetConsentStatusFragment().apply {
                arguments = Bundle().apply {
                    putString(CONFIG_JSON, Gson().toJson(configuration))
                }
            }
    }
}
