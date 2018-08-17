package com.boardgamegeek.ui.dialog

import android.annotation.SuppressLint
import android.content.Context
import android.support.annotation.StringRes
import android.support.v7.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import com.appyvet.materialrangebar.RangeBar
import com.boardgamegeek.R
import com.boardgamegeek.filterer.CollectionFilterer
import kotlinx.android.synthetic.main.dialog_slider_filter.view.*

abstract class SliderFilterDialog : CollectionFilterDialog {
    private var low: Int = 0
    private var high: Int = 0

    @get:StringRes
    protected abstract val titleResId: Int

    protected open val checkboxVisibility: Int = View.VISIBLE

    protected open val checkboxTextResId: Int
        @StringRes
        get() = R.string.include_missing_values

    protected open val descriptionResId: Int
        @StringRes
        get() = INVALID_STRING_RES_ID

    protected abstract val absoluteMin: Int

    protected abstract val absoluteMax: Int

    override fun createDialog(context: Context, listener: CollectionFilterDialog.OnFilterChangedListener?, filter: CollectionFilterer?) {
        @SuppressLint("InflateParams")
        val layout = LayoutInflater.from(context).inflate(R.layout.dialog_slider_filter, null)

        val initialValues = initValues(filter)
        low = initialValues.min.coerceIn(absoluteMin, absoluteMax)
        high = initialValues.max.coerceIn(absoluteMin, absoluteMax)

        layout.rangeBar.apply {
            tickStart = absoluteMin.toFloat()
            tickEnd = absoluteMax.toFloat()
            setRangePinsByValue(low.toFloat(), high.toFloat())
            setPinTextFormatter { value -> getPinText(context, value) }
            setOnRangeBarChangeListener { _, leftPinIndex, rightPinIndex, _, _ ->
                low = (leftPinIndex + absoluteMin).coerceIn(absoluteMin, absoluteMax)
                high = (rightPinIndex + absoluteMin).coerceIn(absoluteMin, absoluteMax)
            }
        }

        layout.minDownButton.setOnClickListener {
            layout.rangeBar.apply {
                if (leftIndex > 0) {
                    updateRange(this, leftIndex - 1, rightIndex)
                }
            }
        }

        layout.minUpButton.setOnClickListener {
            layout.rangeBar.apply {
                if (leftIndex < tickCount - 1) {
                    if (leftIndex == rightIndex) {
                        updateRange(this, leftIndex + 1, rightIndex + 1)
                    } else {
                        updateRange(this, leftIndex + 1, rightIndex)
                    }
                }
            }
        }

        layout.maxUpButton.setOnClickListener {
            layout.rangeBar.apply {
                if (rightIndex < tickCount - 1) {
                    updateRange(this, leftIndex, rightIndex + 1)
                }
            }
        }

        layout.maxDownButton.setOnClickListener {
            layout.rangeBar.apply {
                if (rightIndex > 0) {
                    if (leftIndex == rightIndex) {
                        updateRange(this, leftIndex - 1, rightIndex - 1)
                    } else {
                        updateRange(this, leftIndex, rightIndex - 1)
                    }
                }
            }
        }

        layout.checkBox.apply {
            visibility = checkboxVisibility
            setText(checkboxTextResId)
            isChecked = initialValues.isChecked
        }

        layout.explanationView.apply {
            visibility = if (descriptionResId == INVALID_STRING_RES_ID) {
                View.GONE
            } else {
                setText(descriptionResId)
                View.VISIBLE
            }
        }

        val builder = AlertDialog.Builder(context, R.style.Theme_bgglight_Dialog_Alert)
                .setTitle(titleResId)
                .setNegativeButton(R.string.clear) { _, _ -> listener?.removeFilter(getType(context)) }
                .setPositiveButton(R.string.set) { _, _ -> listener?.addFilter(getPositiveData(context, low, high, layout.checkBox.isChecked)) }
                .setView(layout)

        builder.create().show()
    }

    private fun updateRange(rangeBar: RangeBar, leftPinIndex: Int, rightPinIndex: Int) {
        rangeBar.setRangePinsByIndices(
                leftPinIndex.coerceIn(0, rangeBar.tickCount - 1),
                rightPinIndex.coerceIn(0, rangeBar.tickCount - 1)
        )
        // HACK to make the pins remain visible when using the up/down buttons
        rangeBar.left = rangeBar.left + 1
    }

    protected abstract fun initValues(filter: CollectionFilterer?): InitialValues

    protected abstract fun getPositiveData(context: Context, min: Int, max: Int, checkbox: Boolean): CollectionFilterer

    protected open fun getPinText(context: Context, value: String) = value

    data class InitialValues @JvmOverloads constructor(val min: Int, val max: Int, val isChecked: Boolean = false)

    companion object {
        private const val INVALID_STRING_RES_ID = -1
    }
}
