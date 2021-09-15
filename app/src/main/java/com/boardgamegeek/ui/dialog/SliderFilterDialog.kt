package com.boardgamegeek.ui.dialog

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
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

    protected open val supportsSlider = true

    protected open val supportsNone: Boolean
        get() = supportsSlider

    protected open val rangeInterval = 1

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
            if (low == high && supportsSlider) setRangeBarEnabled(false)
            setPinTextFormatter { value -> getPinText(context, value) }
            setOnRangeBarChangeListener(object : RangeBar.OnRangeBarChangeListener {
                override fun onRangeChangeListener(rangeBar: RangeBar?, leftPinIndex: Int, rightPinIndex: Int, leftPinValue: String?, rightPinValue: String?) {
                    high = (rightPinIndex + absoluteMin).coerceIn(absoluteMin, absoluteMax)
                    low = if (isRangeBar) {
                        (leftPinIndex + absoluteMin).coerceIn(absoluteMin, absoluteMax)
                    } else {
                        high
                    }
                }

                override fun onTouchStarted(rangeBar: RangeBar?) {
                }

                override fun onTouchEnded(rangeBar: RangeBar?) {
                }
            })
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

        layout.rangeRadioButton.apply {
            isVisible = supportsSlider
            isChecked = (low != high) && !initialValues.ignoreRange
            setOnClickListener {
                configRange(layout, RangeType.RANGE)
            }
        }

        layout.singleValueRadioButton.apply {
            isVisible = supportsSlider
            isChecked = (low == high) && !initialValues.ignoreRange
            setOnClickListener {
                configRange(layout, RangeType.SINGLE)
            }
        }

        layout.noneRadioButton.apply {
            isVisible = supportsNone
            isChecked = initialValues.ignoreRange
            setOnClickListener {
                layout.checkBox.isChecked = true
                configRange(layout, RangeType.NONE)
            }
        }

        if (supportsSlider) {
            val type = when {
                initialValues.ignoreRange -> RangeType.NONE
                (low == high) -> RangeType.SINGLE
                else -> RangeType.RANGE
            }
            configRange(layout, type)
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
                .setPositiveButton(R.string.set) { _, _ ->
                    val l = if (layout.noneRadioButton.isChecked) 0 else low
                    listener?.addFilter(getPositiveData(context, l, high, layout.checkBox.isChecked, layout.noneRadioButton.isChecked))
                }
                .setView(layout)

        builder.create().show()
    }

    enum class RangeType {
        RANGE, SINGLE, NONE
    }

    private fun configRange(layout: View, type: RangeType) {
        layout.rangeButtonContainer.isVisible = type != RangeType.NONE
        layout.rangeBar.isVisible = type != RangeType.NONE
        layout.rangeBar.setRangeBarEnabled(type == RangeType.RANGE)
        layout.minDownButton.isVisible = type == RangeType.RANGE
        layout.minUpButton.isVisible = type == RangeType.RANGE
        layout.buttonSpace.isVisible = type == RangeType.RANGE
        if (type == RangeType.RANGE) {
            layout.rangeBar.apply {
                if (leftIndex == rightIndex) {
                    if (leftIndex > 0) {
                        updateRange(this, leftIndex - rangeInterval, rightIndex)
                    } else {
                        updateRange(this, leftIndex, rightIndex + rangeInterval)
                    }
                } else {
                    updateRange(this, leftIndex, rightIndex)
                }
            }
        } else if (type == RangeType.SINGLE) {
            layout.rangeBar.apply { updateRange(this, leftIndex, rightIndex) }
        }
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

    protected abstract fun getPositiveData(context: Context, min: Int, max: Int, checkbox: Boolean, ignoreRange: Boolean): CollectionFilterer

    protected open fun getPinText(context: Context, value: String) = value

    data class InitialValues(val min: Int, val max: Int, val isChecked: Boolean = false, val ignoreRange: Boolean = false)

    companion object {
        private const val INVALID_STRING_RES_ID = -1
    }
}
