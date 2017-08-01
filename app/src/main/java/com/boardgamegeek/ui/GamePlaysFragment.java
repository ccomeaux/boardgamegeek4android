package com.boardgamegeek.ui;


import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.ColorInt;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.graphics.Palette;
import android.support.v7.graphics.Palette.Swatch;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.events.GameInfoChangedEvent;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.ui.adapter.GameColorAdapter;
import com.boardgamegeek.ui.model.Game;
import com.boardgamegeek.ui.model.GamePlays;
import com.boardgamegeek.util.ActivityUtils;
import com.boardgamegeek.util.PaletteUtils;
import com.boardgamegeek.util.PresentationUtils;
import com.boardgamegeek.util.UIUtils;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.List;

import butterknife.BindView;
import butterknife.BindViews;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import hugo.weaving.DebugLog;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

public class GamePlaysFragment extends Fragment implements LoaderCallbacks<Cursor> {
	private static final int GAME_TOKEN = 0;
	private static final int PLAYS_TOKEN = 1;
	private static final int COLOR_TOKEN = 2;
	private Uri gameUri;
	private String gameName;
	private String imageUrl;
	private String thumbnailUrl;
	private boolean arePlayersCustomSorted;
	@ColorInt private int iconColor;

	Unbinder unbinder;
	@BindView(R.id.plays_root) View playsRoot;
	@BindView(R.id.plays_label) TextView playsLabel;
	@BindView(R.id.plays_last_play) TextView lastPlayView;
	@BindView(R.id.play_stats_root) View playStatsRoot;
	@BindView(R.id.colors_root) View colorsRoot;
	@BindView(R.id.game_colors_label) TextView colorsLabel;
	@BindViews({
		R.id.icon_plays,
		R.id.icon_play_stats,
		R.id.icon_colors
	}) List<ImageView> colorizedIcons;
	private Palette palette;

	@Override
	@DebugLog
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final Intent intent = UIUtils.fragmentArgumentsToIntent(getArguments());
		gameUri = intent.getData();
		gameName = intent.getStringExtra(ActivityUtils.KEY_GAME_NAME);
	}

	@DebugLog
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_game_plays, container, false);
		unbinder = ButterKnife.bind(this, rootView);

		colorize();

		getLoaderManager().restartLoader(GAME_TOKEN, null, this);
		getLoaderManager().restartLoader(PLAYS_TOKEN, null, this);
		getLoaderManager().restartLoader(COLOR_TOKEN, null, this);

		return rootView;
	}

	@DebugLog
	@Override
	public void onDestroyView() {
		super.onDestroyView();
		if (unbinder != null) unbinder.unbind();
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		int gameId = Games.getGameId(gameUri);
		switch (id) {
			case GAME_TOKEN:
				return new CursorLoader(getContext(), gameUri, Game.PROJECTION, null, null, null);
			case PLAYS_TOKEN:
				return new CursorLoader(getContext(),
					GamePlays.URI,
					GamePlays.PROJECTION,
					GamePlays.getSelection(getContext()),
					GamePlays.getSelectionArgs(gameId),
					null);
			case COLOR_TOKEN:
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
				if (cursor == null && cursor.moveToFirst()) {
					Game game = Game.fromCursor(cursor);
					gameName = game.Name;
					imageUrl = game.ImageUrl;
					thumbnailUrl = game.ThumbnailUrl;
					arePlayersCustomSorted = game.CustomPlayerSort;
				}
				break;
			case PLAYS_TOKEN:
				onPlaysQueryComplete(cursor);
				break;
			case COLOR_TOKEN:
				colorsRoot.setVisibility(VISIBLE);
				int count = cursor == null ? 0 : cursor.getCount();
				colorsLabel.setText(PresentationUtils.getQuantityText(getActivity(), R.plurals.colors_suffix, count, count));
				break;
		}
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
	}

	@DebugLog
	private void onPlaysQueryComplete(Cursor cursor) {
		if (cursor.moveToFirst()) {
			playsRoot.setVisibility(VISIBLE);

			GamePlays plays = GamePlays.fromCursor(cursor);

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
		}
	}

	@SuppressWarnings("unused")
	@Subscribe
	public void onEvent(GameActivity.PaletteEvent event) {
		if (event.getGameId() == Games.getGameId(gameUri)) {
			palette = event.getPalette();
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
		if (palette == null) return;
		Palette.Swatch swatch = PaletteUtils.getIconSwatch(palette);
		iconColor = swatch.getRgb();
		ButterKnife.apply(colorizedIcons, PaletteUtils.colorIconSetter, swatch);
	}

	@OnClick(R.id.plays_root)
	@DebugLog
	public void onPlaysClick() {
		Intent intent = ActivityUtils.createGamePlaysIntent(getActivity(),
			gameUri,
			gameName,
			imageUrl,
			thumbnailUrl,
			arePlayersCustomSorted,
			iconColor);
		startActivity(intent);
	}

	@OnClick(R.id.play_stats_root)
	@DebugLog
	public void onPlayStatsClick() {
		Intent intent = new Intent(getActivity(), GamePlayStatsActivity.class);
		intent.setData(gameUri);
		intent.putExtra(ActivityUtils.KEY_GAME_NAME, gameName);
		if (palette != null) {
			final Swatch swatch = PaletteUtils.getHeaderSwatch(palette);
			intent.putExtra(ActivityUtils.KEY_HEADER_COLOR, swatch.getRgb());
		}
		startActivity(intent);
	}

	@OnClick(R.id.colors_root)
	@DebugLog
	public void onColorsClick() {
		Intent intent = new Intent(getActivity(), GameColorsActivity.class);
		intent.setData(gameUri);
		intent.putExtra(ActivityUtils.KEY_GAME_NAME, gameName);
		intent.putExtra(ActivityUtils.KEY_ICON_COLOR, iconColor);
		startActivity(intent);
	}
}
