package com.boardgamegeek.ui.dialog

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import com.boardgamegeek.R
import com.boardgamegeek.databinding.DialogSliderFilterBinding
import com.boardgamegeek.extensions.createThemedBuilder
import com.boardgamegeek.filterer.CollectionFilterer
import com.boardgamegeek.ui.viewmodel.CollectionViewViewModel
import com.google.android.material.slider.RangeSlider
import kotlin.math.roundToInt

abstract class SliderFilterDialog : CollectionFilterDialog {
    private var _binding: DialogSliderFilterBinding? = null
    private val binding get() = _binding!!

    protected var low = 0f
    protected var high = 0f
    protected var checkboxIsChecked = false
    protected var rangeIsIgnored = false

    @get:StringRes
    protected abstract val titleResId: Int

    protected open val supportsCheckbox: Boolean = true

    protected open val checkboxTextResId: Int
        @StringRes
        get() = R.string.include_missing_values

    // allows a single point value, in addition to a range
    protected open val supportsSlider = true

    protected open val supportsNone: Boolean
        get() = supportsSlider

    protected open val descriptionResId: Int
        @StringRes
        get() = INVALID_STRING_RES_ID

    protected abstract val valueFrom: Float
    protected abstract val valueTo: Float
    protected open val stepSize = 1.0f

    override fun createDialog(activity: FragmentActivity, filter: CollectionFilterer?) {
        val viewModel by lazy { ViewModelProvider(activity)[CollectionViewViewModel::class.java] }
        _binding = DialogSliderFilterBinding.inflate(LayoutInflater.from(activity), null, false)

        initValues(filter).apply {
            low = min.coerceIn(valueFrom, valueTo)
            high = max.coerceIn(valueFrom, valueTo)
            checkboxIsChecked = isChecked
            rangeIsIgnored = ignoreRange
        }

        binding.rangeSlider.apply {
            valueFrom = this@SliderFilterDialog.valueFrom
            valueTo = this@SliderFilterDialog.valueTo
            stepSize = this@SliderFilterDialog.stepSize
            values = if (low == high && supportsSlider) listOf(low) else listOf(low, high)

            setLabelFormatter { value ->
                return@setLabelFormatter formatSliderLabel(activity, value)
            }

            addOnChangeListener(RangeSlider.OnChangeListener { slider, _, _ ->
                low = slider.values.first()
                high = slider.values.last()
                binding.displayTextView.text = describeRange(activity)
            })
        }

        binding.minDownButton.setOnClickListener {
            binding.rangeSlider.apply {
                val first = values.first()
                if (first > valueFrom) {
                    val newValue = (first - stepSize).coerceAtLeast(valueFrom)
                    values = if (values.size == 1) listOf(newValue) else listOf(newValue, values.last())
                }
            }
        }

        binding.minUpButton.setOnClickListener {
            binding.rangeSlider.apply {
                val firstValue = values.first()
                val lastValue = values.last()
                if (firstValue < valueTo) {
                    val newValue = (firstValue + stepSize).coerceAtMost(valueTo)
                    values = if (values.size == 1)
                        listOf(newValue)
                    else
                        listOf(newValue, if (firstValue == lastValue) newValue else lastValue)
                }
            }
        }

        binding.maxUpButton.setOnClickListener {
            binding.rangeSlider.apply {
                val lastValue = values.last()
                if (lastValue < valueTo) {
                    values = listOf(values.first(), (lastValue + stepSize).coerceAtMost(valueTo))
                }
            }
        }

        binding.maxDownButton.setOnClickListener {
            binding.rangeSlider.apply {
                val firstValue = values.first()
                val lastValue = values.last()
                if (lastValue > valueFrom) {
                    val newValue = (lastValue - stepSize).coerceAtLeast(valueFrom)
                    values = listOf(if (firstValue == lastValue) newValue else firstValue, newValue)
                }
            }
        }

        binding.rangeRadioButton.apply {
            isVisible = supportsSlider || supportsNone
            isChecked = (low != high)
            setOnClickListener {
                binding.checkBox.isEnabled = true
                configRange(RangeType.RANGE)
            }
        }

        binding.singleValueRadioButton.apply {
            isVisible = supportsSlider
            isChecked = (low == high)
            setOnClickListener {
                binding.checkBox.isEnabled = true
                configRange(RangeType.SINGLE)
            }
        }

        binding.noneRadioButton.apply {
            isVisible = supportsNone
            isChecked = false
            setOnClickListener {
                binding.checkBox.isChecked = true
                binding.checkBox.isEnabled = false
                rangeIsIgnored = isChecked
                configRange(RangeType.NONE)
            }
        }

        if (supportsSlider) {
            val type = when {
                rangeIsIgnored -> RangeType.NONE
                (low == high) -> RangeType.SINGLE
                else -> RangeType.RANGE
            }
            configRange(type)
        }

        binding.checkBox.apply {
            isVisible = supportsCheckbox
            setText(checkboxTextResId)
            isChecked = checkboxIsChecked
            setOnCheckedChangeListener { _, _ ->
                checkboxIsChecked = isChecked
            }
        }

        binding.explanationView.apply {
            visibility = if (descriptionResId == INVALID_STRING_RES_ID) {
                View.GONE
            } else {
                setText(descriptionResId)
                View.VISIBLE
            }
        }

        binding.displayTextView.text = describeRange(activity)

        activity.createThemedBuilder()
            .setTitle(titleResId)
            .setNegativeButton(R.string.clear) { _, _ ->
                viewModel.removeFilter(getType(activity))
            }
            .setPositiveButton(R.string.set) { _, _ ->
                if (binding.rangeRadioButton.isChecked && low == valueFrom && high == valueTo && !binding.checkBox.isChecked) {
                    viewModel.removeFilter(getType(activity))
                } else {
                    viewModel.addFilter(createFilterer(activity))
                }
            }
            .setView(binding.root)
            .create()
            .show()
    }

    enum class RangeType {
        RANGE, SINGLE, NONE
    }

    private fun configRange(type: RangeType) {
        binding.displayTextView.isVisible = type != RangeType.NONE
        binding.rangeButtonContainer.isVisible = type != RangeType.NONE
        binding.rangeSlider.isVisible = type != RangeType.NONE
        binding.maxDownButton.isVisible = type == RangeType.RANGE
        binding.maxUpButton.isVisible = type == RangeType.RANGE
        binding.buttonSpace.isVisible = type == RangeType.RANGE
        if (type == RangeType.RANGE) {
            binding.rangeSlider.apply { values = listOf(low, high) }
        } else if (type == RangeType.SINGLE) {
            binding.rangeSlider.apply { values = listOf(low) } // TODO remember low & high?
        }
    }

    protected abstract fun initValues(filter: CollectionFilterer?): InitialValues

    protected abstract fun createFilterer(context: Context): CollectionFilterer

    protected open fun describeRange(context: Context): String {
        return if (low == high) formatSliderLabel(context, low) else "${formatSliderLabel(context, low)} - ${formatSliderLabel(context, high)}"
    }

    protected open fun formatSliderLabel(context: Context, value: Float) = value.roundToInt().toString()

    data class InitialValues(val min: Float, val max: Float, val isChecked: Boolean = false, val ignoreRange: Boolean = false)

    companion object {
        private const val INVALID_STRING_RES_ID = -1
    }
}
