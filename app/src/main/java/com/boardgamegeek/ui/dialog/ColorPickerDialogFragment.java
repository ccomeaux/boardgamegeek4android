package com.boardgamegeek.ui.dialog;

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
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

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnItemClick;

public class ColorPickerDialogFragment extends DialogFragment {
	private static final String KEY_TITLE_ID = "title_id";
	private static final String KEY_COLOR_COUNT = "color_count";
	private static final String KEY_COLORS_DESCRIPTION = "colors_desc";
	private static final String KEY_COLORS = "colors";
	private static final String KEY_FEATURED_COLORS = "featured_colors";
	private static final String KEY_USED_COLORS = "used_colors";
	private static final String KEY_SELECTED_COLOR = "selected_color";
	private static final String KEY_COLUMNS = "columns";

	@InjectView(R.id.color_grid) GridView mColorGrid;
	@InjectView(R.id.featured_color_grid) GridView mFeaturedColorGrid;
	@InjectView(R.id.hr) View mDivider;

	private ColorGridAdapter mAdapter;
	private ColorGridAdapter mFeaturedAdapter;
	private List<Pair<String, Integer>> mColorChoices = new ArrayList<>();
	private ArrayList<String> mFeaturedColors = new ArrayList<>();
	private int mNumColumns = 3;
	private String mSelectedColor;
	private ArrayList<String> mUsedColors;
	private int mTitleResId = 0;
	private OnColorSelectedListener mListener;

	public ColorPickerDialogFragment() {
	}

	public static ColorPickerDialogFragment newInstance() {
		return new ColorPickerDialogFragment();
	}

	/**
	 * Constructor
	 *
	 * @param titleResId     title resource id
	 * @param colors         list of colors and their description
	 * @param featuredColors subset of the list of colors that should be featured above the rest
	 * @param selectedColor  selected color
	 * @param usedColors     colors already used in this context
	 * @param columns        number of columns
	 * @return new ColorPickerDialog
	 */
	public static ColorPickerDialogFragment newInstance(int titleResId, List<Pair<String, Integer>> colors,
														ArrayList<String> featuredColors, String selectedColor,
														ArrayList<String> usedColors, int columns) {
		ColorPickerDialogFragment colorPicker = ColorPickerDialogFragment.newInstance();
		colorPicker.initialize(titleResId, colors, featuredColors, selectedColor, usedColors, columns);
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
		mListener = listener;
	}

	/**
	 * Initialize the dialog picker
	 *
	 * @param titleResId    title resource id
	 * @param colors        list of colors and their description
	 * @param selectedColor selected color
	 * @param columns       number of columns
	 */
	public void initialize(int titleResId, List<Pair<String, Integer>> colors, ArrayList<String> featuredColors,
						   String selectedColor, ArrayList<String> usedColors, int columns) {
		mColorChoices = colors;
		mFeaturedColors = featuredColors;
		mNumColumns = columns;
		mSelectedColor = selectedColor;
		mUsedColors = usedColors;
		if (titleResId > 0) {
			mTitleResId = titleResId;
		}
		setArguments(mTitleResId, mNumColumns);
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		if (mColorChoices.size() > 0)
			tryBindLists();
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt(KEY_COLOR_COUNT, mColorChoices.size());
		for (int i = 0; i < mColorChoices.size(); i++) {
			Pair<String, Integer> color = mColorChoices.get(i);
			outState.putString(KEY_COLORS_DESCRIPTION + i, color.first);
			outState.putInt(KEY_COLORS + i, color.second);
		}
		outState.putStringArrayList(KEY_FEATURED_COLORS, mFeaturedColors);
		outState.putStringArrayList(KEY_USED_COLORS, mUsedColors);
		outState.putString(KEY_SELECTED_COLOR, mSelectedColor);
	}

	@Override
	@NonNull
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		LayoutInflater layoutInflater = LayoutInflater.from(getActivity());
		View rootView = layoutInflater.inflate(R.layout.dialog_colors, null); // TODO provide root

