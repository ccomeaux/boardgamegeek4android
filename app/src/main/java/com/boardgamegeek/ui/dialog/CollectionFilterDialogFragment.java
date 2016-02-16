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
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.util.StringUtils;

import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.InjectViews;
import butterknife.OnClick;
import hugo.weaving.DebugLog;
import timber.log.Timber;

public class CollectionFilterDialogFragment extends DialogFragment {
	public interface Listener {
		void onFilterSelected(int filterType);
	}

	private ViewGroup root;
	private Listener listener;
	@SuppressWarnings("unused") @InjectView(R.id.scroll_container) ScrollView scrollContainer;
	@SuppressWarnings("unused") @InjectView(R.id.container) LinearLayout radioGroup;
	@SuppressWarnings("unused") @InjectViews({
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
	}) List<TextView> textViews;

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

		ButterKnife.inject(this, rootView);

		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity()).setView(rootView);
		builder.setTitle(R.string.title_filter);
		return builder.create();
	}

	@SuppressWarnings("unused")
	@OnClick({
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
			Timber.d("Filter by " + type);
			if (listener != null) {
				listener.onFilterSelected(type);
			}
		} else {
			Timber.w("Invalid filter type selected: " + type);
		}
		dismiss();
	}
}
