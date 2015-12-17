package com.boardgamegeek.ui.dialog;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;

import com.boardgamegeek.R;
import com.boardgamegeek.sorter.CollectionSorter;
import com.boardgamegeek.sorter.CollectionSorterFactory;
import com.boardgamegeek.util.StringUtils;

import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.InjectViews;
import hugo.weaving.DebugLog;
import timber.log.Timber;

public class CollectionSortDialogFragment extends DialogFragment implements OnCheckedChangeListener {
	public interface Listener {
		void onSortSelected(int sortType);
	}

	private ViewGroup root;
	private Listener listener;
	private int selectedType;
	@SuppressWarnings("unused") @InjectView(R.id.radio_group) RadioGroup radioGroup;
	@SuppressWarnings("unused") @InjectViews({
		R.id.name,
		R.id.rank,
		R.id.geek_rating,
		R.id.average_rating,
		R.id.my_rating,
		R.id.last_viewed,
		R.id.wishlist_priority,
		R.id.play_count,
		R.id.year_published_asc,
		R.id.year_published_desc,
		R.id.play_time_asc,
		R.id.play_time_desc,
		R.id.suggested_age_asc,
		R.id.suggested_age_desc,
		R.id.average_weight_asc,
		R.id.average_weight_desc,
		R.id.acquisition_date,
	}) List<RadioButton> radioButtons;

	@DebugLog
	public CollectionSortDialogFragment() {
	}

	@DebugLog
	public static CollectionSortDialogFragment newInstance(@Nullable ViewGroup root, @Nullable Listener listener) {
		final CollectionSortDialogFragment fragment = new CollectionSortDialogFragment();
		fragment.initialize(root, listener);
		return fragment;
	}

	@DebugLog
	private void initialize(@Nullable ViewGroup root, @Nullable Listener listener) {
		this.root = root;
		this.listener = listener;
	}

	@DebugLog
	@Override
	@NonNull
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		LayoutInflater layoutInflater = LayoutInflater.from(getActivity());
		View rootView = layoutInflater.inflate(R.layout.dialog_collection_sort, root, false);

		ButterKnife.inject(this, rootView);
		setChecked();
		radioGroup.setOnCheckedChangeListener(this);
		createNames();

		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity()).setView(rootView);
		builder.setTitle(R.string.title_sort);
		return builder.create();
	}

	@DebugLog
	private void createNames() {
		CollectionSorterFactory factory = new CollectionSorterFactory(getActivity());
		for (RadioButton radioButton : radioButtons) {
			int sortType = getTypeFromView(radioButton);
			CollectionSorter sorter = factory.create(sortType);
			radioButton.setText(sorter.getDescription());
		}
	}

	@DebugLog
	@Override
	public void onCheckedChanged(RadioGroup group, int checkedId) {
		int sortType = getTypeFromView(group.findViewById(checkedId));
		Timber.d("Sort by " + sortType);
		if (listener != null) {
			listener.onSortSelected(sortType);
		}
		dismiss();
	}

	@DebugLog
	public void setSelection(int type) {
		selectedType = type;
		setChecked();
	}

	@DebugLog
	private void setChecked() {
		if (radioButtons != null) {
			for (RadioButton radioButton : radioButtons) {
				int type = getTypeFromView(radioButton);
				if (type == selectedType) {
					radioGroup.setOnCheckedChangeListener(null);
					radioButton.setChecked(true);
					radioGroup.setOnCheckedChangeListener(this);
					return;
				}
			}
		}
	}

	@DebugLog
	private int getTypeFromView(View view) {
		return StringUtils.parseInt(view.getTag().toString(), CollectionSorterFactory.TYPE_UNKNOWN);
	}
}
