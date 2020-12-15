package com.ketch.android

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import com.google.android.material.snackbar.Snackbar
import com.ketch.android.cache.SharedPreferencesCacheProvider
import com.ketch.android.repository.KetchRepository
import kotlinx.android.synthetic.main.fragment_setup.*

class SetupFragment : BaseFragment() {

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
        return inflater.inflate(R.layout.fragment_setup, container, false);
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupActionBar("Step1. Setup SDK", false)

        applicationCodeText.apply {
            setOnEditorActionListener { textView, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    setupRepository()
                    true
                } else {
                    false
                }
            }
        }
        setup.setOnClickListener {
            setupRepository()
        }
    }

    private fun setupRepository() {
        if (organizationCodeText.text.toString().isBlank() || applicationCodeText.text.toString().isBlank()) {
            Snackbar.make(parent, "Fill codes", Snackbar.LENGTH_SHORT)
            return
        }
        repositoryProvider?.setRepository(
            KetchRepository.Builder()
                .organizationCode(organizationCodeText.text.toString())
                .applicationCode(applicationCodeText.text.toString())
                .context(requireContext())
                .cacheProvider(SharedPreferencesCacheProvider(requireContext()))
                .build()
        )

        (requireActivity() as MainActivity).addFragment(
            GetFullConfigFragment.newInstance()
        )
    }

    companion object {
        fun newInstance() = SetupFragment()
    }
}
