package com.boardgamegeek.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.boardgamegeek.R;

import butterknife.ButterKnife;
import butterknife.OnClick;

public class PlaysSummaryFragment extends Fragment {
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_plays_summary, container, false);

		ButterKnife.inject(this, rootView);

		return rootView;
	}

	@OnClick(R.id.container_plays)
	public void onPlaysClick(View v) {
		startActivity(new Intent(getActivity(), PlaysActivity.class));
	}

	@OnClick(R.id.container_players)
	public void onPlayersClick(View v) {
		startActivity(new Intent(getActivity(), PlayersActivity.class));
	}

	@OnClick(R.id.container_locations)
	public void onLocationsClick(View v) {
		startActivity(new Intent(getActivity(), LocationsActivity.class));
	}

	@OnClick(R.id.container_stats)
	public void onStatsClick(View v) {
		startActivity(new Intent(getActivity(), PlayStatsActivity.class));
	}
}
