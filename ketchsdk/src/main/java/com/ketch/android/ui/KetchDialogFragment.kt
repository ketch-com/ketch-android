package com.ketch.android.ui

import android.app.Dialog
import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND
import android.widget.FrameLayout
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.ketch.android.R
import com.ketch.android.databinding.KetchDialogLayoutBinding
import java.util.concurrent.atomic.AtomicBoolean

internal class KetchDialogFragment : DialogFragment() {

    private var _binding: KetchDialogLayoutBinding? = null
    private val binding get() = _binding!!

    private var webView: KetchWebView? = null
    
    // Flag to track if we've already cleaned up resources
    private val hasCleanedUp = AtomicBoolean(false)
    // Callback to notify the parent when this fragment is dismissed
    private var onDismissCallback: (() -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = KetchDialogLayoutBinding.bind(inflater.inflate(R.layout.ketch_dialog_layout, container))
        webView?.let { web ->
            try {
                (web.parent as? ViewGroup)?.removeView(web)
                binding.root.addView(
                    web,
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error adding WebView to view hierarchy: ${e.message}")
            }
        }
        return binding.root
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
        }
        return dialog
    }

    override fun onDestroyView() {
        cleanupResources()
        _binding = null
        super.onDestroyView()
    }
    
    override fun onDetach() {
        super.onDetach()
        // Notify parent this fragment is fully detached
        notifyDismissComplete()
    }
    
    override fun onDestroy() {
        cleanupResources()
        super.onDestroy()
    }
    
    override fun dismiss() {
        cleanupResources()
        try {
            super.dismiss()
        } catch (e: Exception) {
            Log.e(TAG, "Error during dismiss: ${e.message}")
            try {
                dismissAllowingStateLoss()
            } catch (e2: Exception) {
                Log.e(TAG, "Error during fallback dismissAllowingStateLoss: ${e2.message}")
                notifyDismissComplete()
            }
        }
    }
    
    override fun dismissAllowingStateLoss() {
        cleanupResources()
        try {
            super.dismissAllowingStateLoss()
        } catch (e: Exception) {
            Log.e(TAG, "Error during dismissAllowingStateLoss: ${e.message}")
            notifyDismissComplete()
        }
    }
    
    // Notify parent this fragment is dismissed
    private fun notifyDismissComplete() {
        onDismissCallback?.invoke()
        // Clear callback to prevent memory leaks
        onDismissCallback = null
    }
    
    private fun cleanupResources() {
        // Only clean up once to avoid double resource cleanup
        if (hasCleanedUp.getAndSet(true)) return
        
        try {
            // If binding is initialized, remove webview from root
            _binding?.let { binding ->
                binding.root.removeView(webView)
            }
            
            // Cancel any coroutines in KetchWebView and fully tear down webview to prevent memory leaks
            webView?.kill()
            
            // Set webview reference to null to prevent memory leaks
            webView = null
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up resources: ${e.message}")
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dialog?.window?.also { window ->
            window.clearFlags(FLAG_DIM_BEHIND)
            window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            val displayMetrics = requireActivity().resources.displayMetrics

            val width = displayMetrics.widthPixels
            val height = displayMetrics.heightPixels

            val params = window.attributes.apply {
                this.width = width
                this.height = height
                gravity = Gravity.CENTER
            }

            window.attributes = params
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        dialog?.window?.also { window ->
            val displayMetrics = requireActivity().resources.displayMetrics

            val width = displayMetrics.widthPixels
            val height = displayMetrics.heightPixels

            val params = window.attributes.apply {
                this.width = width
                this.height = height
                gravity = Gravity.CENTER
            }

            window.attributes = params
        }
    }

    fun show(manager: FragmentManager, webView: KetchWebView, onDismiss: () -> Unit) {
        try {
            this.webView = webView
            this.onDismissCallback = onDismiss
            
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
            
            // Now show this fragment
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
            notifyDismissComplete()
            cleanupResources()
        }
    }

    companion object {
        internal val TAG = KetchDialogFragment::class.java.simpleName

        fun newInstance(): KetchDialogFragment {
            return KetchDialogFragment()
        }
    }
}