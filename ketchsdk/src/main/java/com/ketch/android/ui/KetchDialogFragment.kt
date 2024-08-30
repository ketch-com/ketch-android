package com.ketch.android.ui

import android.app.Dialog
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND
import android.widget.FrameLayout
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.ketch.android.R
import com.ketch.android.databinding.KetchDialogLayoutBinding

internal class KetchDialogFragment() : DialogFragment() {

    private lateinit var binding: KetchDialogLayoutBinding

    private var webView: KetchWebView? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
        }
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding =
            KetchDialogLayoutBinding.bind(inflater.inflate(R.layout.ketch_dialog_layout, container))

        webView?.let { web ->
            (web.parent as? ViewGroup)?.removeView(web)

            with(binding) {
                root.addView(
                    web,
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
            }
        }
        return binding.root
    }

    override fun onDestroyView() {

        binding.root.removeView(webView)

        // Cancel any coroutines in KetchWebView and fully tear down webview to prevent memory leaks
        webView?.kill()

        // Set webview reference to null to prevent memory leaks
        webView = null
        super.onDestroyView()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dialog?.window?.also { window ->
            setWindow(window)
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        dialog?.window?.also { window ->
            setWindow(window)
        }
    }

    private fun setWindow(window: Window) {
        window.clearFlags(FLAG_DIM_BEHIND)
        window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        WindowCompat.getInsetsController(window,window.decorView).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.statusBars())
            hide(WindowInsetsCompat.Type.navigationBars())
        }

        val windowAttributes = requireActivity().window.attributes

        val params = window.attributes.apply {
            this.width = windowAttributes.width
            this.height = windowAttributes.height
            this.x = windowAttributes.x
            this.x = windowAttributes.y
        }

        window.attributes = params
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