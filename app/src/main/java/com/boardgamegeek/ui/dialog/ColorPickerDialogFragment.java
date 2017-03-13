package com.boardgamegeek.ui.dialog;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.util.ColorUtils;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnItemClick;
import butterknife.Unbinder;
import icepick.Icepick;
import icepick.State;

public class ColorPickerDialogFragment extends DialogFragment {
	private static final String KEY_TITLE_ID = "title_id";
	private static final String KEY_COLOR_COUNT = "color_count";
	private static final String KEY_COLORS_DESCRIPTION = "colors_desc";
	private static final String KEY_COLORS = "colors";
	private static final String KEY_COLUMNS = "columns";

	private Unbinder unbinder;
	@BindView(R.id.color_grid) GridView colorGrid;
	@BindView(R.id.featured_color_grid) GridView featuredColorGrid;
	@BindView(R.id.hr) View divider;

	private ColorGridAdapter colorGridAdapter;
	private ColorGridAdapter featuredColorGridAdapter;
	private List<Pair<String, Integer>> colorChoices = new ArrayList<>();
	@State ArrayList<String> featuredColors = new ArrayList<>();
	private int numberOfColumns = 3;
	@State String selectedColor;
	@State ArrayList<String> disabledColors;
	@State ArrayList<String> hiddenColors;
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

	public void setArguments(int titleResId, int columns) {
		Bundle bundle = new Bundle();
		bundle.putInt(KEY_TITLE_ID, titleResId);
		bundle.putInt(KEY_COLUMNS, columns);
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

	private void initialize(@StringRes int titleResId,
							List<Pair<String, Integer>> colors,
							ArrayList<String> featuredColors,
							String selectedColor,
							ArrayList<String> disabledColors,
							ArrayList<String> hiddenColors,
							int columns) {
		colorChoices = colors;
		this.featuredColors = featuredColors;
		numberOfColumns = columns;
		this.selectedColor = selectedColor;
		this.disabledColors = disabledColors;
		this.hiddenColors = hiddenColors;
		if (titleResId > 0) this.titleResId = titleResId;
		setArguments(this.titleResId, numberOfColumns);
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (colorChoices.size() > 0) tryBindLists();
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		Icepick.saveInstanceState(this, outState);
		outState.putInt(KEY_COLOR_COUNT, colorChoices.size());
		for (int i = 0; i < colorChoices.size(); i++) {
			Pair<String, Integer> color = colorChoices.get(i);
			outState.putString(KEY_COLORS_DESCRIPTION + i, color.first);
			outState.putInt(KEY_COLORS + i, color.second);
		}
	}

	@Override
	@NonNull
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		LayoutInflater layoutInflater = LayoutInflater.from(getActivity());
		@SuppressLint("InflateParams") View rootView = layoutInflater.inflate(R.layout.dialog_colors, null);

		if (getArguments() != null) {
			titleResId = getArguments().getInt(KEY_TITLE_ID);
			numberOfColumns = getArguments().getInt(KEY_COLUMNS);
		}
		if (savedInstanceState != null) {
			colorChoices = new ArrayList<>();
			for (int i = 0; i < savedInstanceState.getInt(KEY_COLOR_COUNT); i++) {
				colorChoices.add(new Pair<>(savedInstanceState.getString(KEY_COLORS_DESCRIPTION + i),
					savedInstanceState.getInt(KEY_COLORS + i)));
			}
		}
		Icepick.restoreInstanceState(this, savedInstanceState);

		unbinder = ButterKnife.bind(this, rootView);
		colorGrid.setNumColumns(numberOfColumns);
		featuredColorGrid.setNumColumns(numberOfColumns);

		tryBindLists();
		divider.setVisibility(featuredColors == null ? View.GONE : View.VISIBLE);

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
			if (featuredColors == null) {
				colorGridAdapter = new ColorGridAdapter(choices);
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
				colorGridAdapter = new ColorGridAdapter(choices);
				featuredColorGridAdapter = new ColorGridAdapter(features);
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

	private class ColorGridAdapter extends BaseAdapter {
		private List<Pair<String, Integer>> choices = new ArrayList<>();
		private String selectedColor;

		private ColorGridAdapter(List<Pair<String, Integer>> choices) {
			this.choices = choices;
		}

		@Override
		public int getCount() {
			return choices.size();
		}

		@Override
		public Pair<String, Integer> getItem(int position) {
			return choices.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup container) {
			if (convertView == null) {
				convertView = LayoutInflater.from(getActivity()).inflate(R.layout.widget_color, container, false);
			}

			Pair<String, Integer> color = getItem(position);
			((TextView) convertView.findViewById(R.id.color_description)).setText(color.first);
			ColorUtils.setColorViewValue(convertView.findViewById(R.id.color_view), color.second);
			View frame = convertView.findViewById(R.id.color_frame);
			if (color.first.equals(selectedColor)) {
				frame.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.light_blue));
			} else if (disabledColors != null && disabledColors.contains(color.first)) {
				frame.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.disabled));
			}

			return convertView;
		}

		public void setSelectedColor(String selectedColor) {
			if (this.selectedColor == null ||
				!this.selectedColor.equals(selectedColor)) {
				this.selectedColor = selectedColor;
				notifyDataSetChanged();
			}
		}
	}
}