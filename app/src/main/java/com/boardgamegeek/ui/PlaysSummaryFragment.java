package com.boardgamegeek.ui;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.auth.AccountUtils;
import com.boardgamegeek.provider.BggContract.PlayerColors;
import com.boardgamegeek.provider.BggContract.Plays;
import com.boardgamegeek.sorter.LocationsSorter;
import com.boardgamegeek.sorter.LocationsSorterFactory;
import com.boardgamegeek.ui.model.BuddyColor;
import com.boardgamegeek.ui.model.Location;
import com.boardgamegeek.util.ActivityUtils;
import com.boardgamegeek.util.ColorUtils;
import com.boardgamegeek.util.PreferencesUtils;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;

public class PlaysSummaryFragment extends Fragment implements LoaderCallbacks<Cursor> {
	private static final int LOCATIONS_TOKEN = 3;
	private static final int COLORS_TOKEN = 4;

	@SuppressWarnings("unused") @InjectView(R.id.locations_container) LinearLayout locationsContainer;
	@SuppressWarnings("unused") @InjectView(R.id.card_colors) View colorsCard;
	@SuppressWarnings("unused") @InjectView(R.id.color_container) LinearLayout colorContainer;
	@SuppressWarnings("unused") @InjectView(R.id.h_index) TextView hIndexView;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_plays_summary, container, false);

		ButterKnife.inject(this, rootView);
		colorsCard.setVisibility(TextUtils.isEmpty(AccountUtils.getUsername(getActivity())) ? View.GONE : View.VISIBLE);
		//TODO ensure this is bold
		hIndexView.setText(getString(R.string.h_index_prefix, PreferencesUtils.getHIndex(getActivity())));

		return rootView;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		getLoaderManager().restartLoader(LOCATIONS_TOKEN, null, this);
		getLoaderManager().restartLoader(COLORS_TOKEN, null, this);
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		CursorLoader loader = null;
		switch (id) {
			case LOCATIONS_TOKEN:
				// TODO limit to 4 locations
				LocationsSorter sorter = LocationsSorterFactory.create(getActivity(), LocationsSorterFactory.TYPE_QUANTITY);
				loader = new CursorLoader(getActivity(),
					Plays.buildLocationsUri(),
					Location.PROJECTION,
					null, null, sorter.getOrderByClause());
				break;
			case COLORS_TOKEN:
				loader = new CursorLoader(getActivity(),
					PlayerColors.buildUserUri(AccountUtils.getUsername(getActivity())),
					BuddyColor.PROJECTION,
					null, null, null);
				break;
		}
		return loader;
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		if (getActivity() == null) {
			return;
		}

		switch (loader.getId()) {
			case LOCATIONS_TOKEN:
				onLocationsQueryComplete(cursor);
				break;
			case COLORS_TOKEN:
				onColorsQueryComplete(cursor);
				break;
			default:
				cursor.close();
				break;
		}
	}

	private void onLocationsQueryComplete(Cursor cursor) {
		if (cursor == null) {
			return;
		}

		int count = 0;
		while (cursor.moveToNext()) {
			Location location = Location.fromCursor(cursor);

			if (TextUtils.isEmpty(location.getName())) {
				continue;
			}

			View view = getLayoutInflater(null).inflate(R.layout.row_text_2_short, locationsContainer, false);
			locationsContainer.addView(view);
			((TextView) view.findViewById(android.R.id.title)).setText(location.getName());
			((TextView) view.findViewById(android.R.id.text1)).setText(getResources().getQuantityString(R.plurals.plays, location.getPlayCount(), location.getPlayCount()));
			count++;
			if (count >= 3) {
				break;
			}
		}
	}

	private void onColorsQueryComplete(Cursor cursor) {
		if (cursor == null) {
			return;
		}

		if (cursor.getCount() > 0) {
			colorContainer.removeAllViews();
			for (int i = 0; i < 5; i++) {
				if (cursor.moveToNext()) {
					ImageView view = createViewToBeColored();
					BuddyColor color = BuddyColor.fromCursor(cursor);
					ColorUtils.setColorViewValue(view, ColorUtils.parseColor(color.getColor()));
					colorContainer.addView(view);
				} else {
					return;
				}
			}
		}
	}

	private ImageView createViewToBeColored() {
		ImageView view = new ImageView(getActivity());
		int size = getResources().getDimensionPixelSize(R.dimen.color_circle_diameter_small);
		int margin = getResources().getDimensionPixelSize(R.dimen.color_circle_diameter_small_margin);
		LayoutParams lp = new LayoutParams(size, size);
		lp.setMargins(margin, margin, margin, margin);
		view.setLayoutParams(lp);
		return view;
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
	}

	@SuppressWarnings("unused")
	@OnClick(R.id.container_plays)
	public void onPlaysClick(View v) {
		startActivity(new Intent(getActivity(), PlaysActivity.class));
	}

	@SuppressWarnings("unused")
	@OnClick(R.id.container_players)
	public void onPlayersClick(View v) {
		startActivity(new Intent(getActivity(), PlayersActivity.class));
	}

	@SuppressWarnings("unused")
	@OnClick(R.id.container_locations)
	public void onLocationsClick(View v) {
		startActivity(new Intent(getActivity(), LocationsActivity.class));
	}

	@SuppressWarnings("unused")
	@OnClick(R.id.container_colors)
	public void onColorsClick(View v) {
		Intent intent = new Intent(getActivity(), BuddyColorsActivity.class);
		intent.putExtra(ActivityUtils.KEY_BUDDY_NAME, AccountUtils.getUsername(getActivity()));
		startActivity(intent);
	}

	@SuppressWarnings("unused")
	@OnClick(R.id.container_stats)
	public void onStatsClick(View v) {
		startActivity(new Intent(getActivity(), PlayStatsActivity.class));
	}
}
