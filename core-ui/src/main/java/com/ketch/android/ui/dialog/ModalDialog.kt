package com.ketch.android.ui.dialog

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.ketch.android.api.response.Consent
import com.ketch.android.api.response.FullConfiguration
import com.ketch.android.api.response.Modal
import com.ketch.android.ui.adapter.PurposeItem
import com.ketch.android.ui.databinding.ModalBinding
import com.ketch.android.ui.extension.poweredByKetch
import com.ketch.android.ui.theme.ColorTheme

/**
 * Modal Dialog
 *
 * @param context - [android.content.Context]
 * @param configuration - [com.ketch.android.api.response.FullConfiguration]
 * @param consent - [com.ketch.android.api.response.Consent]
 * @param listener - [com.ketch.android.ui.dialog.ModalDialog.ModalDialogListener]
 */
internal class ModalDialog(
    context: Context,
    private val configuration: FullConfiguration,
    consent: Consent,
    private val listener: ModalDialogListener,
) : BaseDialog(context) {

    private lateinit var binding: ModalBinding
    private val consent = consent.copy()

    @SuppressLint("NotifyDataSetChanged")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ModalBinding.inflate(LayoutInflater.from(context))
        configuration.experiences?.consentExperience?.modal?.let { modal ->
            val theme = configuration.theme?.let {
                ColorTheme.modalColorTheme(it)
            }

            binding.theme = theme

            binding.title.text = modal.title
            binding.closeButton.isVisible = modal.showCloseIcon == true
            binding.closeButton.setOnClickListener {
                cancel()
            }

            binding.poweredByKetch.setOnClickListener {
                context.poweredByKetch()
            }

            buildPurposes(theme, binding, modal)

            setContentView(binding.root)
            setCanceledOnTouchOutside(false)
            setCancelable(true)

            setOnShowListener {
                listener.onShow(this)
            }
            setOnDismissListener { listener.onHide(this) }
        }
    }

    override fun onBackPressed() {
        if (binding.vendorsPanel.isVisible) {
            consent.vendors = binding.vendorsView.items.filter {
                it.accepted
            }.map {
                it.vendor.id
            }

            binding.purposesPanel.isVisible = true
            binding.vendorsPanel.isVisible = false
        } else {
            super.onBackPressed()
        }
    }

    private fun buildPurposes(theme: ColorTheme?, binding: ModalBinding, modal: Modal) {
        binding.purposesView.buildUi(theme, modal.bodyTitle, modal.bodyDescription, configuration, consent)

        binding.purposesView.categoryClickListener = {
            buildDataCategories(theme, binding, it)

            binding.purposesPanel.isVisible = false
            binding.categoriesPanel.isVisible = true
        }

        binding.purposesView.vendorClickListener = {
            buildVendors(theme, binding, it)

            binding.purposesPanel.isVisible = false
            binding.vendorsPanel.isVisible = true
        }

        binding.firstButton.apply {
            text = modal.buttonText
            isVisible = modal.buttonText.isNotEmpty() == true

            setOnClickListener {
                val purposes: Map<String, String> = binding.purposesView.items.associate {
                    it.purpose.code to it.accepted.toString()
                }
                consent.purposes = purposes
                listener.onButtonClick(this@ModalDialog, consent)
            }
        }
    }

    private fun buildDataCategories(theme: ColorTheme?, binding: ModalBinding, item: PurposeItem) {
        binding.theme = theme
        binding.categoriesView.buildUi(theme, item.purpose.name, item.purpose.description, item.purpose.categories, configuration)
        binding.categoriesView.onBackClickListener = {
            binding.purposesPanel.isVisible = true
            binding.categoriesPanel.isVisible = false
        }
    }

    private fun buildVendors(theme: ColorTheme?, binding: ModalBinding, item: PurposeItem) {
        binding.vendorsView.buildUi(theme, item.purpose.name, item.purpose.description, configuration, consent)
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

    interface ModalDialogListener {
        fun onShow(modalDialog: ModalDialog)
        fun onHide(modalDialog: ModalDialog)
        fun onButtonClick(modalDialog: ModalDialog, consent: Consent)
    }
}