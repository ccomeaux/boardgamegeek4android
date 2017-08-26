package com.boardgamegeek.ui;


import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.ColorInt;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v4.widget.SwipeRefreshLayout.OnRefreshListener;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.events.GameInfoChangedEvent;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.tasks.sync.SyncPlaysByGameTask;
import com.boardgamegeek.ui.GameActivity.ColorEvent;
import com.boardgamegeek.ui.adapter.GameColorAdapter;
import com.boardgamegeek.ui.model.GamePlays;
import com.boardgamegeek.ui.model.PlaysByGame;
import com.boardgamegeek.ui.widget.TimestampView;
import com.boardgamegeek.util.DateTimeUtils;
import com.boardgamegeek.util.PaletteUtils;
import com.boardgamegeek.util.PresentationUtils;
import com.boardgamegeek.util.TaskUtils;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.List;

import butterknife.BindView;
import butterknife.BindViews;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import hugo.weaving.DebugLog;
import icepick.Icepick;
import icepick.State;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

public class GamePlaysFragment extends Fragment implements LoaderCallbacks<Cursor>, OnRefreshListener {
	private static final String KEY_GAME_ID = "GAME_ID";
	private static final String KEY_GAME_NAME = "GAME_NAME";
	private static final String KEY_ICON_COLOR = "ICON_COLOR";
	private static final int GAME_TOKEN = 0;
	private static final int PLAYS_TOKEN = 1;
	private static final int COLORS_TOKEN = 2;
	private static final int AGE_IN_DAYS_TO_REFRESH = 1;

	private int gameId;
	private String gameName;
	private String imageUrl;
	private String thumbnailUrl;
	private boolean arePlayersCustomSorted;
	@ColorInt private int iconColor = Color.TRANSPARENT;
	private boolean isRefreshing;
	@State boolean mightNeedRefreshing = true;

	Unbinder unbinder;
	@BindView(R.id.swipe_refresh) SwipeRefreshLayout swipeRefreshLayout;
	@BindView(R.id.plays_root) View playsRoot;
	@BindView(R.id.plays_label) TextView playsLabel;
	@BindView(R.id.plays_last_play) TextView lastPlayView;
	@BindView(R.id.play_stats_root) View playStatsRoot;
	@BindView(R.id.colors_root) View colorsRoot;
	@BindView(R.id.game_colors_label) TextView colorsLabel;
	@BindView(R.id.sync_timestamp) TimestampView syncTimestampView;
	@BindViews({
		R.id.icon_plays,
		R.id.icon_play_stats,
		R.id.icon_colors
	}) List<ImageView> colorizedIcons;

	public static GamePlaysFragment newInstance(int gameId, String gameName, @ColorInt int iconColor) {
		Bundle args = new Bundle();
		args.putInt(KEY_GAME_ID, gameId);
		args.putString(KEY_GAME_NAME, gameName);
		args.putInt(KEY_ICON_COLOR, iconColor);
		GamePlaysFragment fragment = new GamePlaysFragment();
		fragment.setArguments(args);
		return fragment;
	}

