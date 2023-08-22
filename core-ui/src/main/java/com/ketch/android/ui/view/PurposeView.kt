package com.ketch.android.ui.view

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.core.view.isVisible
import com.ketch.android.api.response.Consent
import com.ketch.android.api.response.FullConfiguration
import com.ketch.android.api.response.Purpose
import com.ketch.android.ui.databinding.ViewPurposeBinding
import com.ketch.android.ui.extension.MarkdownUtils
import com.ketch.android.ui.R
import com.ketch.android.ui.theme.ColorTheme

/**
 * Purposes View
 */
class PurposeView @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null,
    defStyleAttr: Int = 0
) :
    FrameLayout(context, attributeSet, defStyleAttr) {

    internal var vendorClickListener: (purpose: Purpose) -> Unit = {}
    internal var categoryClickListener: (purpose: Purpose) -> Unit = {}

    private var binding: ViewPurposeBinding =
        ViewPurposeBinding.inflate(LayoutInflater.from(context), this, true)

    @SuppressLint("NotifyDataSetChanged")
    fun buildUi(
        theme: ColorTheme?,
        configuration: FullConfiguration,
        purpose: Purpose
    ) {
        binding.theme = theme
        binding.purposeTitle.text = purpose.name

        MarkdownUtils.markdown(
            context,
            binding.purposeDescription,
            purpose.description ?: "",
            configuration
        )
        Log.d("legal basis consent visibility check", configuration.experiences?.consentExperience?.modal?.hideLegalBases.toString())
        Log.d("legal basis preference visibility check",configuration.experiences?.preference?.consents?.extensions?.get("hideLegalBases") ?: "not found")
        if (configuration.experiences?.consentExperience?.modal?.hideLegalBases == true || configuration.experiences?.preference?.consents?.extensions?.get("hideLegalBases") == "true") {
            binding.legalBasisName.isVisible = false
            binding.legalBasicDescription.isVisible = false
        } else {
            binding.legalBasisName.text = purpose.legalBasisName

            MarkdownUtils.markdown(
                context,
                binding.legalBasicDescription,
                purpose.legalBasisDescription ?: "",
                configuration
            )
        }

        val translations = configuration.translations
        if (translations != null) {
            binding.legalBasisName.text = translations["legal_basis"] + ": " +  purpose.legalBasisName
        } else {
            binding.legalBasisName.text = context.getString(R.string.legal_basic_name, purpose.legalBasisName)
        }

        binding.categories.isVisible = purpose.categories?.isNotEmpty() == true
        binding.categories.setOnClickListener {
            categoryClickListener.invoke(purpose)
        }

        binding.vendors.isVisible = purpose.tcfID?.isNotEmpty() == true
        binding.vendors.setOnClickListener {
            vendorClickListener.invoke(purpose)
        }
    }
}