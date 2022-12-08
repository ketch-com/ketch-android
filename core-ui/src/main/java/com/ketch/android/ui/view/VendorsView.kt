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
import com.ketch.android.ui.R
import com.ketch.android.ui.adapter.VendorItem
import com.ketch.android.ui.adapter.VendorListAdapter
import com.ketch.android.ui.databinding.ViewVendorsBinding
import com.ketch.android.ui.extension.DividerItemDecoration
import com.ketch.android.ui.extension.MarkdownUtils
import com.ketch.android.ui.theme.ColorTheme

/**
 * Vendors View
 */
class VendorsView @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null,
    defStyleAttr: Int = 0
) :
    FrameLayout(context, attributeSet, defStyleAttr) {

    internal var items: List<VendorItem> = emptyList()

    var onBackClickListener: () -> Unit = {}

    private var binding: ViewVendorsBinding =
        ViewVendorsBinding.inflate(LayoutInflater.from(context), this, true)

    @SuppressLint("NotifyDataSetChanged")
    fun buildUi(
        theme: ColorTheme?,
        bodyTitle: String?,
        bodyDescription: String?,
        configuration: FullConfiguration,
        consent: Consent
    ) {
        binding.theme = theme
        binding.backButton.setOnClickListener {
            onBackClickListener.invoke()
        }

        binding.bodyTitle.text = bodyTitle
        binding.bodyTitle.isVisible = bodyTitle?.isNotEmpty() == true

        MarkdownUtils.markdown(
            context,
            binding.bodyDescription,
            bodyDescription ?: "",
            configuration
        )

        val adapter = VendorListAdapter(theme)
        items = configuration.vendors?.map { vendor ->
            VendorItem(
                vendor,
                consent.vendors?.contains(vendor.id) == true
            )
        } ?: emptyList()

        adapter.submitList(items)

        val drawable = ContextCompat.getDrawable(context, R.drawable.horizontal_divider)
        val dividerItemDecoration = DividerItemDecoration(drawable)

        binding.vendorsListView.apply {
            vendorsList.addItemDecoration(dividerItemDecoration)
            vendorsList.adapter = adapter
            vendorsList.layoutManager = LinearLayoutManager(context)
        }

        binding.vendorsListView.acceptAllButton.setOnClickListener {
            adapter.currentList.onEach {
                it.accepted = true
            }
            adapter.notifyDataSetChanged()
        }

        binding.vendorsListView.rejectAllButton.setOnClickListener {
            adapter.currentList.onEach {
                it.accepted = false
            }
            adapter.notifyDataSetChanged()
        }
    }
}