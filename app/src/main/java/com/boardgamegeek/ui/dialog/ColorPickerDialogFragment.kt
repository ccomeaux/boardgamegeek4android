package com.boardgamegeek.ui.dialog

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.support.annotation.StringRes
import android.support.v4.app.DialogFragment
import android.support.v7.app.AlertDialog
import android.util.Pair
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.boardgamegeek.R
import com.boardgamegeek.ui.adapter.ColorGridAdapter
import kotlinx.android.synthetic.main.dialog_colors.*
import org.jetbrains.anko.support.v4.ctx
import java.util.*

class ColorPickerDialogFragment : DialogFragment() {
    private lateinit var layout: View
    private var listener: Listener? = null

    interface Listener {
        fun onColorSelected(description: String, color: Int, requestCode: Int)
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        listener = context as? Listener
        if (listener == null) throw ClassCastException("$context must implement Listener")
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        @SuppressLint("InflateParams")
        layout = LayoutInflater.from(ctx).inflate(R.layout.dialog_colors, null)

        val builder = AlertDialog.Builder(ctx, R.style.Theme_bgglight_Dialog_Alert).setView(layout)
        @StringRes val titleResId = arguments?.getInt(KEY_TITLE_ID) ?: 0
        if (titleResId > 0) builder.setTitle(titleResId)
        return builder.create()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return layout
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val colorChoices: MutableList<Pair<String, Int>> = ArrayList()
        for (i in 0 until (arguments?.getInt(KEY_COLOR_COUNT) ?: 0)) {
            val name = arguments?.getString(KEY_COLORS_DESCRIPTION + i) ?: ""
            val color = arguments?.getInt(KEY_COLORS + i) ?: Color.TRANSPARENT
            colorChoices.add(Pair(name, color))
        }
        val selectedColor = arguments?.getString(KEY_SELECTED_COLOR)
        val hiddenColors = arguments?.getStringArrayList(KEY_HIDDEN_COLORS)
        val disabledColors = arguments?.getStringArrayList(KEY_DISABLED_COLORS)
        val featuredColors = arguments?.getStringArrayList(KEY_FEATURED_COLORS)
        val requestCode = arguments?.getInt(KEY_REQUEST_CODE) ?: 0

        val numColumns = arguments?.getInt(KEY_COLUMNS) ?: DEFAULT_NUMBER_Of_COLUMNS
        colorGrid.numColumns = numColumns
        featuredColorGrid.numColumns = numColumns

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
        val featuredColorGridAdapter = if (featuredColors == null) {
            null
        } else {
            val features = ArrayList<Pair<String, Int>>()
            for (i in choices.indices.reversed()) {
                val pair = choices[i]
                if (featuredColors.contains(pair.first)) {
                    choices.removeAt(i)
                    features.add(0, pair)
                }
            }
            ColorGridAdapter(ctx, disabledColors, features)
        }

        val colorGridAdapter = ColorGridAdapter(ctx, disabledColors, choices)
        colorGridAdapter.setSelectedColor(selectedColor)
        colorGrid.adapter = colorGridAdapter

        if (featuredColorGridAdapter != null) {
            featuredColorGridAdapter.setSelectedColor(selectedColor)
            featuredColorGrid.adapter = featuredColorGridAdapter
        }

        dividerView.visibility = if (featuredColors?.isNotEmpty() == true) View.VISIBLE else View.GONE

        listOf(colorGrid, featuredColorGrid).forEach {
            it.setOnItemClickListener { parent, _, position, _ ->
                val adapter = parent.adapter as? ColorGridAdapter
                val item = adapter?.getItem(position)
                if (item != null) listener?.onColorSelected(item.first, item.second, requestCode)
                dismiss()
            }
        }
    }

    companion object {
        private const val KEY_TITLE_ID = "title_id"
        private const val KEY_COLOR_COUNT = "color_count"
        private const val KEY_COLORS_DESCRIPTION = "colors_desc"
        private const val KEY_COLORS = "colors"
        private const val KEY_SELECTED_COLOR = "selected_color"
        private const val KEY_DISABLED_COLORS = "disabled_colors"
        private const val KEY_HIDDEN_COLORS = "hidden_colors"
        private const val KEY_FEATURED_COLORS = "featured_colors"
        private const val KEY_COLUMNS = "columns"
        private const val KEY_REQUEST_CODE = "request_code"

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
        @JvmStatic
        @JvmOverloads
        fun newInstance(@StringRes titleResId: Int,
                        colors: List<Pair<String, Int>>,
                        featuredColors: ArrayList<String>?,
                        selectedColor: String?,
                        disabledColors: ArrayList<String>?,
                        columns: Int = DEFAULT_NUMBER_Of_COLUMNS,
                        requestCode: Int = 0,
                        hiddenColors: ArrayList<String>? = null): ColorPickerDialogFragment {
            return ColorPickerDialogFragment().apply {
                arguments = Bundle().apply {
                    putInt(KEY_TITLE_ID, titleResId)
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
                }
            }
        }
    }
}