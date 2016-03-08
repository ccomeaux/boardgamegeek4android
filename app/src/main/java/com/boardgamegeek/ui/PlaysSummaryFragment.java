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
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.auth.AccountUtils;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.provider.BggContract.PlayerColors;
import com.boardgamegeek.provider.BggContract.Plays;
import com.boardgamegeek.sorter.LocationsSorter;
import com.boardgamegeek.sorter.LocationsSorterFactory;
import com.boardgamegeek.sorter.PlayersSorter;
import com.boardgamegeek.sorter.PlayersSorterFactory;
import com.boardgamegeek.sorter.PlaysSorter;
import com.boardgamegeek.sorter.PlaysSorterFactory;
import com.boardgamegeek.ui.model.BuddyColor;
import com.boardgamegeek.ui.model.Location;
import com.boardgamegeek.ui.model.PlayModel;
import com.boardgamegeek.ui.model.Player;
import com.boardgamegeek.util.ActivityUtils;
import com.boardgamegeek.util.ColorUtils;
import com.boardgamegeek.util.PreferencesUtils;
import com.boardgamegeek.util.PresentationUtils;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;

public class PlaysSummaryFragment extends Fragment implements LoaderCallbacks<Cursor> {
	private static final int PLAYS_TOKEN = 1;
	private static final int PLAY_COUNT_TOKEN = 2;
	private static final int PLAYERS_TOKEN = 3;
	private static final int LOCATIONS_TOKEN = 4;
	private static final int COLORS_TOKEN = 5;

	@SuppressWarnings("unused") @InjectView(R.id.plays_container) LinearLayout playsContainer;
	@SuppressWarnings("unused") @InjectView(R.id.card_footer_plays) TextView playsFooter;
	@SuppressWarnings("unused") @InjectView(R.id.players_container) LinearLayout playersContainer;
	@SuppressWarnings("unused") @InjectView(R.id.card_footer_players) TextView playersFooter;
	@SuppressWarnings("unused") @InjectView(R.id.locations_container) LinearLayout locationsContainer;
	@SuppressWarnings("unused") @InjectView(R.id.card_footer_locations) TextView locationsFooter;
	@SuppressWarnings("unused") @InjectView(R.id.card_colors) View colorsCard;
	@SuppressWarnings("unused") @InjectView(R.id.colors_hint) View colorsHint;
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