		if (getArguments() != null) {
			mTitleResId = getArguments().getInt(KEY_TITLE_ID);
			mNumColumns = getArguments().getInt(KEY_COLUMNS);
		}

		if (savedInstanceState != null) {
			mColorChoices = new ArrayList<>();
			for (int i = 0; i < savedInstanceState.getInt(KEY_COLOR_COUNT); i++) {
				mColorChoices.add(new Pair<>(savedInstanceState.getString(KEY_COLORS_DESCRIPTION + i),
					savedInstanceState.getInt(KEY_COLORS + i)));
			}
			mFeaturedColors = savedInstanceState.getStringArrayList(KEY_FEATURED_COLORS);
			mUsedColors = savedInstanceState.getStringArrayList(KEY_USED_COLORS);
			mSelectedColor = savedInstanceState.getString(KEY_SELECTED_COLOR);
		}

		ButterKnife.inject(this, rootView);
		mColorGrid.setNumColumns(mNumColumns);
		mFeaturedColorGrid.setNumColumns(mNumColumns);

		tryBindLists();
		mDivider.setVisibility(mFeaturedColors == null ? View.GONE : View.VISIBLE);

		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity()).setView(rootView);
		if (mTitleResId > 0) {
			builder.setTitle(mTitleResId);
		}
		return builder.create();
	}

	@OnItemClick({ R.id.color_grid, R.id.featured_color_grid })
	public void onItemClick(AdapterView<?> listView, View view, int position, long itemId) {
		if (mListener != null) {
			@SuppressWarnings("unchecked")
			Pair<String, Integer> item = (Pair<String, Integer>) listView.getAdapter().getItem(position);
			mListener.onColorSelected(item.first, item.second);
		}
		dismiss();
	}

	private void tryBindLists() {
		if (isAdded() && mAdapter == null) {
			if (mFeaturedColors == null) {
				mAdapter = new ColorGridAdapter(mColorChoices);
				mFeaturedAdapter = null;
			} else {
				ArrayList<Pair<String, Integer>> choices = new ArrayList<>(mColorChoices);
				ArrayList<Pair<String, Integer>> features = new ArrayList<>();
				for (int i = mColorChoices.size() - 1; i >= 0; i--) {
					Pair<String, Integer> pair = choices.get(i);
					if (mFeaturedColors.contains(pair.first)) {
						choices.remove(i);
						features.add(0, pair);
					}
				}
				mAdapter = new ColorGridAdapter(choices);
				mFeaturedAdapter = new ColorGridAdapter(features);
			}
		}

		if (mAdapter != null && mColorGrid != null) {
			mAdapter.setSelectedColor(mSelectedColor);
			mColorGrid.setAdapter(mAdapter);
		}

		if (mFeaturedAdapter != null && mFeaturedColorGrid != null) {
			mFeaturedAdapter.setSelectedColor(mSelectedColor);
			mFeaturedColorGrid.setAdapter(mFeaturedAdapter);
		}
	}

	private class ColorGridAdapter extends BaseAdapter {
		private List<Pair<String, Integer>> mChoices = new ArrayList<>();
		private String mSelectedColor;

		private ColorGridAdapter(List<Pair<String, Integer>> choices) {
			mChoices = choices;
		}

		@Override
		public int getCount() {
			return mChoices.size();
		}

		@Override
		public Pair<String, Integer> getItem(int position) {
			return mChoices.get(position);
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
			if (color.first.equals(mSelectedColor)) {
				frame.setBackgroundColor(getResources().getColor(R.color.primary));
			} else if (mUsedColors != null && mUsedColors.contains(color.first)) {
				frame.setBackgroundColor(getResources().getColor(R.color.disabled));
			}

			return convertView;
		}

		public void setSelectedColor(String selectedColor) {
			if (mSelectedColor == null ||
				!mSelectedColor.equals(selectedColor)) {
				mSelectedColor = selectedColor;
				notifyDataSetChanged();
			}
		}
	}
}