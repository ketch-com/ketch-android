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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding =
            KetchDialogLayoutBinding.bind(inflater.inflate(R.layout.ketch_dialog_layout, container))
        webView?.let { web ->
            (web.parent as? ViewGroup)?.removeView(web)
            binding.root.addView(
                web,
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
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
                    // Prevent any JavaScript execution during cleanup
                    wv.evaluateJavascript(
                        "document.body.style.pointerEvents = 'none';" +
                        "document.body.removeEventListener('touchstart', handleTapOutside);" +
                        "document.body.removeEventListener('mousedown', handleTapOutside);",
                        null
                    )
                } catch (e: Exception) {
                    // Ignore JavaScript errors during cleanup
                    Log.e(TAG, "Error disabling JS events: ${e.message}")
                }
                
                // Wait a moment for any pending events to clear
                Handler(Looper.getMainLooper()).post {
                    try {
                        // Remove from view hierarchy 
                        binding.root.removeView(wv)
                        
                        // Clean up the WebView
                        wv.destroy()
                        
                        // Suggest garbage collection
                        Runtime.getRuntime().gc()
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

    fun show(manager: FragmentManager, webView: KetchWebView) {
        this.webView = webView
        super.show(manager, TAG)
    }

    companion object {
        internal val TAG = KetchDialogFragment::class.java.simpleName

        fun newInstance(): KetchDialogFragment {
            return KetchDialogFragment()
        }
    }
}