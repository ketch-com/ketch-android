package com.ketch.android

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.ketch.android.api.Result
import com.ketch.android.api.model.Configuration
import com.ketch.android.api.model.IdentitySpace
import com.ketch.android.api.model.ConfigPurpose
import com.ketch.android.api.model.Purpose
import com.ketch.android.example.R
import kotlinx.android.synthetic.main.fragment_get_consent_status.*
import kotlinx.android.synthetic.main.fragment_get_consent_status.consent1
import kotlinx.android.synthetic.main.fragment_get_consent_status.consent2
import kotlinx.android.synthetic.main.fragment_get_consent_status.identityKeyText
import kotlinx.android.synthetic.main.fragment_get_consent_status.consent3
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

        populateConsent(config?.purposes?.get(0), consent1)
        populateConsent(config?.purposes?.get(1), consent2)
        populateConsent(config?.purposes?.get(2), consent3)

        getConsentStatus.setOnClickListener {
            if (identityKeyText.text.toString().isNotBlank() && config != null) {
                job = CoroutineScope(Dispatchers.Main).launch {

                    val identities: List<IdentitySpace> = config!!.identities!!.map {
                        IdentitySpace(it.key, identityKeyText.text.toString())
                    }

                    repositoryProvider?.getRepository()?.getConsent(
                        configuration = config!!,
                        identities = identities,
                        purposes =  listOf(
                            consent1,
                            consent2,
                            consent3
                        ).filter { it.isChecked }
                        .map {
                            it.text.toString()
                        }
                        .map { consent ->
                            config!!.purposes?.find { it.code == consent }
                        }
                        .filterNotNull()
                            .map {
                                Purpose(it.code!!, it.legalBasisCode!!, false)
                            }
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

    private fun populateConsent(configPurpose: ConfigPurpose?, checkBox: CheckBox) {
        configPurpose?.code?.let {
            checkBox.text = it
        } ?: run {
            checkBox.visibility = View.GONE
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
