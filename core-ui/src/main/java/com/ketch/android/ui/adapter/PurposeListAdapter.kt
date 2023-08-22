package com.ketch.android.ui.adapter

import android.os.Build
import android.os.Handler
import android.os.Looper
import android.text.Html
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ketch.android.api.response.Purpose
import com.ketch.android.ui.R
import com.ketch.android.ui.databinding.ViewListPurposeRowBinding
import com.ketch.android.ui.theme.ColorTheme

/**
 * Purpose List Adapter
 */
internal class PurposeListAdapter(private val theme: ColorTheme?, private val translations: Map<String, String>?) :
    ListAdapter<PurposeItem, PurposeListAdapter.PurposesViewHolder>(DIFF_CALLBACK) {

    var vendorClickListener: (item: PurposeItem) -> Unit = {}
    var categoryClickListener: (item: PurposeItem) -> Unit = {}

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PurposesViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding: ViewListPurposeRowBinding = ViewListPurposeRowBinding.inflate(inflater, parent, false)
        return PurposesViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PurposesViewHolder, position: Int) {
        val item: PurposeItem = currentList[position]

        val context = holder.binding.root.context
        holder.binding.theme = theme
        holder.binding.purpose = item.purpose
        holder.binding.enabled = item.enabled
        holder.binding.accepted = item.accepted
        holder.binding.expanded = item.expanded

        holder.binding.expandedPanel.isVisible = item.expanded

        holder.binding.categories.isVisible = item.purpose.categories?.isNotEmpty() == true
        holder.binding.categories.setOnClickListener {
            categoryClickListener.invoke(item)
        }

        holder.binding.vendors.isVisible = item.purpose.tcfID?.isNotEmpty() == true
        holder.binding.vendors.setOnClickListener {
            vendorClickListener.invoke(item)
        }

        var purposeDescription = context.getString(R.string.purpose_description, item.purpose.description)

        var legalBasisDescription =
            context.getString(R.string.legal_basic_description, item.purpose.legalBasisDescription)

        if (translations != null ){
            val translatedPurpose = translations["purpose"] ?: "Purpose"
            val translatedLegalBasis = translations["legal_basis"] ?: "Legal Basis"
            val pDescription = item.purpose.description ?: ""
            val lbDescription = item.purpose.legalBasisDescription ?: ""
            purposeDescription = "<![CDATA[<b>$translatedPurpose:</b> $pDescription]]>"
            legalBasisDescription = "<![CDATA[<b>$translatedLegalBasis:</b> $lbDescription]]>"
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            holder.binding.purposeDescription.text = Html.fromHtml(purposeDescription, Html.FROM_HTML_MODE_COMPACT)
            holder.binding.legalBasicDescription.text = Html.fromHtml(legalBasisDescription, Html.FROM_HTML_MODE_COMPACT)
        } else {
            holder.binding.purposeDescription.text = Html.fromHtml(purposeDescription, Html.FROM_HTML_MODE_LEGACY)
            holder.binding.legalBasicDescription.text = Html.fromHtml(legalBasisDescription, Html.FROM_HTML_MODE_LEGACY)
        }


        holder.binding.acceptSwitch.setOnCheckedChangeListener { _, isChecked ->
            item.accepted = isChecked
            Handler(Looper.getMainLooper()).post {
                notifyItemChanged(position)
            }
        }

        holder.binding.expandButton.setOnCheckedChangeListener { _, isChecked ->
            item.expanded = isChecked
            Handler(Looper.getMainLooper()).post {
                notifyItemChanged(position)
            }
        }
    }

    class PurposesViewHolder(binding: ViewListPurposeRowBinding) : RecyclerView.ViewHolder(binding.root) {
        val binding: ViewListPurposeRowBinding

        init {
            this.binding = binding
        }
    }

    companion object {
        private val DIFF_CALLBACK: DiffUtil.ItemCallback<PurposeItem> = object : DiffUtil.ItemCallback<PurposeItem>() {
            override fun areItemsTheSame(oldItem: PurposeItem, newItem: PurposeItem): Boolean {
                return oldItem.purpose.code == newItem.purpose.code
            }

            override fun areContentsTheSame(oldItem: PurposeItem, newItem: PurposeItem): Boolean {
                return oldItem == newItem
            }
        }
    }
}

internal data class PurposeItem(
    val purpose: Purpose,
    val enabled: Boolean,
    var accepted: Boolean,
    var expanded: Boolean = false
)