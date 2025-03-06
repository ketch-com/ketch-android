package com.ketch.android.ui

import android.app.Dialog
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
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

internal class KetchDialogFragment() : DialogFragment() {

    private lateinit var binding: KetchDialogLayoutBinding

    // Change to internal access to allow direct manipulation from Ketch.kt
    internal var webView: KetchWebView? = null
    
    // Keep track of whether this instance is being destroyed
    private var isBeingDestroyed = false
    
    // Add a safety touch blocker for the main view
    private val transparentTouchListener = View.OnTouchListener { _, _ -> 
        // Return false to NOT block touches
        false 
    }
    
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
        
        // Ensure the root view is transparent
        binding.root.setBackgroundColor(Color.TRANSPARENT)
        
        webView?.let { web ->
            // Remove from any previous parent
            (web.parent as? ViewGroup)?.removeView(web)
            
            // Add to our layout with appropriate properties
            binding.root.addView(
                web,
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            
            // Ensure WebView is interactive and transparent
            web.isClickable = true
            web.isFocusable = true
            web.isFocusableInTouchMode = true
            web.setBackgroundColor(Color.TRANSPARENT)
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
            
            // Mark as being destroyed to prevent reuse
            isBeingDestroyed = true
            
            // Set the transparent touch listener on the root view
            binding.root.setOnTouchListener(transparentTouchListener)
            
            // Make sure the dialog window is not capturing touches
            dialog?.window?.let { window ->
                try {
                    // Clear any flags that might interfere with touch events
                    window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
                    
                    // Reset any touch interceptors
                    val decorView = window.decorView
                    decorView.setOnTouchListener(null)
                } catch (e: Exception) {
                    Log.e(TAG, "Error resetting window touch properties: ${e.message}", e)
                }
            }
            
            webView?.let { wv ->
                // IMPORTANT: Set touch listener to null instead of blocking touches
                wv.setOnTouchListener(null)
                
                // Stop any ongoing loads
                wv.stopLoading()
                
                // Disable hardware acceleration
                try {
                    wv.setLayerType(View.LAYER_TYPE_NONE, null)
                } catch (e: Exception) {
                    Log.e(TAG, "Error disabling hardware acceleration: ${e.message}", e)
                }
                
                // Clear content
                try {
                    wv.loadUrl("about:blank")
                } catch (e: Exception) {
                    Log.e(TAG, "Error loading blank page: ${e.message}", e)
                }
                
                // Remove from view hierarchy
                try {
                    (wv.parent as? ViewGroup)?.removeView(wv)
                } catch (e: Exception) {
                    Log.e(TAG, "Error removing WebView from parent: ${e.message}", e)
                }
                
                binding.root.removeView(wv)
                
                // Destroy the WebView
                wv.destroy()
                
                // Clear the reference
                webView = null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up WebView: ${e.message}", e)
            // Even if we fail, try to ensure WebView is not blocking touches
            webView?.setOnTouchListener(null)
        } finally {
            // Always reset the showing state when view is destroyed
            isCurrentlyShowing.set(false)
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
            
            // Ensure complete transparency
            window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            window.clearFlags(FLAG_DIM_BEHIND)
            
            // Make sure the window has a transparent background
            val attributes = window.attributes
            attributes.dimAmount = 0f
            
            // Ensure we don't interfere with app touches after close
            attributes.flags = attributes.flags or android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
            
            window.attributes = attributes
        }
    }

    fun show(manager: FragmentManager, webView: KetchWebView) {
        if (manager.isDestroyed) {
            Log.e(TAG, "Cannot show dialog: FragmentManager is destroyed")
            isCurrentlyShowing.set(false)
            return
        }
        
        if (!isAdded) {
            try {
                // Use synchronized block to prevent race conditions
                synchronized(LOCK) {
                    if (!isCurrentlyShowing.get()) {
                        isCurrentlyShowing.set(true)
                        
                        // Check for existing fragments and remove them
                        val existingFragments = manager.fragments.filterIsInstance<KetchDialogFragment>()
                        if (existingFragments.isNotEmpty()) {
                            try {
                                // Remove all existing fragments
                                val transaction = manager.beginTransaction()
                                existingFragments.forEach { fragment ->
                                    try {
                                        if (fragment is DialogFragment) {
                                            fragment.dismissAllowingStateLoss()
                                        }
                                        transaction.remove(fragment)
                                    } catch (e: Exception) {
                                        Log.e(TAG, "Error removing existing fragment: ${e.message}", e)
                                    }
                                }
                                transaction.commitNowAllowingStateLoss()
                                
                                // Small delay to ensure fragments are fully removed
                                Handler(Looper.getMainLooper()).postDelayed({
                                    this.webView = webView
                                    super.show(manager, TAG)
                                }, 200)
                            } catch (e: Exception) {
                                Log.e(TAG, "Error removing existing fragments: ${e.message}", e)
                                isCurrentlyShowing.set(false)
                            }
                        } else {
                            // No existing fragment found, show directly
                            this.webView = webView
                            super.show(manager, TAG)
                        }
                    } else {
                        Log.w(TAG, "Dialog already showing, ignoring request")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error showing dialog: ${e.message}", e)
                isCurrentlyShowing.set(false)
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
        isCurrentlyShowing.set(false)
        
        // Ensure WebView is properly destroyed
        webView?.let { wv ->
            try {
                // Disable interaction during cleanup
                wv.setOnTouchListener(null)
                
                // Destroy the WebView
                wv.destroy()
                
                // Clear the reference
                webView = null
            } catch (e: Exception) {
                Log.e(TAG, "Error destroying WebView in onDestroy: ${e.message}", e)
            }
        }
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
            Log.d(TAG, "prepareForDismissal: Beginning cleanup")
            
            // Mark as being destroyed to prevent reuse
            isBeingDestroyed = true
            
            // Set the transparent touch listener on the root view
            binding.root.setOnTouchListener(transparentTouchListener)
            
            // CRITICAL: Reset touch listener to null first
            webView?.setOnTouchListener(null)
            
            // Stop any ongoing loads
            webView?.stopLoading()
            
            // Clear content
            try {
                webView?.loadUrl("about:blank")
            } catch (e: Exception) {
                Log.e(TAG, "Error loading blank page: ${e.message}", e)
            }
            
            // Ensure we reset the showing flag
            isCurrentlyShowing.set(false)
            
            // Remove WebView from parent
            try {
                (webView?.parent as? ViewGroup)?.removeView(webView)
            } catch (e: Exception) {
                Log.e(TAG, "Error removing WebView from parent: ${e.message}", e)
            }
            
            // Destroy the WebView
            webView?.destroy()
            webView = null
            
            // Force a reset of the showing state
            resetShowingState()
        } catch (e: Exception) {
            Log.e(TAG, "Error preparing for dismissal: ${e.message}", e)
            // Even if there's an error, ensure we reset the state
            isCurrentlyShowing.set(false)
        } finally {
            // Final safety check to ensure touch events aren't blocked
            try {
                webView?.setOnTouchListener(null)
            } catch (e: Exception) {
                Log.e(TAG, "Final touch reset failed: ${e.message}", e)
            }
        }
    }

    companion object {
        internal val TAG = KetchDialogFragment::class.java.simpleName
        private val isCurrentlyShowing = AtomicBoolean(false)
        private val LOCK = Any()
        
        // Add a debounce mechanism
        private var lastShowTime = 0L
        private const val SHOW_DEBOUNCE_TIME = 1000L // 1 second between allowed shows
        
        @Synchronized
        fun newInstance(): KetchDialogFragment? {
            // Debounce rapid creation attempts
            val currentTime = SystemClock.elapsedRealtime()
            if (currentTime - lastShowTime < SHOW_DEBOUNCE_TIME) {
                Log.d(TAG, "Ignoring rapid fragment creation, debouncing for ${SHOW_DEBOUNCE_TIME}ms")
                return null
            }
            lastShowTime = currentTime
            
            // Only allow ONE instance at a time
            return if (!isCurrentlyShowing.get()) {
                isCurrentlyShowing.set(true)
                KetchDialogFragment()
            } else {
                Log.w(TAG, "DialogFragment already showing, ignoring request")
                null
            }
        }
        
        /**
         * Force reset the showing state - should only be used in emergency cleanup situations
         */
        @Synchronized
        fun resetShowingState() {
            isCurrentlyShowing.set(false)
        }
        
        /**
         * Force cleanup all KetchDialogFragment instances from a FragmentManager
         */
        @Synchronized
        fun forceCleanupAllInstances(fragmentManager: FragmentManager) {
            if (fragmentManager.isDestroyed) {
                Log.e(TAG, "Cannot cleanup: FragmentManager is destroyed")
                isCurrentlyShowing.set(false)
                return
            }
            
            try {
                val existingFragments = fragmentManager.fragments.filterIsInstance<KetchDialogFragment>()
                if (existingFragments.isNotEmpty()) {
                    Log.d(TAG, "Force cleaning up ${existingFragments.size} dialog fragments")
                    
                    // First reset all touch listeners and clean up WebViews
                    existingFragments.forEach { fragment ->
                        try {
                            // Mark as being destroyed
                            fragment.isBeingDestroyed = true
                            
                            // Ensure root view doesn't block touches
                            if (::binding.isInitialized) {
                                fragment.binding.root.setOnTouchListener(fragment.transparentTouchListener)
                            }
                            
                            // Ensure WebView is properly destroyed
                            fragment.webView?.let { wv ->
                                try {
                                    // Reset touch listener first to prevent blocking touches
                                    wv.setOnTouchListener(null)
                                    
                                    // Stop any loading
                                    wv.stopLoading()
                                    
                                    // Clear content
                                    wv.loadUrl("about:blank")
                                    
                                    // Remove from parent
                                    (wv.parent as? ViewGroup)?.removeView(wv)
                                    
                                    // Destroy the WebView
                                    wv.destroy()
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error destroying WebView during cleanup: ${e.message}", e)
                                }
                            }
                            
                            // Clear WebView reference
                            fragment.webView = null
                        } catch (e: Exception) {
                            Log.e(TAG, "Error cleaning up fragment WebView: ${e.message}", e)
                        }
                    }
                    
                    // Directly remove them in a transaction without dismissing first
                    // This is more thorough than dismissAllowingStateLoss()
                    val transaction = fragmentManager.beginTransaction()
                    existingFragments.forEach { fragment ->
                        try {
                            // Detach first to ensure view is removed from UI
                            transaction.detach(fragment)
                            transaction.remove(fragment)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error removing fragment: ${e.message}", e)
                        }
                    }
                    transaction.commitNowAllowingStateLoss()
                    
                    // Force immediate execution
                    try {
                        fragmentManager.executePendingTransactions()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error executing pending transactions: ${e.message}", e)
                    }
                    
                    // Reset the showing state
                    isCurrentlyShowing.set(false)
                    
                    // Add a small delay to ensure the UI thread has time to process the changes
                    Handler(Looper.getMainLooper()).postDelayed({
                        // Double-check that all fragments are gone
                        val remainingFragments = fragmentManager.fragments.filterIsInstance<KetchDialogFragment>()
                        if (remainingFragments.isNotEmpty()) {
                            Log.w(TAG, "Found ${remainingFragments.size} remaining fragments after cleanup, forcing removal")
                            try {
                                // One more attempt with detach-then-remove approach
                                val finalTransaction = fragmentManager.beginTransaction()
                                remainingFragments.forEach { fragment ->
                                    // Set touch listeners to null to be extra safe
                                    fragment.webView?.setOnTouchListener(null)
                                    if (::binding.isInitialized) {
                                        fragment.binding.root.setOnTouchListener(fragment.transparentTouchListener)
                                    }
                                    
                                    // Detach first, then remove
                                    finalTransaction.detach(fragment)
                                    finalTransaction.remove(fragment)
                                }
                                finalTransaction.commitNowAllowingStateLoss()
                                fragmentManager.executePendingTransactions()
                                
                                // One final check for any stubborn fragments
                                Handler(Looper.getMainLooper()).postDelayed({
                                    val stubbornFragments = fragmentManager.fragments.filterIsInstance<KetchDialogFragment>()
                                    if (stubbornFragments.isNotEmpty()) {
                                        Log.e(TAG, "CRITICAL: Still found fragments after multiple cleanup attempts!")
                                        
                                        // Last-ditch attempt with individual transactions
                                        stubbornFragments.forEach { fragment ->
                                            try {
                                                val emergencyTransaction = fragmentManager.beginTransaction()
                                                emergencyTransaction.detach(fragment)
                                                emergencyTransaction.remove(fragment)
                                                emergencyTransaction.commitNowAllowingStateLoss()
                                                fragmentManager.executePendingTransactions()
                                            } catch (e: Exception) {
                                                Log.e(TAG, "Error in emergency fragment removal: ${e.message}", e)
                                            }
                                        }
                                    }
                                    
                                    // Suggest garbage collection
                                    System.gc()
                                }, 200)
                            } catch (e: Exception) {
                                Log.e(TAG, "Error in final fragment cleanup: ${e.message}", e)
                            }
                        }
                        
                        // Ensure the showing state is reset
                        isCurrentlyShowing.set(false)
                    }, 500) // Increased delay for thorough cleanup
                } else {
                    // No fragments to clean up, just reset the state
                    isCurrentlyShowing.set(false)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error during force cleanup: ${e.message}", e)
                isCurrentlyShowing.set(false)
            }
        }
    }
}