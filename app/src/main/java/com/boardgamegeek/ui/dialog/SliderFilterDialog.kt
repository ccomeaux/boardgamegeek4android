package com.boardgamegeek.ui.dialog

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import com.appyvet.materialrangebar.RangeBar
import com.boardgamegeek.R
import com.boardgamegeek.databinding.DialogSliderFilterBinding
import com.boardgamegeek.filterer.CollectionFilterer

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

    protected open val rangeInterval = 1

    protected open val descriptionResId: Int
        @StringRes
        get() = INVALID_STRING_RES_ID

    protected abstract val absoluteMin: Int

    protected abstract val absoluteMax: Int

    override fun createDialog(context: Context, listener: CollectionFilterDialog.OnFilterChangedListener?, filter: CollectionFilterer?) {
        val binding = DialogSliderFilterBinding.inflate(LayoutInflater.from(context))

        val initialValues = initValues(filter)
        low = initialValues.min.coerceIn(absoluteMin, absoluteMax)
        high = initialValues.max.coerceIn(absoluteMin, absoluteMax)

        binding.rangeBar.apply {
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

        binding.minDownButton.setOnClickListener {
            binding.rangeBar.apply {
                if (leftIndex > 0) {
                    updateRange(this, leftIndex - 1, rightIndex)
                }
            }
        }

        binding.minUpButton.setOnClickListener {
            binding.rangeBar.apply {
                if (leftIndex < tickCount - 1) {
                    if (leftIndex == rightIndex) {
                        updateRange(this, leftIndex + 1, rightIndex + 1)
                    } else {
                        updateRange(this, leftIndex + 1, rightIndex)
                    }
                }
            }
        }

        binding.maxUpButton.setOnClickListener {
            binding.rangeBar.apply {
                if (rightIndex < tickCount - 1) {
                    updateRange(this, leftIndex, rightIndex + 1)
                }
            }
        }

        binding.maxDownButton.setOnClickListener {
            binding.rangeBar.apply {
                if (rightIndex > 0) {
                    if (leftIndex == rightIndex) {
                        updateRange(this, leftIndex - 1, rightIndex - 1)
                    } else {
                        updateRange(this, leftIndex, rightIndex - 1)
                    }
                }
            }
        }

        binding.rangeCheckBox.apply {
            visibility = if (supportsSlider) View.VISIBLE else View.GONE
            isChecked = (low != high)
            setOnCheckedChangeListener { _, isChecked ->
                binding.rangeBar.setRangeBarEnabled(isChecked)
                binding.minDownButton.visibility = if (isChecked) View.VISIBLE else View.GONE
                binding.minUpButton.visibility = if (isChecked) View.VISIBLE else View.GONE
                binding.buttonSpace.visibility = if (isChecked) View.VISIBLE else View.GONE
                if (isChecked) {
                    binding.rangeBar.apply {
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
                } else {
                    binding.rangeBar.apply { updateRange(this, leftIndex, rightIndex) }
                }
            }
        }

        binding.checkBox.apply {
            visibility = checkboxVisibility
            setText(checkboxTextResId)
            isChecked = initialValues.isChecked
        }

        binding.explanationView.apply {
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
                .setPositiveButton(R.string.set) { _, _ -> listener?.addFilter(getPositiveData(context, low, high, binding.checkBox.isChecked)) }
                .setView(binding.root)

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
