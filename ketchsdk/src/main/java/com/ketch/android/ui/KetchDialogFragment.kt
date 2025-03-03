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
    
    // Define the position property with a default value
    private val position: DialogPosition = DialogPosition.CENTER
    
    // Add DialogPosition enum
    enum class DialogPosition(val gravity: Int) {
        TOP(Gravity.TOP),
        CENTER(Gravity.CENTER),
        BOTTOM(Gravity.BOTTOM)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = KetchDialogLayoutBinding.bind(
            inflater.inflate(R.layout.ketch_dialog_layout, container)
        )
        
        webView?.let { web ->
            // Remove from any previous parent
            (web.parent as? ViewGroup)?.removeView(web)
            
            // Add to our layout with appropriate properties
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
        return super.onCreateDialog(savedInstanceState).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setCanceledOnTouchOutside(isCancelable)
        }
    }

    override fun onDestroyView() {
        try {
            Log.d(TAG, "onDestroyView: Beginning WebView cleanup")
            webView?.let { wv ->
                // Disable interaction during cleanup
                wv.setOnTouchListener { _, _ -> true }
                
                // Remove from view hierarchy
                (wv.parent as? ViewGroup)?.removeView(wv)
                binding.root.removeView(wv)
            }
            webView = null
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
        }
        updateDialogSize()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        updateDialogSize()
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.let { window ->
            window.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            window.setGravity(position.gravity)
            window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
    }

    fun show(manager: FragmentManager, webView: KetchWebView) {
        if (!isAdded) {
            try {
                // Check for existing fragments
                manager.findFragmentByTag(TAG)?.let { existingFragment ->
                    manager.beginTransaction()
                        .remove(existingFragment)
                        .commitNowAllowingStateLoss()
                }
                
                this.webView = webView
                super.show(manager, TAG)
            } catch (e: Exception) {
                Log.e(TAG, "Error showing dialog: ${e.message}", e)
                isCurrentlyShowing = false  // Reset flag if we failed to show
            }
        }
    }

    override fun dismiss() {
        prepareForDismissal()
        super.dismiss()
    }

    override fun dismissAllowingStateLoss() {
        prepareForDismissal()
        super.dismissAllowingStateLoss()
    }

    override fun onDestroy() {
        super.onDestroy()
        isCurrentlyShowing = false
    }

    // Helper methods

    private fun updateDialogSize() {
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

    private fun prepareForDismissal() {
        try {
            // Detach WebView from touch events during dismissal
            webView?.setOnTouchListener { _, _ -> true }
        } catch (e: Exception) {
            Log.e(TAG, "Error preparing for dismissal: ${e.message}", e)
        }
    }

    companion object {
        internal val TAG = KetchDialogFragment::class.java.simpleName
        @Volatile private var isCurrentlyShowing = false
        
        @Synchronized
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
}