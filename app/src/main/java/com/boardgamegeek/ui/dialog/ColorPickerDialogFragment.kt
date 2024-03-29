package com.boardgamegeek.ui.dialog

import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import com.boardgamegeek.databinding.DialogColorsBinding
import com.boardgamegeek.extensions.createThemedBuilder
import com.boardgamegeek.ui.adapter.ColorGridAdapter

abstract class ColorPickerDialogFragment : DialogFragment() {
    private var _binding: DialogColorsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogColorsBinding.inflate(layoutInflater)

        val builder = requireContext().createThemedBuilder().setView(binding.root)
        @StringRes val titleResId = arguments?.getInt(KEY_TITLE_ID) ?: 0
        if (titleResId != 0) {
            builder.setTitle(titleResId)
        } else {
            val title = arguments?.getString(KEY_TITLE).orEmpty()
            if (title.isNotBlank()) builder.setTitle(title)
        }
        return builder.create()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val colorChoices: MutableList<Pair<String, Int>> = ArrayList()
        for (i in 0 until (arguments?.getInt(KEY_COLOR_COUNT) ?: 0)) {
            val name = arguments?.getString(KEY_COLORS_DESCRIPTION + i).orEmpty()
            val color = arguments?.getInt(KEY_COLORS + i) ?: Color.TRANSPARENT
            colorChoices.add(Pair(name, color))
        }
        val selectedColor = arguments?.getString(KEY_SELECTED_COLOR)
        val hiddenColors = arguments?.getStringArrayList(KEY_HIDDEN_COLORS)
        val disabledColors = arguments?.getStringArrayList(KEY_DISABLED_COLORS)
        val featuredColors = arguments?.getStringArrayList(KEY_FEATURED_COLORS)
        val requestCode = arguments?.getInt(KEY_REQUEST_CODE) ?: 0

        val numColumns = arguments?.getInt(KEY_COLUMNS) ?: DEFAULT_NUMBER_Of_COLUMNS
        binding.colorGrid.numColumns = numColumns
        binding.featuredColorGrid.numColumns = numColumns

        binding.addButton.isVisible = arguments?.getBoolean(KEY_SHOW_ADD_BUTTON) ?: false

        // remove the hidden colors from the choices list
        val choices = ArrayList(colorChoices)
        if (hiddenColors != null) {
            for (i in colorChoices.indices.reversed()) {
                val pair = choices[i]
                if (hiddenColors.contains(pair.first)) {
                    choices.removeAt(i)
                }
            }
        }

        // if there are featured colors, remove them from the choices list and add them to the featured choices
        val featuredColorGridAdapter = if (featuredColors?.isNotEmpty() == true) {
            val featured = ArrayList<Pair<String, Int>>()
            for (i in choices.indices.reversed()) {
                val pair = choices[i]
                if (featuredColors.contains(pair.first)) {
                    choices.removeAt(i)
                    featured.add(0, pair)
                }
            }
            if (featured.isEmpty()) null else ColorGridAdapter(featured, disabledColors)
        } else {
            null
        }

        val colorGridAdapter = ColorGridAdapter(choices, disabledColors)
        colorGridAdapter.selectedColor = selectedColor
        binding.colorGrid.adapter = colorGridAdapter

        if (featuredColorGridAdapter != null) {
            featuredColorGridAdapter.selectedColor = selectedColor
            binding.featuredColorGrid.adapter = featuredColorGridAdapter
            binding.featuredColorGrid.visibility = View.VISIBLE

            binding.moreView.visibility = View.VISIBLE
            binding.moreView.setOnClickListener {
                binding.moreView.visibility = View.GONE
                binding.dividerView.visibility = View.VISIBLE
                binding.colorGrid.visibility = View.VISIBLE
            }

            binding.colorGrid.visibility = View.GONE
        } else {
            binding.featuredColorGrid.visibility = View.GONE
            binding.moreView.visibility = View.GONE
            binding.colorGrid.visibility = View.VISIBLE
        }

        listOf(binding.colorGrid, binding.featuredColorGrid).forEach {
            it.setOnItemClickListener { parent, _, position, _ ->
                val adapter = parent.adapter as? ColorGridAdapter
                val item = adapter?.getItem(position)
                onColorClicked(item, requestCode)
                dismiss()
            }
        }