		getLoaderManager().restartLoader(PLAYS_TOKEN, null, this);
		getLoaderManager().restartLoader(PLAY_COUNT_TOKEN, null, this);
		getLoaderManager().restartLoader(PLAYERS_TOKEN, null, this);
		getLoaderManager().restartLoader(LOCATIONS_TOKEN, null, this);
		getLoaderManager().restartLoader(COLORS_TOKEN, null, this);
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		CursorLoader loader = null;
		switch (id) {
			case PLAYS_TOKEN:
				PlaysSorter playsSorter = PlaysSorterFactory.create(getActivity(), PlayersSorterFactory.TYPE_DEFAULT);
				loader = new CursorLoader(getActivity(),
					Plays.CONTENT_URI.buildUpon().appendQueryParameter(BggContract.PARAM_LIMIT, "3").build(),
					PlayModel.PROJECTION,
					null, null, playsSorter.getOrderByClause());
				break;
			case PLAY_COUNT_TOKEN:
				loader = new CursorLoader(getActivity(),
					Plays.CONTENT_SIMPLE_URI,
					new String[] { "SUM(" + Plays.QUANTITY + ")" },
					null, null, null);
				break;
			case PLAYERS_TOKEN:
				// TODO limit to 4 players
				PlayersSorter playersSorter = PlayersSorterFactory.create(getActivity(), PlayersSorterFactory.TYPE_QUANTITY);
				loader = new CursorLoader(getActivity(),
					Plays.buildPlayersByUniquePlayerUri(),
					Player.PROJECTION,
					null, null, playersSorter.getOrderByClause());
				break;
			case LOCATIONS_TOKEN:
				// TODO limit to 4 locations
				LocationsSorter locationsSorter = LocationsSorterFactory.create(getActivity(), LocationsSorterFactory.TYPE_QUANTITY);
				loader = new CursorLoader(getActivity(),
					Plays.buildLocationsUri(),
					Location.PROJECTION,
					null, null, locationsSorter.getOrderByClause());
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
			case PLAYS_TOKEN:
				onPlaysQueryComplete(cursor);
				break;
			case PLAY_COUNT_TOKEN:
				onPlayCountQueryComplete(cursor);
				break;
			case PLAYERS_TOKEN:
				onPlayersQueryComplete(cursor);
				break;
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

	private void onPlaysQueryComplete(Cursor cursor) {
		if (cursor == null) {
			return;
		}

		playsContainer.removeAllViews();
		while (cursor.moveToNext()) {
			PlayModel play = PlayModel.fromCursor(cursor, getActivity());
			View view = createRow(playsContainer, play.getName(), PresentationUtils.describePlayDetails(getActivity(), play.getDate(), play.getLocation(), play.getQuantity(), play.getLength(), play.getPlayerCount()));

			view.setTag(R.id.play_id, play.getPlayId());
			view.setTag(R.id.game_info_id, play.getGameId());
			view.setTag(R.id.game_name, play.getName());
			view.setTag(R.id.thumbnail, play.getThumbnailUrl());
			view.setTag(R.id.account_image, play.getImageUrl());

			view.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					ActivityUtils.startPlayActivity(getActivity(),
						(int) v.getTag(R.id.play_id),
						(int) v.getTag(R.id.game_info_id),
						(String) v.getTag(R.id.game_name),
						(String) v.getTag(R.id.thumbnail),
						(String) v.getTag(R.id.account_image));
				}
			});
		}
	}

	private void onPlayCountQueryComplete(Cursor cursor) {
		if (cursor == null) {
			return;
		}

		if (cursor.moveToFirst()) {
			setQuantityTextView(playsFooter, R.plurals.plays_suffix, cursor.getInt(0));
		}
	}

	private void onPlayersQueryComplete(Cursor cursor) {
		if (cursor == null) {
			return;
		}

		setQuantityTextView(playersFooter, R.plurals.players_suffix, cursor.getCount());
		String accountUsername = AccountUtils.getUsername(getActivity());
		int count = 0;
		playersContainer.removeAllViews();
		while (cursor.moveToNext()) {
			Player player = Player.fromCursor(cursor);

			if (accountUsername.equals(player.getUsername())) {
				continue;
			}

			View view = createRowWithPlayCount(playersContainer, PresentationUtils.describePlayer(player.getName(), player.getUsername()), player.getPlayCount());

			view.setTag(R.id.name, player.getName());
			view.setTag(R.id.username, player.getUsername());

			view.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					ActivityUtils.startBuddyActivity(
						getActivity(),
						(String) v.getTag(R.id.username),
						(String) v.getTag(R.id.name));
				}
			});

			count++;
			if (count >= 3) {
				break;
			}
		}
	}

	private void onLocationsQueryComplete(Cursor cursor) {
		if (cursor == null) {
			return;
		}

		setQuantityTextView(locationsFooter, R.plurals.locations_suffix, cursor.getCount());
		int count = 0;
		locationsContainer.removeAllViews();
		while (cursor.moveToNext()) {
			Location location = Location.fromCursor(cursor);

			if (TextUtils.isEmpty(location.getName())) {
				continue;
			}

			View view = createRowWithPlayCount(locationsContainer, location.getName(), location.getPlayCount());

			view.setTag(R.id.name, location.getName());

			view.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					Intent intent = ActivityUtils.createLocationIntent(
						getActivity(),
						(String) v.getTag(R.id.name));
					startActivity(intent);
				}
			});

			count++;
			if (count >= 3) {
				break;
			}
		}
	}

	private View createRow(LinearLayout container, String title, String text) {
		View view = getLayoutInflater(null).inflate(R.layout.row_player_summary, container, false);
		container.addView(view);
		((TextView) view.findViewById(android.R.id.title)).setText(title);
		((TextView) view.findViewById(android.R.id.text1)).setText(text);
		return view;
	}

	private View createRowWithPlayCount(LinearLayout container, String title, int playCount) {
		return createRow(container, title, getResources().getQuantityString(R.plurals.plays_suffix, playCount, playCount));
	}

	private void setQuantityTextView(TextView textView, int resId, int count) {
		textView.setText(getResources().getQuantityString(resId, count, count));
	}

	private void onColorsQueryComplete(Cursor cursor) {
		if (cursor == null) {
			return;
		}

		colorContainer.removeAllViews();
		if (cursor.getCount() > 0) {
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
		colorsHint.setVisibility(cursor.getCount() == 0 ? View.VISIBLE : View.GONE);
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
	@OnClick(R.id.card_footer_plays)
	public void onPlaysClick(View v) {
		startActivity(new Intent(getActivity(), PlaysActivity.class));
	}

	@SuppressWarnings("unused")
	@OnClick(R.id.card_footer_players)
	public void onPlayersClick(View v) {
		startActivity(new Intent(getActivity(), PlayersActivity.class));
	}

	@SuppressWarnings("unused")
	@OnClick(R.id.card_footer_locations)
	public void onLocationsClick(View v) {
		startActivity(new Intent(getActivity(), LocationsActivity.class));
	}

	@SuppressWarnings("unused")
	@OnClick(R.id.card_footer_colors)
	public void onColorsClick(View v) {
		Intent intent = new Intent(getActivity(), BuddyColorsActivity.class);
		intent.putExtra(ActivityUtils.KEY_BUDDY_NAME, AccountUtils.getUsername(getActivity()));
		startActivity(intent);
	}

	@SuppressWarnings("unused")
	@OnClick(R.id.card_footer_stats)
	public void onStatsClick(View v) {
		startActivity(new Intent(getActivity(), PlayStatsActivity.class));
	}
}
