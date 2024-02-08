package com.ketch.android.ui

import android.app.Dialog
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
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.ketch.android.Ketch
import com.ketch.android.R
import com.ketch.android.databinding.KetchDialogLayoutBinding

class KetchDialogFragment() : DialogFragment() {

    private lateinit var binding: KetchDialogLayoutBinding

    private var windowPosition: Ketch.WindowPosition? = null

    private var webView: KetchWebView? = null

    init {
        isCancelable = true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            windowPosition = it.getSerializable(POSITION_KEY) as? Ketch.WindowPosition
        }
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
            binding.root.addView(
                web,
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }
        return binding.root
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        webView?.let {
            val bundle = Bundle()
            it.saveState(bundle)
            outState.putBundle("webViewState", bundle)
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
        }
        return dialog
    }

    override fun onDestroyView() {
        binding.root.removeView(webView)
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
                gravity = windowPosition?.gravity ?: Gravity.CENTER
            }

            window.attributes = params

            windowPosition?.let {
                window.setWindowAnimations(it.animId)
            }
        }
    }

    fun show(manager: FragmentManager, webView: KetchWebView) {
        this.webView = webView
        super.show(manager, TAG)
    }

    fun changeDialog(windowPosition: Ketch.WindowPosition) {
        this@KetchDialogFragment.windowPosition = windowPosition

        val args = arguments?.apply {
            putSerializable(POSITION_KEY, windowPosition)
        }

        arguments = args

        dialog?.window?.also { window ->
            val params = window.attributes.apply {
                gravity = windowPosition.gravity
            }
            window.attributes = params
            window.setWindowAnimations(windowPosition.animId)
        }
    }

    companion object {
        internal val TAG = KetchDialogFragment::class.java.simpleName

        private const val POSITION_KEY = "position"

        fun newInstance(
            windowPosition: Ketch.WindowPosition? = null
        ): KetchDialogFragment {
            val fragment = KetchDialogFragment()
            val args = Bundle().apply {
                windowPosition?.let {
                    putSerializable(POSITION_KEY, it)
                }
            }

            fragment.arguments = args
            return fragment
        }
    }
}