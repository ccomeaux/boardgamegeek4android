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
import com.boardgamegeek.events.PlaySelectedEvent;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.provider.BggContract.PlayerColors;
import com.boardgamegeek.provider.BggContract.Plays;
import com.boardgamegeek.sorter.LocationsSorter;
import com.boardgamegeek.sorter.LocationsSorterFactory;
import com.boardgamegeek.sorter.PlayersSorter;
import com.boardgamegeek.sorter.PlayersSorterFactory;
import com.boardgamegeek.sorter.PlaysSorter;
import com.boardgamegeek.sorter.PlaysSorterFactory;
import com.boardgamegeek.ui.model.Location;
import com.boardgamegeek.ui.model.PlayModel;
import com.boardgamegeek.ui.model.Player;
import com.boardgamegeek.ui.model.PlayerColor;
import com.boardgamegeek.util.ActivityUtils;
import com.boardgamegeek.util.ColorUtils;
import com.boardgamegeek.util.PreferencesUtils;
import com.boardgamegeek.util.PresentationUtils;
import com.boardgamegeek.util.SelectionBuilder;

import org.greenrobot.eventbus.EventBus;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;

public class PlaysSummaryFragment extends Fragment implements LoaderCallbacks<Cursor> {
	private static final int PLAYS_TOKEN = 1;
	private static final int PLAY_COUNT_TOKEN = 2;
	private static final int PLAYERS_TOKEN = 3;
	private static final int LOCATIONS_TOKEN = 4;
	private static final int COLORS_TOKEN = 5;
	private static final int PLAYS_IN_PROGRESS_TOKEN = 6;

