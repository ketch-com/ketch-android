package com.ketch.android

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.ketch.android.api.Result
import com.ketch.android.api.model.BootstrapConfiguration
import com.ketch.android.api.model.Configuration
import com.ketch.android.api.model.ConfigurationV2
import kotlinx.android.synthetic.main.fragment_full_config.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import java.util.*

class GetFullConfigFragment : BaseFragment() {

    private var repositoryProvider: RepositoryProvider? = null
    private var config: ConfigurationV2? = null

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
        return inflater.inflate(R.layout.fragment_full_config, container, false);
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupActionBar("Step2. Get Config", true)

        countryCodeValue.text = Locale.getDefault().country
        languageCodeValue.text = Locale.getDefault().language

        getFullConfig.setOnClickListener {
            if (environmentText.text.toString().isNotBlank()) {
                job = CoroutineScope(Dispatchers.Main).launch {
                    repositoryProvider?.getRepository()?.getConfigurationProto(environmentText.text.toString(),
                        countryCodeValue.text.toString(), languageCodeValue.text.toString())
                        ?.collect { result ->
                            when (result) {
                                is Result.Success -> {
                                    config = result.value
                                    Log.d("<<<", config.toString())
                                    moreOptionsGroup.visibility = View.VISIBLE
                                    fullConfig.text =
                                        "${GsonBuilder().setPrettyPrinting().create().toJson(result.value)}\n"
                                }
                                is Result.Error -> {
                                    fullConfig.text =
                                        GsonBuilder().setPrettyPrinting().create().toJson(result.error)
                                }
                            }
                        }
                }
            } else {
                Toast.makeText(activity!!, "Environment field should not be blank", Toast.LENGTH_SHORT).show()
            }

            getConsentStatus.setOnClickListener {
                (requireActivity() as MainActivity).addFragment(
                    GetConsentStatusFragment.newInstance(
                        config!!
                    )
                )
            }

            setConsentStatus.setOnClickListener {
                (requireActivity() as MainActivity).addFragment(
                    SetConsentStatusFragment.newInstance(config!!)
                )
            }
            invokeRights.setOnClickListener {
                (requireActivity() as MainActivity).addFragment(
                    InvokeRightsFragment.newInstance(
                        config!!
                    )
                )
            }
        }
    }

    companion object {
        const val EXTRA_ENVIRONMENT = "EXTRA_ENVIRONMENT"

        fun newInstance(): GetFullConfigFragment =
            GetFullConfigFragment()
    }
}
