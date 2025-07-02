package com.ketch.android.ui

import android.app.Dialog
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND
import android.widget.FrameLayout
import androidx.core.graphics.drawable.toDrawable
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager

internal class KetchDialogFragment : DialogFragment() {

    private var webView: KetchWebView? = null
    private var onDismissCallback: (() -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = webView ?: FrameLayout(requireContext())

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
        super.onCreateDialog(savedInstanceState).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
        }

    override fun onDetach() {
        super.onDetach()

        try {
            webView?.kill()
            webView = null
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up resources: ${e.message}")
        }

        onDismissCallback?.invoke()
        onDismissCallback = null
    }

    override fun dismiss() {
        try {
            super.dismiss()
        } catch (e: Exception) {
            Log.e(TAG, "Error during dismiss: ${e.message}")
            try {
                dismissAllowingStateLoss()
            } catch (e2: Exception) {
                Log.e(TAG, "Error during fallback dismissAllowingStateLoss: ${e2.message}")
                onDismissCallback?.invoke()
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dialog?.window?.apply {
            clearFlags(FLAG_DIM_BEHIND)
            setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
            applyFullScreenSize()
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        dialog?.window?.applyFullScreenSize()
    }

    fun show(manager: FragmentManager) {
        try {
            // Check for any existing fragment with the same tag and remove it
            val existingFragment = manager.findFragmentByTag(TAG)
            if (existingFragment != null) {
                try {
                    Log.d(TAG, "Found existing fragment, removing it first")
                    val transaction = manager.beginTransaction()
                    transaction.remove(existingFragment)
                    transaction.commitAllowingStateLoss()
                    manager.executePendingTransactions()
                } catch (e: Exception) {
                    Log.e(TAG, "Error removing existing fragment: ${e.message}")
                }
            }

            val transaction = manager.beginTransaction()
            transaction.add(this, TAG)
            transaction.commitAllowingStateLoss()

            // Execute transaction immediately
            try {
                manager.executePendingTransactions()
            } catch (e: Exception) {
                Log.e(TAG, "Error executing pending transactions: ${e.message}")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error showing dialog: ${e.message}")
            onDismissCallback?.invoke()
        }
    }

    private fun Window.applyFullScreenSize() = attributes.apply {
        width = ViewGroup.LayoutParams.MATCH_PARENT
        height = ViewGroup.LayoutParams.MATCH_PARENT
        gravity = Gravity.CENTER
    }

    companion object {
        internal val TAG = KetchDialogFragment::class.java.simpleName

        fun newInstance(ketchWebView: KetchWebView, onDismiss: () -> Unit): KetchDialogFragment =
            KetchDialogFragment().apply {
                webView = ketchWebView
                onDismissCallback = onDismiss
            }
    }
}
