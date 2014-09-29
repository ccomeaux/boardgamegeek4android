package com.boardgamegeek.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import butterknife.ButterKnife;
import butterknife.InjectView;

import com.actionbarsherlock.app.SherlockFragment;
import com.boardgamegeek.R;
import com.boardgamegeek.util.PreferencesUtils;

public class PlayStatsFragment extends SherlockFragment {

	@InjectView(R.id.progress) View mProgressView;
	@InjectView(R.id.data) View mDataView;
	@InjectView(R.id.hindex) TextView mHIndex;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_play_stats, container, false);

		ButterKnife.inject(this, rootView);

		return rootView;
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		mHIndex.setText(String.valueOf(PreferencesUtils.getHIndex(getActivity())));

		mDataView.setVisibility(View.VISIBLE);
		mProgressView.setVisibility(View.GONE);
	}
}
