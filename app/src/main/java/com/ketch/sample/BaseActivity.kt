package com.ketch.sample

import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.whenStateAtLeast
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

abstract class BaseActivity : AppCompatActivity() {
    protected fun <A> collectState(
        state: StateFlow<A>,
        minState: Lifecycle.State = Lifecycle.State.STARTED,
        collector: suspend (A) -> Unit
    ) = state.collectLifecycle(minState, collector)

    protected fun <T> Flow<T>.collectLifecycle(
        minState: Lifecycle.State = Lifecycle.State.STARTED,
        collector: suspend (T) -> Unit
    ) {
        lifecycleScope.launch {
            lifecycle.whenStateAtLeast(minState) {
                collect {
                    collector(it)
                }
            }
        }
    }
}