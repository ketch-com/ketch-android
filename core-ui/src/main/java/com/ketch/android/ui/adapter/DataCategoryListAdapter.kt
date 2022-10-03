package com.ketch.android.ui.adapter

import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ketch.android.api.response.PurposeCategory
import com.ketch.android.ui.databinding.ViewListDataCategoriesRowBinding
import com.ketch.android.ui.theme.ColorTheme

/**
 * Data Category List Adapter
 */
internal class DataCategoryListAdapter(private val theme: ColorTheme?) :
    ListAdapter<DataCategoryItem, DataCategoryListAdapter.DataCategoryViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DataCategoryViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding: ViewListDataCategoriesRowBinding =
            ViewListDataCategoriesRowBinding.inflate(inflater, parent, false)
        return DataCategoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DataCategoryViewHolder, position: Int) {
        val item: DataCategoryItem = currentList[position]

        holder.binding.theme = theme
        holder.binding.category = item.category
        holder.binding.expanded = item.expanded

        holder.binding.expandedPanel.isVisible = item.expanded

        holder.binding.expandButton.setOnCheckedChangeListener { _, isChecked ->
            item.expanded = isChecked
            Handler(Looper.getMainLooper()).post {
                notifyItemChanged(position)
            }
        }
    }

    class DataCategoryViewHolder(binding: ViewListDataCategoriesRowBinding) : RecyclerView.ViewHolder(binding.root) {
        val binding: ViewListDataCategoriesRowBinding

        init {
            this.binding = binding
        }
    }

    companion object {
        private val DIFF_CALLBACK: DiffUtil.ItemCallback<DataCategoryItem> =
            object : DiffUtil.ItemCallback<DataCategoryItem>() {
                override fun areItemsTheSame(oldItem: DataCategoryItem, newItem: DataCategoryItem): Boolean {
                    return oldItem.category == newItem.category
                }

                override fun areContentsTheSame(oldItem: DataCategoryItem, newItem: DataCategoryItem): Boolean {
                    return oldItem == newItem
                }
            }
    }
}

internal data class DataCategoryItem(
    val category: PurposeCategory,
    var expanded: Boolean = false
)
