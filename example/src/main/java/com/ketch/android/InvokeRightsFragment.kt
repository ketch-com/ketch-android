package com.ketch.android

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.ketch.android.api.Result
import com.ketch.android.api.model.Configuration
import com.ketch.android.model.UserData
import kotlinx.android.synthetic.main.fragment_invoke_rights.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class InvokeRightsFragment : BaseFragment() {

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
        return inflater.inflate(R.layout.fragment_invoke_rights, container, false);
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupActionBar("Usage. Invoke Rights", true)

        var config: Configuration? = null
        arguments?.getString(CONFIG_JSON)?.let {
            config = Gson().fromJson(it, Configuration::class.java)
        }

        invokeRights.setOnClickListener {
            if (identityKeyText.text.toString().isNotBlank() && config != null) {
                val rights = listOf(portability, rtbf, wrong_right).filter { it.isChecked }
                    .map {
                        it.text.toString()
                    }
                job = CoroutineScope(Dispatchers.Main).launch {
                    repositoryProvider?.getRepository()?.invokeRights(
                        configuration = config!!,
                        identities = mapOf(identityKeyText.text.toString() to "testValue"),
                        userData = UserData(email = userDataValue.text.toString()),
                        rights = rights
                    )
                        ?.collect { result ->
                            when (result) {
                                is Result.Success -> {
                                    invokeRightsResult.text =
                                        GsonBuilder().setPrettyPrinting().create()
                                            .toJson(JsonObject())
                                }
                                is Result.Error -> {
                                    invokeRightsResult.text =
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

        fun newInstance(configuration: Configuration): InvokeRightsFragment =
            InvokeRightsFragment().apply {
                arguments = Bundle().apply {
                    putString(CONFIG_JSON, Gson().toJson(configuration))
                }
            }
    }
}