        binding.addButton.setOnClickListener {
            onAddClicked(requestCode)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    abstract fun onColorClicked(item: Pair<String, Int>?, requestCode: Int)

    open fun onAddClicked(requestCode: Int) {}

    companion object {
        private const val KEY_TITLE_ID = "title_id"
        private const val KEY_TITLE = "title"
        private const val KEY_COLOR_COUNT = "color_count"
        private const val KEY_COLORS_DESCRIPTION = "colors_desc"
        private const val KEY_COLORS = "colors"
        private const val KEY_SELECTED_COLOR = "selected_color"
        private const val KEY_DISABLED_COLORS = "disabled_colors"
        private const val KEY_HIDDEN_COLORS = "hidden_colors"
        private const val KEY_FEATURED_COLORS = "featured_colors"
        private const val KEY_COLUMNS = "columns"
        private const val KEY_REQUEST_CODE = "request_code"
        private const val KEY_SHOW_ADD_BUTTON = "show_add_button"

        private const val DEFAULT_NUMBER_Of_COLUMNS = 4

        /**
         * Constructor
         *
         * @param titleResId     title resource id
         * @param colors         list of colors and their description
         * @param featuredColors subset of the list of colors that should be featured above the rest
         * @param selectedColor  selected color
         * @param disabledColors colors that should be displayed as disabled (but still selectable
         * @param hiddenColors   colors that should be hidden
         * @param columns        number of columns
         * @return new ColorPickerDialog
         */
        fun createBundle(
            @StringRes titleResId: Int,
            colors: List<Pair<String, Int>>,
            featuredColors: ArrayList<String>? = null,
            selectedColor: String? = null,
            disabledColors: ArrayList<String>? = null,
            columns: Int = DEFAULT_NUMBER_Of_COLUMNS,
            requestCode: Int = 0,
            hiddenColors: ArrayList<String>? = null,
            showAddButton: Boolean = false
        ): Bundle {
            return Bundle().apply {
                putInt(KEY_TITLE_ID, titleResId)
                buildBundle(colors, featuredColors, selectedColor, disabledColors, hiddenColors, columns, requestCode, showAddButton)
            }
        }

        /**
         * Constructor
         *
         * @param title          title
         * @param colors         list of colors and their description
         * @param featuredColors subset of the list of colors that should be featured above the rest
         * @param selectedColor  selected color
         * @param disabledColors colors that should be displayed as disabled (but still selectable
         * @param hiddenColors   colors that should be hidden
         * @param columns        number of columns
         * @return new ColorPickerDialog
         */
        fun createBundle(
            title: String,
            colors: List<Pair<String, Int>>,
            featuredColors: ArrayList<String>?,
            selectedColor: String?,
            disabledColors: ArrayList<String>?,
            columns: Int = DEFAULT_NUMBER_Of_COLUMNS,
            requestCode: Int = 0,
            hiddenColors: ArrayList<String>? = null,
            showAddButton: Boolean = false
        ): Bundle {
            return Bundle().apply {
                putString(KEY_TITLE, title)
                buildBundle(colors, featuredColors, selectedColor, disabledColors, hiddenColors, columns, requestCode, showAddButton)
            }
        }

        private fun Bundle.buildBundle(
            colors: List<Pair<String, Int>>,
            featuredColors: ArrayList<String>?,
            selectedColor: String?,
            disabledColors: ArrayList<String>?,
            hiddenColors: ArrayList<String>?,
            columns: Int,
            requestCode: Int,
            showAddButton: Boolean = false
        ) {
            putInt(KEY_COLOR_COUNT, colors.size)
            for (i in colors.indices) {
                val color = colors[i]
                putString(KEY_COLORS_DESCRIPTION + i, color.first)
                putInt(KEY_COLORS + i, color.second)
            }
            putStringArrayList(KEY_FEATURED_COLORS, featuredColors)
            putString(KEY_SELECTED_COLOR, selectedColor)
            putStringArrayList(KEY_DISABLED_COLORS, disabledColors)
            putStringArrayList(KEY_HIDDEN_COLORS, hiddenColors)
            putInt(KEY_COLUMNS, columns)
            putInt(KEY_REQUEST_CODE, requestCode)
            putBoolean(KEY_SHOW_ADD_BUTTON, showAddButton)
        }
    }
}
