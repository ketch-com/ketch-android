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
import com.ketch.android.api.response.Purpose
import com.ketch.android.ui.databinding.ModalBinding
import com.ketch.android.ui.extension.poweredByKetch
import com.ketch.android.ui.R
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
    configuration: FullConfiguration,
    consent: Consent,
    private val listener: ModalDialogListener,
) : BaseDialog(context, configuration, consent) {

    private lateinit var binding: ModalBinding

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

        var translations = configuration.translations
        if (translations != null) {
            //use translations provided by config
            binding.poweredByKetch.contentDescription = translations["powered_by"] ?: getString(R.string.powered_by_ketch)
        } else {
            //use translations from local
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
                val items = binding.purposesView.items.associate {
                    it.purpose.code to it.accepted.toString()
                }

                val purposes: Map<String, String>? = consent.purposes?.map {
                    val accepted = items[it.key] ?: it.value
                    Pair(it.key, accepted)
                }?.toMap()

                consent.purposes = purposes
                listener.onButtonClick(this@ModalDialog, consent)
            }
        }
    }

    private fun buildDataCategories(theme: ColorTheme?, binding: ModalBinding, purpose: Purpose) {
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

    private fun buildVendors(theme: ColorTheme?, binding: ModalBinding, purpose: Purpose) {
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

    interface ModalDialogListener {
        fun onShow(modalDialog: ModalDialog)
        fun onHide(modalDialog: ModalDialog)
        fun onButtonClick(modalDialog: ModalDialog, consent: Consent)
    }
}