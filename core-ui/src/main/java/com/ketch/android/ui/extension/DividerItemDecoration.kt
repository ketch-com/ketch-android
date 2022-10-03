package com.ketch.android.ui.extension

import android.graphics.Canvas
import android.graphics.drawable.Drawable
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemDecoration

internal class DividerItemDecoration(private val mDivider: Drawable?) :
    ItemDecoration() {

    override fun onDrawOver(canvas: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        val dividerLeft = parent.paddingLeft
        val dividerRight = parent.width - parent.paddingRight
        val childCount = parent.childCount

        for (i in 0 until childCount) {
            val child = parent.getChildAt(i)
            val params = child.layoutParams as RecyclerView.LayoutParams
            val dividerTop = child.bottom + params.bottomMargin
            val dividerBottom = dividerTop + (mDivider?.intrinsicHeight ?: 0)
            mDivider?.setBounds(dividerLeft, dividerTop, dividerRight, dividerBottom)
            mDivider?.draw(canvas)
        }
    }
}