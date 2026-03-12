package com.example.library

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager

class GridSpacingItemDecoration(
    private val spanCount: Int,
    private val spacingPx: Int
) : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        val position = parent.getChildAdapterPosition(view)
        if (position == 0) {
            outRect.set(0, 0, 0, spacingPx)
            return
        }

        val params = view.layoutParams as? StaggeredGridLayoutManager.LayoutParams
        val column = params?.spanIndex ?: 0

        outRect.left = spacingPx - column * spacingPx / spanCount
        outRect.right = (column + 1) * spacingPx / spanCount
        outRect.bottom = spacingPx

        val adjustedPosition = position - 1
        if (adjustedPosition < spanCount) {
            outRect.top = 0
        } else {
            outRect.top = spacingPx
        }
    }
}
