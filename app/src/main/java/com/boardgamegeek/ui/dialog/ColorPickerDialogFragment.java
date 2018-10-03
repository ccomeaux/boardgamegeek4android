package com.boardgamegeek.ui.dialog;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;

import com.boardgamegeek.R;
import com.boardgamegeek.ui.adapter.ColorGridAdapter;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnItemClick;
import butterknife.Unbinder;

public class ColorPickerDialogFragment extends DialogFragment {
	private static final String KEY_TITLE_ID = "title_id";
	private static final String KEY_COLOR_COUNT = "color_count";
	private static final String KEY_COLORS_DESCRIPTION = "colors_desc";
	private static final String KEY_COLORS = "colors";
	private static final String KEY_SELECTED_COLOR = "selected_color";
	private static final String KEY_DISABLED_COLORS = "disabled_colors";
	private static final String KEY_HIDDEN_COLORS = "hidden_colors";
	private static final String KEY_FEATURED_COLORS = "featured_colors";
	private static final String KEY_COLUMNS = "columns";

	private Unbinder unbinder;
	@BindView(R.id.color_grid) GridView colorGrid;
	@BindView(R.id.featured_color_grid) GridView featuredColorGrid;
	@BindView(R.id.hr) View divider;

	private ColorGridAdapter colorGridAdapter;
	private ColorGridAdapter featuredColorGridAdapter;
	private List<Pair<String, Integer>> colorChoices = new ArrayList<>();
	private ArrayList<String> featuredColors;
	private int numberOfColumns = 3;
	private String selectedColor;
	private ArrayList<String> disabledColors;
	private ArrayList<String> hiddenColors;
	@StringRes private int titleResId = 0;
	private OnColorSelectedListener listener;

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
	public static ColorPickerDialogFragment newInstance(@StringRes int titleResId,
														List<Pair<String, Integer>> colors,
														ArrayList<String> featuredColors,
														String selectedColor,
														ArrayList<String> disabledColors,
														ArrayList<String> hiddenColors,
														int columns) {
		ColorPickerDialogFragment colorPicker = new ColorPickerDialogFragment();
		colorPicker.initialize(titleResId, colors, featuredColors, selectedColor, disabledColors, hiddenColors, columns);
		return colorPicker;
	}

	private void initialize(@StringRes int titleResId,
							List<Pair<String, Integer>> colors,
							ArrayList<String> featuredColors,
							String selectedColor,
							ArrayList<String> disabledColors,
							ArrayList<String> hiddenColors,
							int columns) {
		colorChoices = colors;
		setArguments(titleResId, featuredColors, selectedColor, disabledColors, hiddenColors, columns);
	}

	public void setArguments(int titleResId, ArrayList<String> featuredColors, String selectedColor, ArrayList<String> disabledColors, ArrayList<String> hiddenColors, int columns) {
		Bundle bundle = new Bundle();
		bundle.putInt(KEY_TITLE_ID, titleResId);
		bundle.putStringArrayList(KEY_FEATURED_COLORS, featuredColors);
		bundle.putString(KEY_SELECTED_COLOR, selectedColor);
		bundle.putStringArrayList(KEY_DISABLED_COLORS, disabledColors);
		bundle.putStringArrayList(KEY_HIDDEN_COLORS, hiddenColors);
		bundle.putInt(KEY_COLUMNS, columns);
		bundle.putInt(KEY_COLOR_COUNT, colorChoices.size());
		for (int i = 0; i < colorChoices.size(); i++) {
			Pair<String, Integer> color = colorChoices.get(i);
			bundle.putString(KEY_COLORS_DESCRIPTION + i, color.first);
			bundle.putInt(KEY_COLORS + i, color.second);
		}
		setArguments(bundle);
	}

	/**
	 * Interface for a callback when a color square is selected.
	 */
	public interface OnColorSelectedListener {

		/**
		 * Called when a specific color square has been selected.
		 */
		void onColorSelected(String description, int color);
	}

	public void setOnColorSelectedListener(OnColorSelectedListener listener) {
		this.listener = listener;
	}

	@Override
	@NonNull
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		@SuppressLint("InflateParams")
		View rootView = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_colors, null);

		if (getArguments() != null) {
			titleResId = getArguments().getInt(KEY_TITLE_ID);
			featuredColors = getArguments().getStringArrayList(KEY_FEATURED_COLORS);
			selectedColor = getArguments().getString(KEY_SELECTED_COLOR);
			disabledColors = getArguments().getStringArrayList(KEY_DISABLED_COLORS);
			hiddenColors = getArguments().getStringArrayList(KEY_HIDDEN_COLORS);
			numberOfColumns = getArguments().getInt(KEY_COLUMNS);

			colorChoices = new ArrayList<>();
			for (int i = 0; i < getArguments().getInt(KEY_COLOR_COUNT); i++) {
				colorChoices.add(new Pair<>(getArguments().getString(KEY_COLORS_DESCRIPTION + i), getArguments().getInt(KEY_COLORS + i)));
			}
		}

		unbinder = ButterKnife.bind(this, rootView);
		colorGrid.setNumColumns(numberOfColumns);
		featuredColorGrid.setNumColumns(numberOfColumns);

		tryBindLists();
		divider.setVisibility(featuredColors == null || featuredColors.size() == 0 ? View.GONE : View.VISIBLE);

		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), R.style.Theme_bgglight_Dialog_Alert).setView(rootView);
		if (titleResId > 0) {
			builder.setTitle(titleResId);
		}
		return builder.create();
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		if (unbinder != null) unbinder.unbind();
	}

	@SuppressWarnings("unused")
	@OnItemClick({ R.id.color_grid, R.id.featured_color_grid })
	public void onItemClick(AdapterView<?> listView, View view, int position, long itemId) {
		if (listener != null) {
			@SuppressWarnings("unchecked")
			Pair<String, Integer> item = (Pair<String, Integer>) listView.getAdapter().getItem(position);
			listener.onColorSelected(item.first, item.second);
		}
		dismiss();
	}

	private void tryBindLists() {
		if (isAdded() && colorGridAdapter == null) {
			ArrayList<Pair<String, Integer>> choices = new ArrayList<>(colorChoices);
			if (hiddenColors != null) {
				for (int i = colorChoices.size() - 1; i >= 0; i--) {
					Pair<String, Integer> pair = choices.get(i);
					if (hiddenColors.contains(pair.first)) {
						choices.remove(i);
					}
				}
			}
			colorGridAdapter = new ColorGridAdapter(getContext(), disabledColors, choices);
			if (featuredColors == null) {
				featuredColorGridAdapter = null;
			} else {
				ArrayList<Pair<String, Integer>> features = new ArrayList<>();
				for (int i = colorChoices.size() - 1; i >= 0; i--) {
					Pair<String, Integer> pair = choices.get(i);
					if (featuredColors.contains(pair.first)) {
						choices.remove(i);
						features.add(0, pair);
					}
				}
				featuredColorGridAdapter = new ColorGridAdapter(getContext(), disabledColors, features);
			}
		}

		if (colorGridAdapter != null && colorGrid != null) {
			colorGridAdapter.setSelectedColor(selectedColor);
			colorGrid.setAdapter(colorGridAdapter);
		}

		if (featuredColorGridAdapter != null && featuredColorGrid != null) {
			featuredColorGridAdapter.setSelectedColor(selectedColor);
			featuredColorGrid.setAdapter(featuredColorGridAdapter);
		}
	}

}