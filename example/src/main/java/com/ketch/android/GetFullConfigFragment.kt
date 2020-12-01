package com.ketch.android

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.ketch.android.api.Result
import com.ketch.android.api.model.BootstrapConfiguration
import com.ketch.android.api.model.Configuration
import kotlinx.android.synthetic.main.fragment_full_config.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import java.util.*

class GetFullConfigFragment : BaseFragment() {

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
        return inflater.inflate(R.layout.fragment_full_config, container, false);
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupActionBar("Step3. Get FullConfig", true)

        var bootConfig: BootstrapConfiguration? = null
        arguments?.getString(BOOT_JSON)?.let {
            bootConfig = Gson().fromJson(it, BootstrapConfiguration::class.java)
        }

        languageCodeValue.text = Locale.getDefault().toString()

        getFullConfig.setOnClickListener {
            if (environmentText.text.toString().isNotBlank() && bootConfig != null) {
                job = CoroutineScope(Dispatchers.Main).launch {
                    repositoryProvider?.getRepository()?.getFullConfiguration(
                        configuration = bootConfig!!,
                        environment = environmentText.text.toString(),
                        languageCode = languageCodeValue.text.toString()
                    )
                        ?.collect { result ->
                            when (result) {
                                is Result.Success -> {
                                    config = result.value
                                    moreOptionsGroup.visibility = View.VISIBLE
                                    fullConfig.text =
                                        "${GsonBuilder().setPrettyPrinting().create().toJson(result.value)}\n"
                                }
                                is Result.Error -> {
                                    fullConfig.text = GsonBuilder().setPrettyPrinting().create()
                                        .toJson(result.error)
                                }
                            }
                        }
                }
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
                    SetConsentStatusFragment.newInstance(
                        config!!
                    )
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
        const val BOOT_JSON = "bootJson"

        fun newInstance(bootstrapConfiguration: BootstrapConfiguration): GetFullConfigFragment =
            GetFullConfigFragment().apply {
                arguments = Bundle().apply {
                    putString(BOOT_JSON, Gson().toJson(bootstrapConfiguration))
                }
            }
    }
}
