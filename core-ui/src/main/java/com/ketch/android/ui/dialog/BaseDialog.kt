package com.ketch.android.ui.dialog

import android.app.Dialog
import android.content.Context
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.whenStateAtLeast
import com.ketch.android.api.response.Consent
import com.ketch.android.api.response.FullConfiguration
import com.ketch.android.ui.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Base Ketch Dialog
 */
internal abstract class BaseDialog(
    context: Context,
    protected val configuration: FullConfiguration,
    consent: Consent
) : Dialog(context, R.style.FullScreenTheme) {
    private val lifecycleOwner by lazy { DialogLifecycleOwner() }

    private val scope = CoroutineScope(Dispatchers.Main)

    protected val consent = consent.copy()

    init {
        if (consent.purposes.isNullOrEmpty()) {
            consent.vendors = configuration.vendors?.map {
                it.id
            }
        }

        consent.purposes = configuration.purposes?.associate {
            val requiresDisplay = it.requiresDisplay == true
            val enabled = it.allowsOptOut == true
            val requiresOptIn = it.requiresOptIn == true

            val accepted = !requiresDisplay ||
                    !enabled ||
                    consent.purposes?.get(it.code)?.toBoolean() ?: !requiresOptIn
            it.code to accepted.toString()
        }
    }

    override fun onStart() {
        super.onStart()
        lifecycleOwner.start()
    }

    override fun onStop() {
        lifecycleOwner.stop()
        super.onStop()
    }

    protected fun <A> collectState(
        state: StateFlow<A>,
        minState: Lifecycle.State = Lifecycle.State.STARTED,
        collector: suspend (A) -> Unit
    ) = state.collectLifecycle(minState, collector)

    private fun <T> Flow<T>.collectLifecycle(
        minState: Lifecycle.State,
        collector: suspend (T) -> Unit
    ) {
        scope.launch {
            lifecycleOwner.lifecycle.whenStateAtLeast(minState) {
                collect {
                    collector(it)
                }
            }
        }
    }
}