	private Unbinder unbinder;
	@BindView(R.id.card_plays) View playsCard;
	@BindView(R.id.plays_subtitle_in_progress) TextView playsInProgressSubtitle;
	@BindView(R.id.plays_in_progress_container) LinearLayout playsInProgressContainer;
	@BindView(R.id.plays_subtitle_recent) TextView recentPlaysSubtitle;
	@BindView(R.id.plays_container) LinearLayout recentPlaysContainer;
	@BindView(R.id.card_footer_plays) TextView playsFooter;
	@BindView(R.id.card_players) View playersCard;
	@BindView(R.id.players_container) LinearLayout playersContainer;
	@BindView(R.id.card_locations) View locationsCard;
	@BindView(R.id.locations_container) LinearLayout locationsContainer;
	@BindView(R.id.card_colors) View colorsCard;
	@BindView(R.id.colors_hint) View colorsHint;
	@BindView(R.id.color_container) LinearLayout colorContainer;
	@BindView(R.id.h_index) TextView hIndexView;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_plays_summary, container, false);

		unbinder = ButterKnife.bind(this, rootView);
		hIndexView.setText(PresentationUtils.getText(getActivity(), R.string.h_index_prefix, PreferencesUtils.getHIndex(getActivity())));

		return rootView;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		getLoaderManager().restartLoader(PLAYS_TOKEN, null, this);
		getLoaderManager().restartLoader(PLAY_COUNT_TOKEN, null, this);
		getLoaderManager().restartLoader(PLAYERS_TOKEN, null, this);
		getLoaderManager().restartLoader(LOCATIONS_TOKEN, null, this);
		if (!TextUtils.isEmpty(AccountUtils.getUsername(getActivity()))) {
			getLoaderManager().restartLoader(COLORS_TOKEN, null, this);
		}
		getLoaderManager().restartLoader(PLAYS_IN_PROGRESS_TOKEN, null, this);
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		unbinder.unbind();
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		CursorLoader loader = null;
		switch (id) {
			case PLAYS_IN_PROGRESS_TOKEN:
				PlaysSorter playsSorter = PlaysSorterFactory.create(getActivity(), PlayersSorterFactory.TYPE_DEFAULT);
				loader = new CursorLoader(getActivity(),
					Plays.CONTENT_URI,
					PlayModel.PROJECTION,
					Plays.DIRTY_TIMESTAMP + ">0",
					null,
					playsSorter == null ? null : playsSorter.getOrderByClause());
				break;
			case PLAYS_TOKEN:
				playsSorter = PlaysSorterFactory.create(getActivity(), PlayersSorterFactory.TYPE_DEFAULT);
				loader = new CursorLoader(getActivity(),
					Plays.CONTENT_URI.buildUpon().appendQueryParameter(BggContract.QUERY_KEY_LIMIT, "3").build(),
					PlayModel.PROJECTION,
					SelectionBuilder.whereZeroOrNull(Plays.DIRTY_TIMESTAMP) + " AND " + SelectionBuilder.whereZeroOrNull(Plays.DELETE_TIMESTAMP),
					null,
					playsSorter == null ? null : playsSorter.getOrderByClause());
				break;
			case PLAY_COUNT_TOKEN:
				loader = new CursorLoader(getActivity(),
					Plays.CONTENT_SIMPLE_URI,
					new String[] { "SUM(" + Plays.QUANTITY + ")" },
					null, null, null);
				break;
			case PLAYERS_TOKEN:
				PlayersSorter playersSorter = PlayersSorterFactory.create(getActivity(), PlayersSorterFactory.TYPE_QUANTITY);
				loader = new CursorLoader(getActivity(),
					Plays.buildPlayersByUniquePlayerUri().buildUpon().appendQueryParameter(BggContract.QUERY_KEY_LIMIT, "4").build(),
					Player.PROJECTION,
					null, null, playersSorter.getOrderByClause());
				break;
			case LOCATIONS_TOKEN:
				LocationsSorter locationsSorter = LocationsSorterFactory.create(getActivity(), LocationsSorterFactory.TYPE_QUANTITY);
				loader = new CursorLoader(getActivity(),
					Plays.buildLocationsUri().buildUpon().appendQueryParameter(BggContract.QUERY_KEY_LIMIT, "4").build(),
					Location.PROJECTION,
					null, null, locationsSorter.getOrderByClause());
				break;
			case COLORS_TOKEN:
				loader = new CursorLoader(getActivity(),
					PlayerColors.buildUserUri(AccountUtils.getUsername(getActivity())),
					PlayerColor.PROJECTION,
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
			case PLAYS_IN_PROGRESS_TOKEN:
				onPlaysInProgressQueryComplete(cursor);
				break;
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

	private void onPlaysInProgressQueryComplete(Cursor cursor) {
		if (cursor == null) {
			return;
		}

		final int visibility = cursor.getCount() == 0 ? View.GONE : View.VISIBLE;
		playsInProgressSubtitle.setVisibility(visibility);
		playsInProgressContainer.setVisibility(visibility);
		recentPlaysSubtitle.setVisibility(visibility);

		playsInProgressContainer.removeAllViews();
		while (cursor.moveToNext()) {
			playsCard.setVisibility(View.VISIBLE);
			addPlayToContainer(cursor, playsInProgressContainer);
		}
	}

	private void onPlaysQueryComplete(Cursor cursor) {
		if (cursor == null) {
			return;
		}

		recentPlaysContainer.removeAllViews();
		while (cursor.moveToNext()) {
			playsCard.setVisibility(View.VISIBLE);
			recentPlaysContainer.setVisibility(View.VISIBLE);
			addPlayToContainer(cursor, recentPlaysContainer);
		}
	}

	private void addPlayToContainer(Cursor cursor, LinearLayout container) {
		long internalId = cursor.getLong(cursor.getColumnIndex(Plays._ID));
		PlayModel play = PlayModel.fromCursor(cursor, getActivity());
		View view = createRow(container, play.getName(), PresentationUtils.describePlayDetails(getActivity(), play.getDate(), play.getLocation(), play.getQuantity(), play.getLength(), play.getPlayerCount()));

		view.setTag(R.id.id, internalId);
		view.setTag(R.id.game_info_id, play.getGameId());
		view.setTag(R.id.game_name, play.getName());
		view.setTag(R.id.thumbnail, play.getThumbnailUrl());
		view.setTag(R.id.account_image, play.getImageUrl());

		view.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				EventBus.getDefault().postSticky(new PlaySelectedEvent(
					(long) v.getTag(R.id.id),
					(int) v.getTag(R.id.game_info_id),
					(String) v.getTag(R.id.game_name),
					(String) v.getTag(R.id.thumbnail),
					(String) v.getTag(R.id.account_image)));
			}
		});
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

		String accountUsername = AccountUtils.getUsername(getActivity());
		int count = 0;
		playersContainer.removeAllViews();
		while (cursor.moveToNext()) {
			Player player = Player.fromCursor(cursor);

			if (accountUsername != null && accountUsername.equals(player.getUsername())) {
				continue;
			}

			playersCard.setVisibility(View.VISIBLE);
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

		int count = 0;
		locationsContainer.removeAllViews();
		while (cursor.moveToNext()) {
			Location location = Location.fromCursor(cursor);

			if (TextUtils.isEmpty(location.getName())) {
				continue;
			}

			locationsCard.setVisibility(View.VISIBLE);
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
		if (cursor == null) return;

		colorContainer.removeAllViews();
		if (cursor.getCount() > 0) {
			while (cursor.moveToNext()) {
				ImageView view = createViewToBeColored();
				PlayerColor color = PlayerColor.fromCursor(cursor);
				ColorUtils.setColorViewValue(view, ColorUtils.parseColor(color.getColor()));
				colorContainer.addView(view);
			}
		}
		colorsHint.setVisibility(cursor.getCount() == 0 ? View.VISIBLE : View.GONE);
		colorsCard.setVisibility(View.VISIBLE);
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
	public void onPlaysClick() {
		startActivity(new Intent(getActivity(), PlaysActivity.class));
	}

	@SuppressWarnings("unused")
	@OnClick(R.id.card_footer_players)
	public void onPlayersClick() {
		startActivity(new Intent(getActivity(), PlayersActivity.class));
	}

	@SuppressWarnings("unused")
	@OnClick(R.id.card_footer_locations)
	public void onLocationsClick() {
		startActivity(new Intent(getActivity(), LocationsActivity.class));
	}

	@SuppressWarnings("unused")
	@OnClick(R.id.card_footer_colors)
	public void onColorsClick() {
		Intent intent = new Intent(getActivity(), PlayerColorsActivity.class);
		intent.putExtra(ActivityUtils.KEY_BUDDY_NAME, AccountUtils.getUsername(getActivity()));
		startActivity(intent);
	}

	@SuppressWarnings("unused")
	@OnClick(R.id.card_footer_stats)
	public void onStatsClick() {
		startActivity(new Intent(getActivity(), PlayStatsActivity.class));
	}
}
