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
import com.ketch.android.api.model.IdentitySpace
import com.ketch.android.example.R
import com.ketch.android.model.UserData
import kotlinx.android.synthetic.main.fragment_invoke_rights.*
import kotlinx.android.synthetic.main.fragment_invoke_rights.identityKeyText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class InvokeRightsFragment : BaseFragment() {

    private var repositoryProvider: RepositoryProvider? = null

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

        right1.text = config!!.rights?.get(0)?.code ?: ""
        right2.text = config!!.rights?.get(1)?.code ?: ""
        right3.text = config!!.rights?.get(2)?.code ?: ""

        val userData = UserData(
            first = "Foo",
            last = "Bar",
            country = "US",
            region = "CA",
            email = "someone@ketch.com"
        )

        userDataValue.text =
            """First Name: ${userData.first}
            |Last name: ${userData.last}
            |Country: ${userData.country}
            |Region: ${userData.region}
            |Email: ${userData.email}""".trimMargin()

        invokeRights.setOnClickListener {
            if (identityKeyText.text.toString().isNotBlank() && config != null) {
                val rights = listOf(right1, right2, right3).filter { it.isChecked }
                    .map {
                        it.text.toString()
                    }
                val identities: List<IdentitySpace> = config!!.identities!!.map {
                    IdentitySpace(it.key, identityKeyText.text.toString())
                }
                job = CoroutineScope(Dispatchers.Main).launch {
                    repositoryProvider?.getRepository()?.invokeRights(
                        configuration = config!!,
                        identities = identities,
                        userData = userData,
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
