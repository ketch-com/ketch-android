package com.ketch.android.ui.view

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.ketch.android.api.response.Consent
import com.ketch.android.api.response.FullConfiguration
import com.ketch.android.api.response.Purpose
import com.ketch.android.ui.R
import com.ketch.android.ui.adapter.PurposeItem
import com.ketch.android.ui.adapter.PurposeListAdapter
import com.ketch.android.ui.databinding.ViewPurposesBinding
import com.ketch.android.ui.extension.DividerItemDecoration
import com.ketch.android.ui.extension.MarkdownUtils
import com.ketch.android.ui.theme.ColorTheme

/**
 * Purposes View
 */
class PurposesView @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null,
    defStyleAttr: Int = 0
) :
    FrameLayout(context, attributeSet, defStyleAttr) {

    internal var items: List<PurposeItem> = emptyList()
    internal var vendorClickListener: (purpose: Purpose) -> Unit = {}
    internal var categoryClickListener: (purpose: Purpose) -> Unit = {}

    private var binding: ViewPurposesBinding =
        ViewPurposesBinding.inflate(LayoutInflater.from(context), this, true)

    @SuppressLint("NotifyDataSetChanged")
    fun buildUi(
        theme: ColorTheme?,
        bodyTitle: String?,
        bodyDescription: String?,
        configuration: FullConfiguration,
        consent: Consent
    ) {
        binding.theme = theme
        binding.bodyTitle.text = bodyTitle
        binding.bodyTitle.isVisible = bodyTitle?.isNotEmpty() == true

        MarkdownUtils.markdown(
            context,
            binding.bodyDescription,
            bodyDescription ?: "",
            configuration
        )

        val adapter = PurposeListAdapter(theme)
        items = configuration.purposes?.filter {
            it.requiresDisplay == true
        }?.map { purpose ->
            val enabled = purpose.allowsOptOut == true
            val accepted = consent.purposes?.get(purpose.code)?.toBoolean() == true
            PurposeItem(
                purpose = purpose,
                enabled = enabled,
                accepted = accepted
            )
        } ?: emptyList()

        adapter.submitList(items)

        adapter.categoryClickListener = {
            categoryClickListener.invoke(it.purpose)
        }

        adapter.vendorClickListener = {
            vendorClickListener.invoke(it.purpose)
        }

        val drawable = ContextCompat.getDrawable(context, R.drawable.horizontal_divider)
        val dividerItemDecoration = DividerItemDecoration(drawable)

        binding.purposesListView.apply {
            purposesList.addItemDecoration(dividerItemDecoration)
            purposesList.adapter = adapter
            purposesList.layoutManager = LinearLayoutManager(context)
        }

        binding.purposesListView.acceptAllButton.setOnClickListener {
            adapter.currentList.onEach {
                it.accepted = true
            }
            adapter.notifyDataSetChanged()
        }

        binding.purposesListView.rejectAllButton.setOnClickListener {
            adapter.currentList.onEach {
                if (it.enabled) {
                    it.accepted = false
                }
            }
            adapter.notifyDataSetChanged()
        }

        var translations = configuration.translations
        if (translations != null) {
            binding.purposesListView.acceptAllButton.text = translations["accept_all"]
            binding.purposesListView.rejectAllButton.text = translations["reject_all"]
        }
    }
}