	@Override
	@DebugLog
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		readBundle(getArguments());
		Icepick.restoreInstanceState(this, savedInstanceState);
	}

	@DebugLog
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_game_plays, container, false);
		unbinder = ButterKnife.bind(this, rootView);

		colorize();

		swipeRefreshLayout.setOnRefreshListener(this);
		swipeRefreshLayout.setColorSchemeResources(PresentationUtils.getColorSchemeResources());

		getLoaderManager().restartLoader(GAME_TOKEN, null, this);
		getLoaderManager().restartLoader(PLAYS_TOKEN, null, this);
		getLoaderManager().restartLoader(COLORS_TOKEN, null, this);

		return rootView;
	}

	private void readBundle(Bundle bundle) {
		gameId = bundle.getInt(KEY_GAME_ID, BggContract.INVALID_ID);
		gameName = bundle.getString(KEY_GAME_NAME);
		iconColor = bundle.getInt(KEY_ICON_COLOR, Color.TRANSPARENT);
	}

	@Override
	public void onStart() {
		super.onStart();
		EventBus.getDefault().register(this);
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		Icepick.saveInstanceState(this, outState);
	}

	@Override
	public void onStop() {
		super.onStop();
		EventBus.getDefault().unregister(this);
	}

	@DebugLog
	@Override
	public void onDestroyView() {
		super.onDestroyView();
		if (unbinder != null) unbinder.unbind();
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		switch (id) {
			case GAME_TOKEN:
				return new CursorLoader(getContext(), Games.buildGameUri(gameId), GamePlays.PROJECTION, null, null, null);
			case PLAYS_TOKEN:
				return new CursorLoader(getContext(),
					PlaysByGame.URI,
					PlaysByGame.PROJECTION,
					PlaysByGame.getSelection(getContext()),
					PlaysByGame.getSelectionArgs(gameId),
					null);
			case COLORS_TOKEN:
				return new CursorLoader(getContext(),
					GameColorAdapter.createUri(gameId),
					GameColorAdapter.PROJECTION,
					null, null, null);
			default:
				return null;
		}
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		if (getActivity() == null) return;
		switch (loader.getId()) {
			case GAME_TOKEN:
				onGameQueryComplete(cursor);
				break;
			case PLAYS_TOKEN:
				onPlaysQueryComplete(cursor);
				break;
			case COLORS_TOKEN:
				onColorsQueryComplete(cursor);
				break;
		}
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
	}

	@Override
	public void onRefresh() {
		requestRefresh();
	}

	private void requestRefresh() {
		if (!isRefreshing) {
			updateRefreshStatus(true);
			TaskUtils.executeAsyncTask(new SyncPlaysByGameTask(getContext(), gameId));
		} else {
			updateRefreshStatus(false);
		}
	}

	@SuppressWarnings("unused")
	@Subscribe(threadMode = ThreadMode.MAIN)
	public void onEvent(SyncPlaysByGameTask.CompletedEvent event) {
		if (event.getGameId() == gameId) {
			updateRefreshStatus(false);
		}
	}

	protected void updateRefreshStatus(boolean refreshing) {
		this.isRefreshing = refreshing;
		if (swipeRefreshLayout != null) {
			swipeRefreshLayout.post(new Runnable() {
				@Override
				public void run() {
					if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(isRefreshing);
				}
			});
		}
	}

	private void onGameQueryComplete(Cursor cursor) {
		if (cursor != null && cursor.moveToFirst()) {
			GamePlays game = GamePlays.fromCursor(cursor);
			gameName = game.getName();
			imageUrl = game.getImageUrl();
			thumbnailUrl = game.getThumbnailUrl();
			arePlayersCustomSorted = game.arePlayersCustomSorted();
			syncTimestampView.setTimestamp(game.getSyncTimestamp());

			if (mightNeedRefreshing) {
				mightNeedRefreshing = false;
				if (DateTimeUtils.howManyDaysOld(game.getSyncTimestamp()) > AGE_IN_DAYS_TO_REFRESH)
					requestRefresh();
			}
		} else {
			syncTimestampView.setTimestamp(System.currentTimeMillis());
		}
	}

	@DebugLog
	private void onPlaysQueryComplete(Cursor cursor) {
		if (cursor != null && cursor.moveToFirst()) {
			playsRoot.setVisibility(VISIBLE);

			PlaysByGame plays = PlaysByGame.fromCursor(cursor);

			String description = PresentationUtils.describePlayCount(getActivity(), plays.getCount());
			if (!TextUtils.isEmpty(description)) {
				description = " (" + description + ")";
			}
			playsLabel.setText(PresentationUtils.getQuantityText(getActivity(), R.plurals.plays_prefix, plays.getCount(), plays.getCount(), description));

			if (plays.getMaxDateInMillis() > 0) {
				lastPlayView.setText(PresentationUtils.getText(getActivity(), R.string.last_played_prefix, PresentationUtils.describePastDaySpan(plays.getMaxDateInMillis())));
				lastPlayView.setVisibility(VISIBLE);
			} else {
				lastPlayView.setVisibility(GONE);
			}

			playStatsRoot.setVisibility(plays.getCount() == 0 ? GONE : VISIBLE);
		}
	}

	private void onColorsQueryComplete(Cursor cursor) {
		colorsRoot.setVisibility(VISIBLE);
		int count = cursor == null ? 0 : cursor.getCount();
		colorsLabel.setText(PresentationUtils.getQuantityText(getContext(), R.plurals.colors_suffix, count, count));
	}

	@SuppressWarnings("unused")
	@Subscribe
	public void onEvent(ColorEvent event) {
		if (event.getGameId() == gameId) {
			iconColor = event.getIconColor();
			colorize();
		}
	}

	@SuppressWarnings("unused")
	@DebugLog
	@Subscribe(threadMode = ThreadMode.MAIN)
	public void onEvent(GameInfoChangedEvent event) {
		imageUrl = event.getImageUrl();
		thumbnailUrl = event.getThumbnailUrl();
		arePlayersCustomSorted = event.arePlayersCustomSorted();
	}

	private void colorize() {
		if (!isAdded()) return;
		if (iconColor != Color.TRANSPARENT) {
			ButterKnife.apply(colorizedIcons, PaletteUtils.rgbIconSetter, iconColor);
		}
	}

	@OnClick(R.id.plays_root)
	@DebugLog
	public void onPlaysClick() {
		GamePlaysActivity.start(getContext(), gameId, gameName, imageUrl, thumbnailUrl, arePlayersCustomSorted, iconColor);
	}

	@OnClick(R.id.play_stats_root)
	@DebugLog
	public void onPlayStatsClick() {
		GamePlayStatsActivity.start(getContext(), gameId, gameName, iconColor);
	}

	@OnClick(R.id.colors_root)
	@DebugLog
	public void onColorsClick() {
		GameColorsActivity.start(getContext(), gameId, gameName, iconColor);
	}
}
