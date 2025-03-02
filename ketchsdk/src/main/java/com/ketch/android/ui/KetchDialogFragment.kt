package com.ketch.android.ui

import android.app.Dialog
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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

internal class KetchDialogFragment() : DialogFragment() {

    private lateinit var binding: KetchDialogLayoutBinding

    private var webView: KetchWebView? = null

    // Add a flag to track if we're in the process of cleaning up
    private var isCleaningUp = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding =
            KetchDialogLayoutBinding.bind(inflater.inflate(R.layout.ketch_dialog_layout, container))
        webView?.let { web ->
            // Make sure the WebView is detached from any previous parent
            (web.parent as? ViewGroup)?.removeView(web)
            
            // Reset WebView to a clean state
            web.clearFocus()
            web.setOnTouchListener(null) // Remove any touch blockers
            
            // Add the WebView to our layout
            binding.root.addView(
                web,
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            
            // Ensure WebView is interactive
            web.isClickable = true
            web.isFocusable = true
            web.isFocusableInTouchMode = true
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
        try {
            isCleaningUp = true
            
            Log.d(TAG, "onDestroyView: Beginning WebView cleanup")
            
            // Get a local reference to the WebView before nulling it out
            val webViewToCleanup = webView
            
            // Set the class reference to null first to prevent any new operations from being triggered
            webView = null
            
            // Perform cleanup on the local reference if it exists
            webViewToCleanup?.let { wv ->
                // Prevent any new touch events or interactions
                wv.setOnTouchListener { _, _ -> true }
                
                try {
                    // Execute JavaScript to clean up event listeners
                    wv.evaluateJavascript(
                        """
                        (function() {
                            // Remove all event listeners from document and window
                            var oldElement = document.documentElement;
                            var newElement = oldElement.cloneNode(true);
                            oldElement.parentNode.replaceChild(newElement, oldElement);
                            
                            // Disable interaction with any content
                            document.body.style.pointerEvents = 'none';
                        })();
                        """,
                        null
                    )
                } catch (e: Exception) {
                    // Ignore JavaScript errors during cleanup
                    Log.e(TAG, "Error disabling JS events: ${e.message}")
                }
                
                // Immediately remove from view hierarchy
                try {
                    (wv.parent as? ViewGroup)?.removeView(wv)
                    binding.root.removeView(wv)
                    Log.d(TAG, "onDestroyView: WebView immediately removed from view hierarchy")
                } catch (e: Exception) {
                    Log.e(TAG, "Error removing WebView from view hierarchy: ${e.message}", e)
                }
                
                // Additional delayed cleanup to ensure proper UI thread operations
                Handler(Looper.getMainLooper()).post {
                    try {
                        // Force another removal attempt in case the immediate one failed
                        (wv.parent as? ViewGroup)?.removeView(wv)
                        binding.root.removeView(wv)
                        
                        // Explicitly invalidate the WebView
                        wv.clearHistory()
                        wv.clearFormData()
                        wv.clearCache(true)
                        
                        Log.d(TAG, "onDestroyView: WebView additional cleanup completed")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error in delayed WebView cleanup: ${e.message}", e)
                    }
                }
            }
            
            Log.d(TAG, "onDestroyView: WebView cleanup initiated")
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up WebView: ${e.message}", e)
        }
        
        super.onDestroyView()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dialog?.window?.also { window ->
            // Keep background transparent to allow interaction with WebView content
            window.clearFlags(FLAG_DIM_BEHIND)
            window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            
            // Ensure dialog takes full screen
            val displayMetrics = requireActivity().resources.displayMetrics
            val width = displayMetrics.widthPixels
            val height = displayMetrics.heightPixels

            val params = window.attributes.apply {
                this.width = width
                this.height = height
                gravity = Gravity.CENTER
            }

            window.attributes = params
            
            // Add window animations for smoother transitions
            window.setWindowAnimations(android.R.style.Animation_Dialog)
        }
        
        // Don't set background on root view so content remains fully interactive
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

    fun show(manager: FragmentManager, webView: KetchWebView) {
        // Reset the cleanup flag
        isCleaningUp = false
        
        // First check if there are any existing fragments with our tag and remove them
        try {
            val existingFragment = manager.findFragmentByTag(TAG)
            if (existingFragment != null) {
                Log.d(TAG, "Found existing fragment with same tag - removing it first")
                manager.beginTransaction()
                    .remove(existingFragment)
                    .commitNowAllowingStateLoss()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up before show: ${e.message}", e)
        }
        
        // Store the WebView reference
        this.webView = webView
        
        // Ensure WebView is in a good state for display
        webView.clearFocus()
        webView.setOnTouchListener(null) // Remove any touch blockers
        webView.isClickable = true
        webView.isFocusable = true
        
        // Now show the dialog
        super.show(manager, TAG)
    }

    override fun dismiss() {
        try {
            // Set cleanup flag first
            isCleaningUp = true
            
            // Detach the WebView from touch events before dismissing
            webView?.setOnTouchListener { _, _ -> true }
        } catch (e: Exception) {
            Log.e(TAG, "Error in dismiss pre-cleanup: ${e.message}", e)
        }
        
        super.dismiss()
    }

    override fun dismissAllowingStateLoss() {
        try {
            // Set cleanup flag first
            isCleaningUp = true
            
            // Detach the WebView from touch events before dismissing
            webView?.setOnTouchListener { _, _ -> true }
        } catch (e: Exception) {
            Log.e(TAG, "Error in dismissAllowingStateLoss pre-cleanup: ${e.message}", e)
        }
        
        super.dismissAllowingStateLoss()
    }

    companion object {
        internal val TAG = KetchDialogFragment::class.java.simpleName

        fun newInstance(): KetchDialogFragment {
            return KetchDialogFragment()
        }
    }
}