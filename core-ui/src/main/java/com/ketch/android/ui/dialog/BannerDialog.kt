package com.ketch.android.ui.dialog

import android.content.Context
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import androidx.core.view.isVisible
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.ketch.android.api.response.FullConfiguration
import com.ketch.android.ui.databinding.BannerBinding
import com.ketch.android.ui.extension.MarkdownUtils
import com.ketch.android.ui.extension.poweredByKetch
import com.ketch.android.ui.theme.ColorTheme

/**
 * Banner Dialog
 *
 * @param context - [android.content.Context]
 * @param configuration - [com.ketch.android.api.response.FullConfiguration]
 * @param listener - [com.ketch.android.ui.dialog.BannerDialog.BannerDialogListener]
 */
internal class BannerDialog(
    context: Context,
    private val configuration: FullConfiguration,
    private val listener: BannerDialogListener,
) : BottomSheetDialog(context) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding = BannerBinding.inflate(LayoutInflater.from(context))

        configuration.experiences?.consentExperience?.banner?.let { banner ->
            binding.theme = configuration.theme?.let {
                ColorTheme.bannerColorTheme(it)
            }

            binding.title.text = banner.title
            binding.closeButton.isVisible = banner.showCloseIcon == true
            binding.closeButton.setOnClickListener {
                cancel()
            }

            MarkdownUtils.markdown(
                context,
                binding.description,
                banner.footerDescription,
                configuration,
                object : MarkdownUtils.MarkdownTriggerListener {
                    override fun showModal() {
                        listener.showModal(this@BannerDialog)
                    }
                }
            )

            binding.closeButton.setOnClickListener {
                cancel()
            }

            binding.secondButton.apply {
                text = banner.secondaryButtonText
                isVisible = banner.secondaryButtonText?.isNotEmpty() == true

                setOnClickListener {
                    listener.onSecondButtonClick(this@BannerDialog)
                }
            }

            binding.firstButton.apply {
                text = banner.buttonText
                isVisible = banner.buttonText.isNotEmpty() == true

                setOnClickListener {
                    listener.onFirstButtonClick(this@BannerDialog)
                }
            }

            binding.poweredByKetch.setOnClickListener {
                context.poweredByKetch()
            }

            setContentView(binding.root)
        }

        setCanceledOnTouchOutside(false)
        setCancelable(true)

        setOnShowListener {
            behavior.apply {
                isFitToContents = true
                isHideable = false
                peekHeight =
                    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1000.0f, context.resources.displayMetrics)
                        .toInt()
                state = BottomSheetBehavior.STATE_EXPANDED
            }

            listener.onShow(this)
        }
        setOnDismissListener { listener.onHide(this) }
    }

    interface BannerDialogListener {
        fun onShow(bannerDialog: BannerDialog)
        fun onHide(bannerDialog: BannerDialog)
        fun onFirstButtonClick(bannerDialog: BannerDialog)
        fun onSecondButtonClick(bannerDialog: BannerDialog)
        fun showModal(bannerDialog: BannerDialog)
    }
}