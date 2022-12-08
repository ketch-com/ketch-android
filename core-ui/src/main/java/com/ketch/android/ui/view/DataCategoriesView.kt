package com.ketch.android.ui.view

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.ketch.android.api.response.FullConfiguration
import com.ketch.android.api.response.PurposeCategory
import com.ketch.android.ui.R
import com.ketch.android.ui.adapter.DataCategoryItem
import com.ketch.android.ui.adapter.DataCategoryListAdapter
import com.ketch.android.ui.databinding.ViewDataCategoriesBinding
import com.ketch.android.ui.extension.DividerItemDecoration
import com.ketch.android.ui.extension.MarkdownUtils
import com.ketch.android.ui.theme.ColorTheme

/**
 * Data Categories View
 */
class DataCategoriesView @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null,
    defStyleAttr: Int = 0
) :
    FrameLayout(context, attributeSet, defStyleAttr) {

    var onBackClickListener: () -> Unit = {}

    private var binding: ViewDataCategoriesBinding =
        ViewDataCategoriesBinding.inflate(LayoutInflater.from(context), this, true)

    @SuppressLint("NotifyDataSetChanged")
    fun buildUi(
        theme: ColorTheme?,
        bodyTitle: String?,
        bodyDescription: String?,
        categories: List<PurposeCategory>?,
        configuration: FullConfiguration
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

        val adapter = DataCategoryListAdapter(theme)
        val items = categories?.map { category ->
            DataCategoryItem(
                category = category,
                expanded = false
            )
        } ?: emptyList()

        adapter.submitList(items)

        val drawable = ContextCompat.getDrawable(context, R.drawable.horizontal_divider)
        val dividerItemDecoration = DividerItemDecoration(drawable)

        binding.vendorsListView.apply {
            categoriesList.addItemDecoration(dividerItemDecoration)
            categoriesList.adapter = adapter
            categoriesList.layoutManager = LinearLayoutManager(context)
        }
    }
}