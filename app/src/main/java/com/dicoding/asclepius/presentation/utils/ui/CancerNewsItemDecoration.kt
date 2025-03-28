package com.dicoding.asclepius.presentation.utils.ui

import android.content.Context
import android.graphics.Canvas
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemDecoration
import com.dicoding.asclepius.R

class CancerNewsItemDecoration(context: Context) : ItemDecoration() {
    private val divider = ContextCompat.getDrawable(context, R.drawable.divider_horizontal)!!

    override fun onDrawOver(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        val childCount = parent.childCount
        for (i in 0 until childCount) {
            val view = parent.getChildAt(i)
            val position = parent.getChildAdapterPosition(view)

            val isPositionOdd = position % 2 == 1
            if (position > 0 && isPositionOdd) {
                val params = view.layoutParams as RecyclerView.LayoutParams
                val left = view.right + params.marginEnd
                val right = left + divider.intrinsicWidth
                val top = view.top
                val bottom = view.bottom

                divider.setBounds(left, top, right, bottom)
                divider.draw(c)
            }
        }
    }
}