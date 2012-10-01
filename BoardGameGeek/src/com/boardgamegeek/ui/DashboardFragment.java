package com.boardgamegeek.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;

import com.actionbarsherlock.app.SherlockFragment;
import com.boardgamegeek.BggApplication;
import com.boardgamegeek.R;
import com.boardgamegeek.pref.Preferences;
import com.boardgamegeek.provider.BggContract.Buddies;
import com.boardgamegeek.provider.BggContract.Collection;
import com.boardgamegeek.provider.BggContract.Plays;

public class DashboardFragment extends SherlockFragment {

	private View mCollection;
	private View mBuddies;
	private View mPlays;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View root = inflater.inflate(R.layout.fragment_dashboard, container);

		mCollection = root.findViewById(R.id.home_btn_collection);
		mCollection.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				startActivity(new Intent(Intent.ACTION_VIEW, Collection.CONTENT_URI));
			}
		});

		root.findViewById(R.id.home_btn_hotness).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				startActivity(new Intent(getActivity(), HotnessActivity.class));
			}
		});

		root.findViewById(R.id.home_btn_forums).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				startActivity(new Intent(getActivity(), ForumsActivity.class));
			}
		});

		mBuddies = root.findViewById(R.id.home_btn_buddies);
		mBuddies.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				startActivity(new Intent(Intent.ACTION_VIEW, Buddies.CONTENT_URI));
			}
		});

		mPlays = root.findViewById(R.id.home_btn_plays);
		mPlays.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				startActivity(new Intent(Intent.ACTION_VIEW, Plays.CONTENT_URI));
			}
		});

		root.findViewById(R.id.home_btn_settings).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				startActivity(new Intent(getActivity(), Preferences.class));
			}
		});

		return root;
	}

	@Override
	public void onResume() {
		super.onResume();
		mCollection.setVisibility((BggApplication.getInstance().getSyncStatuses() != null && BggApplication
			.getInstance().getSyncStatuses().length > 0) ? View.VISIBLE : View.GONE);
		mBuddies.setVisibility(BggApplication.getInstance().getSyncBuddies() ? View.VISIBLE : View.GONE);
		mPlays.setVisibility(BggApplication.getInstance().getSyncPlays() ? View.VISIBLE : View.GONE);
	}
}
