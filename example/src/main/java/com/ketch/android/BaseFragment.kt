package com.ketch.android

import androidx.fragment.app.Fragment
import kotlinx.coroutines.Job

open class BaseFragment : Fragment() {

    protected var job: Job? = null

    protected fun setupActionBar(title: String, showUp: Boolean) {
        (requireActivity() as MainActivity).supportActionBar?.title = title
        (requireActivity() as MainActivity).supportActionBar?.setDisplayHomeAsUpEnabled(showUp)
    }

    override fun onStop() {
        job?.cancel()
        super.onStop()
    }
}
