package com.boardgamegeek.ui.dialog;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatCheckBox;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;

import com.boardgamegeek.R;
import com.boardgamegeek.filterer.CollectionFiltererFactory;
import com.boardgamegeek.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.BindViews;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import hugo.weaving.DebugLog;
import timber.log.Timber;

public class CollectionFilterDialogFragment extends DialogFragment {
	public interface Listener {
		void onFilterSelected(int filterType);
	}

	private ViewGroup root;
	private Listener listener;
	private final List<Integer> enabledFilterTypes = new ArrayList<>();
	private Unbinder unbinder;
	@BindView(R.id.scroll_container) ScrollView scrollContainer;
	@BindViews({
		R.id.collection_name,
		R.id.collection_status,
		R.id.subtype,
		R.id.geek_ranking,
		R.id.geek_rating,
		R.id.average_rating,
		R.id.my_rating,
		R.id.number_of_players,
		R.id.play_count,
		R.id.year_published,
		R.id.play_time,
		R.id.suggested_age,
		R.id.average_weight
	}) List<AppCompatCheckBox> checkBoxes;

	@DebugLog
	public CollectionFilterDialogFragment() {
	}

	@DebugLog
	public static CollectionFilterDialogFragment newInstance(@Nullable ViewGroup root, @Nullable Listener listener) {
		final CollectionFilterDialogFragment fragment = new CollectionFilterDialogFragment();
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
		View rootView = layoutInflater.inflate(R.layout.dialog_collection_filter, root, false);

		unbinder = ButterKnife.bind(this, rootView);
		setEnabledFilterTypes();

		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), R.style.Theme_bgglight_Dialog_Alert).setView(rootView);
		builder.setTitle(R.string.title_filter);
		return builder.create();
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		if (unbinder != null) unbinder.unbind();
	}

	@DebugLog
	public void addEnabledFilter(int type) {
		enabledFilterTypes.add(type);
		setEnabledFilterTypes();
	}

	@DebugLog
	private void setEnabledFilterTypes() {
		if (checkBoxes == null) {
			Timber.d("Text views not initialized");
			return;
		}
		for (Integer filterType : enabledFilterTypes) {
			for (AppCompatCheckBox textView : checkBoxes) {
				int type = getTypeFromView(textView);
				if (filterType == type) {
					textView.setChecked(true);
					break;
				}
			}
		}
	}

	@DebugLog
	private int getTypeFromView(View view) {
		return StringUtils.parseInt(view.getTag().toString(), CollectionFiltererFactory.TYPE_UNKNOWN);
	}

	@DebugLog
	@OnClick({
		R.id.collection_name,
		R.id.collection_status,
		R.id.subtype,
		R.id.geek_ranking,
		R.id.geek_rating,
		R.id.average_rating,
		R.id.my_rating,
		R.id.number_of_players,
		R.id.play_count,
		R.id.year_published,
		R.id.play_time,
		R.id.suggested_age,
		R.id.average_weight })
	void onClick(View view) {
		int type = StringUtils.parseInt(view.getTag().toString(), -1);
		if (type != -1) {
			Timber.d("Filter by %s", type);
			if (listener != null) {
				listener.onFilterSelected(type);
			}
		} else {
			Timber.w("Invalid filter type selected: %s", type);
		}
		dismiss();
	}
}
