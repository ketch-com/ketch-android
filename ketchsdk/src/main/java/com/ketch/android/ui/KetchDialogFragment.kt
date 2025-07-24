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
    ): View = webView ?: FrameLayout(requireContext()).apply { dismiss() }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
        super.onCreateDialog(savedInstanceState).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
        }

    override fun onDetach() {
        super.onDetach()

        onDismissCallback?.invoke()
        onDismissCallback = null

        webView = null
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
        // Check for any existing fragment with the same tag and remove it
        (manager.findFragmentByTag(TAG) as? KetchDialogFragment)?.let { existingFragment ->
            try {
                Log.d(TAG, "Found existing fragment, dismissing it first")
                existingFragment.dismiss()
            } catch (e: Exception) {
                Log.e(TAG, "Error dismissing existing fragment: ${e.message}")
            }
        }

        try {
            this.show(manager, TAG)
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
