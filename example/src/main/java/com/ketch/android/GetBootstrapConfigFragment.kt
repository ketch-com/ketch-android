package com.ketch.android

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.gson.GsonBuilder
import com.ketch.android.api.Result
import com.ketch.android.api.model.BootstrapConfiguration
import kotlinx.android.synthetic.main.fragment_boot_config.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

//class GetBootstrapConfigFragment : BaseFragment() {
//
//    private var repositoryProvider: RepositoryProvider? = null
//    private var bootConfig: BootstrapConfiguration? = null
//
//    override fun onAttach(context: Context) {
//        super.onAttach(context)
//        if (context is RepositoryProvider) {
//            repositoryProvider = context
//        }
//    }
//
//    override fun onCreateView(
//        inflater: LayoutInflater,
//        container: ViewGroup?,
//        savedInstanceState: Bundle?
//    ): View? {
//        return inflater.inflate(R.layout.fragment_boot_config, container, false);
//    }
//
//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//
//        setupActionBar("Step2. Get BootstrapConfig", true)
//
//        arguments?.getString(ORGANIZATION_CODE)?.let {
//            organizationCodeText.text = it
//        }
//        arguments?.getString(APPLICATION_CODE)?.let {
//            applicationCodeText.text = it
//        }
//
//        getConfig.setOnClickListener {
//            (requireActivity() as MainActivity).addFragment(
//                GetFullConfigFragment.newInstance(
//                    bootConfig!!
//                )
//            )
//        }
//
//        getBootConfig.setOnClickListener {
//            getConfig.visibility = View.GONE
//            bootstrapConfig.text = ""
//            job = CoroutineScope(Dispatchers.Main).launch {
//                repositoryProvider?.getRepository()?.getBootstrapConfiguration()
//                    ?.collect { result ->
//                        when (result) {
//                            is Result.Success -> {
//                                bootConfig = result.value
//                                getConfig.visibility = View.VISIBLE
//                                bootstrapConfig.text =
//                                    "${GsonBuilder().setPrettyPrinting().create().toJson(result.value)}\n"
//                            }
//                            is Result.Error -> {
//                                bootstrapConfig.text =
//                                    GsonBuilder().setPrettyPrinting().create().toJson(result.error)
//                            }
//                        }
//                    }
//            }
//        }
//    }
//
//    companion object {
//        const val ORGANIZATION_CODE = "organizationCode"
//        const val APPLICATION_CODE = "applicationCode"
//
//        fun newInstance(orgCode: String, appCode: String): GetBootstrapConfigFragment =
//            GetBootstrapConfigFragment().apply {
//                arguments = Bundle().apply {
//                    putString(ORGANIZATION_CODE, orgCode)
//                    putString(APPLICATION_CODE, appCode)
//                }
//            }
//    }
//}
