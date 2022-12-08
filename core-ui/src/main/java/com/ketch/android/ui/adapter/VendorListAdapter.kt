package com.ketch.android.ui.adapter

import android.os.Handler
import android.os.Looper
import android.text.Html
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ketch.android.api.response.Vendor
import com.ketch.android.api.response.VendorPurpose
import com.ketch.android.ui.R
import com.ketch.android.ui.databinding.ViewFeatureListRowBinding
import com.ketch.android.ui.databinding.ViewListVendorRowBinding
import com.ketch.android.ui.extension.openExternalLink
import com.ketch.android.ui.theme.ColorTheme

/**
 * Vendor List Adapter
 */
internal class VendorListAdapter(private val theme: ColorTheme?) :
    ListAdapter<VendorItem, VendorListAdapter.VendorViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VendorViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding: ViewListVendorRowBinding = ViewListVendorRowBinding.inflate(inflater, parent, false)
        return VendorViewHolder(binding)
    }

    override fun onBindViewHolder(holder: VendorViewHolder, position: Int) {
        val item: VendorItem = currentList[position]

        val context = holder.binding.root.context

        val vendor = item.vendor

        holder.binding.theme = theme
        holder.binding.vendor = vendor
        holder.binding.accepted = item.accepted
        holder.binding.expanded = item.expanded

        holder.binding.expandedPanel.isVisible = item.expanded

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

        holder.binding.purposes.isVisible = vendor.purposes?.isNotEmpty() == true
        val purposesAdapter = FeatureListAdapter(theme)
        purposesAdapter.submitList(vendor.purposes)
        holder.binding.purposesList.adapter = purposesAdapter
        holder.binding.purposesList.layoutManager = LinearLayoutManager(context)

        holder.binding.specialPurposes.isVisible = vendor.specialPurposes?.isNotEmpty() == true
        val specialPurposesAdapter = FeatureListAdapter(theme)
        specialPurposesAdapter.submitList(vendor.specialPurposes)
        holder.binding.specialPurposesList.adapter = specialPurposesAdapter
        holder.binding.specialPurposesList.layoutManager = LinearLayoutManager(context)

        holder.binding.features.isVisible = vendor.features?.isNotEmpty() == true
        val featuresAdapter = FeatureListAdapter(theme)
        featuresAdapter.submitList(vendor.features)
        holder.binding.featuresList.adapter = featuresAdapter
        holder.binding.featuresList.layoutManager = LinearLayoutManager(context)

        holder.binding.specialFeatures.isVisible = vendor.specialFeatures?.isNotEmpty() == true
        val specialFeaturesAdapter = FeatureListAdapter(theme)
        specialFeaturesAdapter.submitList(vendor.specialFeatures)
        holder.binding.specialFeaturesList.adapter = specialFeaturesAdapter
        holder.binding.specialFeaturesList.layoutManager = LinearLayoutManager(context)

        holder.binding.privacyPolicyButton.isVisible = vendor.policyUrl?.isNotEmpty() == true
        vendor.policyUrl?.let { url ->
            holder.binding.privacyPolicyButton.setOnClickListener {
                context.openExternalLink(url)
            }
        }
    }

    class VendorViewHolder(binding: ViewListVendorRowBinding) : RecyclerView.ViewHolder(binding.root) {
        val binding: ViewListVendorRowBinding

        init {
            this.binding = binding
        }
    }

    private class FeatureListAdapter(private val theme: ColorTheme?) :
        ListAdapter<VendorPurpose, FeatureListAdapter.FeatureViewHolder>(DIFF_CALLBACK) {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FeatureViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            val binding: ViewFeatureListRowBinding = ViewFeatureListRowBinding.inflate(inflater, parent, false)
            binding.theme = theme
            return FeatureViewHolder(binding)
        }

        override fun onBindViewHolder(holder: FeatureViewHolder, position: Int) {
            val item: VendorPurpose = currentList[position]
            val context = holder.binding.root.context

            val legalBasisDescription =
                context.getString(R.string.feature_text, item.name, item.legalBasis)
            holder.binding.title.text = Html.fromHtml(legalBasisDescription, Html.FROM_HTML_MODE_LEGACY)
        }

        class FeatureViewHolder(binding: ViewFeatureListRowBinding) : RecyclerView.ViewHolder(binding.root) {
            val binding: ViewFeatureListRowBinding

            init {
                this.binding = binding
            }
        }

        companion object {
            private val DIFF_CALLBACK: DiffUtil.ItemCallback<VendorPurpose> =
                object : DiffUtil.ItemCallback<VendorPurpose>() {
                    override fun areItemsTheSame(oldItem: VendorPurpose, newItem: VendorPurpose): Boolean {
                        return oldItem.name == newItem.name && oldItem.legalBasis == newItem.legalBasis
                    }

                    override fun areContentsTheSame(oldItem: VendorPurpose, newItem: VendorPurpose): Boolean {
                        return oldItem == newItem
                    }
                }
        }
    }

    companion object {
        private val DIFF_CALLBACK: DiffUtil.ItemCallback<VendorItem> = object : DiffUtil.ItemCallback<VendorItem>() {
            override fun areItemsTheSame(oldItem: VendorItem, newItem: VendorItem): Boolean {
                return oldItem.vendor.id == newItem.vendor.id
            }

            override fun areContentsTheSame(oldItem: VendorItem, newItem: VendorItem): Boolean {
                return oldItem == newItem
            }
        }
    }
}

internal data class VendorItem(
    val vendor: Vendor,
    var accepted: Boolean,
    var expanded: Boolean = false
)