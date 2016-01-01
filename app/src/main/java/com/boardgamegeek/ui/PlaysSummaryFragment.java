package com.boardgamegeek.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.boardgamegeek.R;
import com.boardgamegeek.auth.AccountUtils;
import com.boardgamegeek.util.ActivityUtils;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;

public class PlaysSummaryFragment extends Fragment {
	@SuppressWarnings("unused") @InjectView(R.id.card_colors) View colorsCard;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_plays_summary, container, false);

		ButterKnife.inject(this, rootView);
		colorsCard.setVisibility(TextUtils.isEmpty(AccountUtils.getUsername(getActivity())) ? View.GONE : View.VISIBLE);

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

	@OnClick(R.id.container_colors)
	public void onColorsClick(View v) {
		Intent intent = new Intent(getActivity(), BuddyColorsActivity.class);
		intent.putExtra(ActivityUtils.KEY_BUDDY_NAME, AccountUtils.getUsername(getActivity()));
		startActivity(intent);
	}

	@OnClick(R.id.container_stats)
	public void onStatsClick(View v) {
		startActivity(new Intent(getActivity(), PlayStatsActivity.class));
	}
}
