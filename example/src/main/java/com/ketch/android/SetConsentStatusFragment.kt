package com.ketch.android

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.Toast
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.ketch.android.api.Result
import com.ketch.android.api.model.*
import com.ketch.android.example.R
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

        populateConsent(config?.purposes?.get(0), consent1)
        populateConsent(config?.purposes?.get(1), consent2)
        populateConsent(config?.purposes?.get(2), consent3)

        setConsentStatus.setOnClickListener {
            if (identityKeyText.text.toString().isNotBlank() && config != null) {
                job = CoroutineScope(Dispatchers.Main).launch {
                    val identities: List<IdentitySpace> = config!!.identities!!.map {
                        IdentitySpace(it.key, identityKeyText.text.toString())
                    }

                    repositoryProvider?.getRepository()?.setConsent(
                        configuration = config!!,
                        identities = identities,
                        purposes = mapOf(
                            consent1 to consent1Switch,
                            consent2 to consent2Switch,
                            consent3 to consent3Switch
                        )
                            .filter { it.key.isChecked }
                            .map { (consent, switch) ->
                                consent.text.toString() to ConsentStatus(
                                    allowed = switch.isChecked,
                                    legalBasisCode = config!!.purposes?.find { it.code == consent.text.toString() }?.legalBasisCode
                                )
                            }.
                            map {(consent, consentStatus) ->
                                Log.d("~~~", "$consent,${consentStatus.legalBasisCode}, ${consentStatus.allowed}")
                                Purpose(consent, consentStatus.legalBasisCode!!, consentStatus.allowed ?: false)
                            }
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
            } else {
                Toast.makeText(activity!!, "Configuration & Identity key should not be blank", Toast.LENGTH_SHORT).show()
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

        fun newInstance(configuration: Configuration): SetConsentStatusFragment =
            SetConsentStatusFragment().apply {
                arguments = Bundle().apply {
                    putString(CONFIG_JSON, Gson().toJson(configuration))
                }
            }
    }
}
