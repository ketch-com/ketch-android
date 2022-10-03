package com.ketch.android.ui.adapter

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ketch.android.api.response.Right
import com.ketch.android.ui.databinding.ViewListRightRowBinding
import com.ketch.android.ui.theme.ColorTheme

/**
 * Right List Adapter
 */
internal class RightListAdapter(private val theme: ColorTheme?) :
    ListAdapter<Right, RightListAdapter.RightViewHolder>(DIFF_CALLBACK) {

    private var selectedItemPosition: Int? = null
    val selectedItem: Right?
        get() = selectedItemPosition?.let {
            getItem(it)
        }

    override fun submitList(list: List<Right>?) {
        selectedItemPosition = if (list?.isNotEmpty() == true) {
            0
        } else null
        super.submitList(list)
    }

    @SuppressLint("NotifyDataSetChanged")
    fun reset() {
        selectedItemPosition = getItem(0)?.let { 0 }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RightViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding: ViewListRightRowBinding = ViewListRightRowBinding.inflate(inflater, parent, false)
        return RightViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RightViewHolder, position: Int) {
        val item: Right = currentList[position]
        holder.binding.theme = theme
        holder.binding.right = item
        holder.binding.radioButton.apply {
            isChecked = item == selectedItem
            tag = position
            holder.binding.radioButton.setOnClickListener {
                val prevSelectedItemPosition = selectedItemPosition
                selectedItemPosition = tag as Int
                Handler(Looper.getMainLooper()).post {
                    prevSelectedItemPosition?.let {
                        notifyItemChanged(it)
                    }
                }
            }
        }
    }

    class RightViewHolder(binding: ViewListRightRowBinding) : RecyclerView.ViewHolder(binding.root) {
        val binding: ViewListRightRowBinding

        init {
            this.binding = binding
        }
    }

    companion object {
        private val DIFF_CALLBACK: DiffUtil.ItemCallback<Right> = object : DiffUtil.ItemCallback<Right>() {
            override fun areItemsTheSame(oldItem: Right, newItem: Right): Boolean {
                return oldItem.code == newItem.code
            }

            override fun areContentsTheSame(oldItem: Right, newItem: Right): Boolean {
                return oldItem == newItem
            }
        }
    }
}
