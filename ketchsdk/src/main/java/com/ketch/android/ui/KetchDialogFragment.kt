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
            // Remove from any previous parent
            (web.parent as? ViewGroup)?.removeView(web)
            
            // Add to our layout
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
            Log.d(TAG, "onDestroyView: Beginning WebView cleanup")
            
            val webViewToCleanup = webView
            webView = null
            
            webViewToCleanup?.let { wv ->
                // Disable interaction during cleanup
                wv.setOnTouchListener { _, _ -> true }
                
                // Remove from view hierarchy
                (wv.parent as? ViewGroup)?.removeView(wv)
                binding.root.removeView(wv)
                
                // Let standard cleanup handle the rest
                // We don't need to manually destroy the WebView here as it will be
                // handled by the Ketch class
            }
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
        // Check for existing fragments
        try {
            val existingFragment = manager.findFragmentByTag(TAG)
            if (existingFragment != null) {
                manager.beginTransaction()
                    .remove(existingFragment)
                    .commitNowAllowingStateLoss()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up before show: ${e.message}", e)
        }
        
        this.webView = webView
        super.show(manager, TAG)
    }

    override fun dismiss() {
        try {
            // Detach WebView from touch events during dismissal
            webView?.setOnTouchListener { _, _ -> true }
        } catch (e: Exception) {
            Log.e(TAG, "Error in dismiss: ${e.message}", e)
        }
        
        super.dismiss()
    }

    override fun dismissAllowingStateLoss() {
        try {
            // Detach WebView from touch events during dismissal
            webView?.setOnTouchListener { _, _ -> true }
        } catch (e: Exception) {
            Log.e(TAG, "Error in dismissAllowingStateLoss: ${e.message}", e)
        }
        
        super.dismissAllowingStateLoss()
    }

    companion object {
        internal val TAG = KetchDialogFragment::class.java.simpleName
        private var isCurrentlyShowing = false
        
        fun newInstance(): KetchDialogFragment? {
            // Only allow ONE instance at a time
            return if (!isCurrentlyShowing) {
                isCurrentlyShowing = true
                KetchDialogFragment()
            } else {
                Log.w(TAG, "DialogFragment already showing, ignoring request")
                null
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isCurrentlyShowing = false
    }
}