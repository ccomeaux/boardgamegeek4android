/**
 * From: http://www.timothypaetz.com/recycler-view-headers/
 */
package com.boardgamegeek.ui.widget

import android.graphics.Canvas
import android.graphics.Rect
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.boardgamegeek.R
import com.boardgamegeek.extensions.inflate

class RecyclerSectionItemDecoration(private val headerOffset: Int, private val sectionCallback: SectionCallback, private val sticky: Boolean = true) : RecyclerView.ItemDecoration() {
    private var headerView: View? = null
    private var titleView: TextView? = null

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        outRect.set(0, 0, 0, 0)
        val pos = parent.getChildAdapterPosition(view)
        if (pos != RecyclerView.NO_POSITION && sectionCallback.isSection(pos)) {
            outRect.top = headerOffset
        }
    }

    override fun onDrawOver(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        super.onDrawOver(c, parent, state)

        if (headerView == null) {
            headerView = inflateHeaderView(parent)
            titleView = headerView?.findViewById(android.R.id.title)
            fixLayoutSize(headerView, parent)
        }

        var previousHeader: CharSequence = ""
        for (i in 0 until parent.childCount) {
            val child = parent.getChildAt(i) ?: continue
            val nextChild = parent.getChildAt(i + 1)
            val position = parent.getChildAdapterPosition(child)
            val nextPosition = parent.getChildAdapterPosition(nextChild)

            val header = sectionCallback.getSectionHeader(position)
            val isSection = sectionCallback.isSection(position)
            if (previousHeader != header || isSection) {
                titleView?.text = header
                drawHeader(c, child, headerView, if (nextPosition != RecyclerView.NO_POSITION && sectionCallback.isSection(nextPosition)) nextChild else null)
                previousHeader = header
            }
        }
    }

    private fun drawHeader(c: Canvas, child: View, headerView: View?, nextChild: View?) {
        if (headerView == null) return
        c.save()
        if (sticky) {
            c.translate(0f, Math.max(if (nextChild == null) 0 else Math.min(0, nextChild.top - headerView.height * 2), child.top - headerView.height).toFloat())
        } else {
            c.translate(0f, (child.top - headerView.height).toFloat())
        }
        headerView.draw(c)
        c.restore()
    }

    private fun inflateHeaderView(parent: RecyclerView): View {
        return parent.inflate(R.layout.row_header)
    }

    /**
     * Measures the header view to make sure its size is greater than 0 and will be drawn
     * https://yoda.entelect.co.za/view/9627/how-to-android-recyclerview-item-decorations
     */
    private fun fixLayoutSize(view: View?, parent: ViewGroup) {
        if (view == null) return

        val widthSpec = View.MeasureSpec.makeMeasureSpec(parent.width, View.MeasureSpec.EXACTLY)
        val heightSpec = View.MeasureSpec.makeMeasureSpec(parent.height, View.MeasureSpec.UNSPECIFIED)

        val childWidth = ViewGroup.getChildMeasureSpec(widthSpec, parent.paddingLeft + parent.paddingRight, view.layoutParams.width)
        val childHeight = ViewGroup.getChildMeasureSpec(heightSpec, parent.paddingTop + parent.paddingBottom, view.layoutParams.height)

        view.measure(childWidth, childHeight)

        view.layout(0, 0, view.measuredWidth, view.measuredHeight)
    }

    interface SectionCallback {
        fun isSection(position: Int): Boolean

        fun getSectionHeader(position: Int): CharSequence
    }
}
