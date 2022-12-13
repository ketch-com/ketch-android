package com.ketch.android.ui.dialog

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.text.Html
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.ketch.android.api.response.Consent
import com.ketch.android.api.response.FullConfiguration
import com.ketch.android.api.response.Jit
import com.ketch.android.api.response.Purpose
import com.ketch.android.ui.databinding.JitBinding
import com.ketch.android.ui.extension.MarkdownUtils
import com.ketch.android.ui.extension.poweredByKetch
import com.ketch.android.ui.theme.ColorTheme

/**
 * Just in Time Dialog
 *
 * @param context - [android.content.Context]
 * @param configuration - [com.ketch.android.api.response.FullConfiguration]
 * @param consent - [com.ketch.android.api.response.Consent]
 * @param listener - [com.ketch.android.ui.dialog.JitDialog.JitDialogListener]
 */
internal class JitDialog(
    context: Context,
    configuration: FullConfiguration,
    consent: Consent,
    private val purpose: Purpose,
    private val listener: JitDialogListener,
) : BaseDialog(context, configuration, consent) {

    @SuppressLint("NotifyDataSetChanged")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configuration.experiences?.consentExperience?.jit?.let { jit ->
            val binding = JitBinding.inflate(LayoutInflater.from(context))
            val theme = configuration.theme?.let {
                ColorTheme.modalColorTheme(it)
            }
            binding.theme = theme
            binding.title.text = jit.title
            binding.closeButton.isVisible = jit.showCloseIcon == true
            binding.closeButton.setOnClickListener {
                cancel()
            }

            MarkdownUtils.markdown(
                context,
                binding.bodyDescription,
                jit.bodyDescription ?: "",
                configuration
            )

            buildPurpose(theme, binding, jit, purpose)

            setContentView(binding.root)
            setCanceledOnTouchOutside(false)
            setCancelable(true)

            setOnShowListener { listener.onShow(this) }
            setOnDismissListener { listener.onHide(this) }
        }
    }

    private fun buildPurpose(theme: ColorTheme?, binding: JitBinding, jit: Jit, purpose: Purpose) {
        binding.purposeView.buildUi(theme, configuration, purpose)

        binding.purposeView.categoryClickListener = {
            buildDataCategories(theme, binding, it)

            binding.purposesPanel.isVisible = false
            binding.categoriesPanel.isVisible = true
        }

        binding.purposeView.vendorClickListener = {
            buildVendors(theme, binding, it)

            binding.purposesPanel.isVisible = false
            binding.vendorsPanel.isVisible = true
        }

        binding.firstButton.apply {
            text = jit.acceptButtonText
            isVisible = jit.acceptButtonText.isNotEmpty() == true

            setOnClickListener {
                val purposes: Map<String, String>? = consent.purposes?.map {
                    val accepted = if (it.key == purpose.code) {
                        true.toString()
                    } else {
                        it.value
                    }
                    Pair(it.key, accepted)
                }?.toMap()

                consent.purposes = purposes
                listener.onFirstButtonClick(this@JitDialog, consent)
            }
        }

        binding.secondButton.apply {
            text = jit.declineButtonText
            isVisible = jit.declineButtonText.isNotEmpty() == true

            setOnClickListener {
                val purposes: Map<String, String>? = consent.purposes?.map {
                    val accepted = if (purpose.allowsOptOut == true && it.key == purpose.code) {
                        false.toString()
                    } else {
                        it.value
                    }
                    Pair(it.key, accepted)
                }?.toMap()

                consent.purposes = purposes
                listener.onSecondButtonClick(this@JitDialog, consent)
            }
        }

        binding.thirdButton.apply {
            text = Html.fromHtml("<u>${jit.moreInfoText}</u>", Html.FROM_HTML_MODE_LEGACY)
            isVisible = jit.moreInfoText?.isNotEmpty() == true

            setOnClickListener {
                listener.onThirdButtonClick(this@JitDialog)
            }
        }

        binding.poweredByKetch.setOnClickListener {
            context.poweredByKetch()
        }
    }

    private fun buildDataCategories(theme: ColorTheme?, binding: JitBinding, purpose: Purpose) {
        binding.theme = theme
        binding.categoriesView.buildUi(
            theme,
            purpose.name,
            purpose.description,
            purpose.categories,
            configuration
        )
        binding.categoriesView.onBackClickListener = {
            binding.purposesPanel.isVisible = true
            binding.categoriesPanel.isVisible = false
        }
    }

    private fun buildVendors(theme: ColorTheme?, binding: JitBinding, purpose: Purpose) {
        binding.vendorsView.buildUi(theme, purpose.name, purpose.description, configuration, consent)
        binding.vendorsView.onBackClickListener = {
            consent.vendors = binding.vendorsView.items.filter {
                it.accepted
            }.map {
                it.vendor.id
            }

            binding.purposesPanel.isVisible = true
            binding.vendorsPanel.isVisible = false
        }
    }

    override fun onStart() {
        super.onStart()
        window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
    }

    interface JitDialogListener {
        fun onShow(jitDialog: JitDialog)
        fun onHide(jitDialog: JitDialog)
        fun onFirstButtonClick(jitDialog: JitDialog, consent: Consent)
        fun onSecondButtonClick(jitDialog: JitDialog, consent: Consent)
        fun onThirdButtonClick(jitDialog: JitDialog)
        fun showModal(jitDialog: JitDialog)
    }
}