package com.boardgamegeek.ui;

import java.text.DecimalFormat;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.RatingBar;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;
import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.GameRanks;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.util.UIUtils;

public class GameInfoFragment extends SherlockFragment implements LoaderManager.LoaderCallbacks<Cursor> {
	private Uri mGameUri;

	private TextView mRatingView;
	private TextView mNumberRatingView;
	private TextView mRankView;
	private RatingBar mRatingBar;
	private TextView mYearPublishedView;
	private TextView mPlayersView;
	private TextView mPlayingTimeView;
	private TextView mSuggestedAgesView;
	private TextView mIdView;
	private TextView mUpdatedView;
	private WebView mDescriptionView;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final Intent intent = UIUtils.fragmentArgumentsToIntent(getArguments());
		mGameUri = intent.getData();

		if (mGameUri == null) {
			return;
		}

		getLoaderManager().restartLoader(GameQuery._TOKEN, null, this);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.fragment_game_info, null);

		mRatingView = (TextView) rootView.findViewById(R.id.rating);
		mNumberRatingView = (TextView) rootView.findViewById(R.id.number_rating);
		mRankView = (TextView) rootView.findViewById(R.id.rank);
		mRatingBar = (RatingBar) rootView.findViewById(R.id.rating_stars);
		mYearPublishedView = (TextView) rootView.findViewById(R.id.year_published);
		mPlayersView = (TextView) rootView.findViewById(R.id.num_of_players);
		mPlayingTimeView = (TextView) rootView.findViewById(R.id.playing_time);
		mSuggestedAgesView = (TextView) rootView.findViewById(R.id.suggested_ages);
		mIdView = (TextView) rootView.findViewById(R.id.game_id);
		mUpdatedView = (TextView) rootView.findViewById(R.id.updated);
		mDescriptionView = (WebView) rootView.findViewById(R.id.description);
		WebSettings webSettings = mDescriptionView.getSettings();
		webSettings.setDefaultFontSize((int) (getResources().getDimension(R.dimen.text_size_small) / getResources()
			.getDisplayMetrics().density));

		return rootView;
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle data) {
		CursorLoader loader = null;
		if (id == GameQuery._TOKEN) {
			loader = new CursorLoader(getActivity(), mGameUri, GameQuery.PROJECTION, null, null, null);
		}
		return loader;
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		if (getActivity() == null) {
			return;
		}

		if (loader.getId() == GameQuery._TOKEN) {
			onGameQueryComplete(cursor);
		} else {
			cursor.close();
		}
	}

	@Override
	public void onLoaderReset(Loader<Cursor> arg0) {
	}

	private void onGameQueryComplete(Cursor cursor) {
		if (cursor == null || !cursor.moveToFirst()) {
			return;
		}

		Game game = new Game(cursor);

		mRankView.setText(game.getRankDescription());
		mRatingView.setText(game.getRatingDescription());
		mRatingBar.setRating(game.Rating);
		mNumberRatingView.setText(game.getNumberOfUsersRating());
		mYearPublishedView.setText(game.getYearPublished());
		mPlayingTimeView.setText(game.getPlayingTimeDescription());
		mPlayersView.setText(game.getPlayerRangeDescription());
		mSuggestedAgesView.setText(game.getAgeDescription());
		mDescriptionView.loadDataWithBaseURL(null, game.Description, "text/html", "UTF-8", null);
		mIdView.setText(game.getIdDescription());

		if (game.Updated == 0) {
			mUpdatedView.setVisibility(View.GONE);
		} else {
			mUpdatedView.setVisibility(View.VISIBLE);
			CharSequence u = DateUtils.getRelativeTimeSpanString(game.Updated, System.currentTimeMillis(),
				DateUtils.MINUTE_IN_MILLIS);
			mUpdatedView.setText(getResources().getString(R.string.updated) + " " + u);
		}
	}

	private interface GameQuery {
		int _TOKEN = 0x1;

		String[] PROJECTION = { Games.GAME_ID, Games.STATS_AVERAGE, Games.YEAR_PUBLISHED, Games.MIN_PLAYERS,
			Games.MAX_PLAYERS, Games.PLAYING_TIME, Games.MINIMUM_AGE, Games.DESCRIPTION, Games.STATS_USERS_RATED,
			Games.UPDATED, GameRanks.GAME_RANK_VALUE };

		int GAME_ID = 0;
		int STATS_AVERAGE = 1;
		int YEAR_PUBLISHED = 2;
		int MIN_PLAYERS = 3;
		int MAX_PLAYERS = 4;
		int PLAYING_TIME = 5;
		int MINIMUM_AGE = 6;
		int DESCRIPTION = 7;
		int STATS_USERS_RATED = 8;
		int UPDATED = 9;
		int GAME_RANK_VALUE = 10;
	}

	private class Game {
		int Id;
		float Rating;
		int YearPublished;
		int MinPlayers;
		int MaxPlayers;
		int PlayingTime;
		int MinimumAge;
		String Description;
		int UsersRated;
		long Updated;
		int Rank;

		public Game(Cursor cursor) {
			Id = cursor.getInt(GameQuery.GAME_ID);
			Rating = (float) cursor.getDouble(GameQuery.STATS_AVERAGE);
			YearPublished = cursor.getInt(GameQuery.YEAR_PUBLISHED);
			MinPlayers = cursor.getInt(GameQuery.MIN_PLAYERS);
			MaxPlayers = cursor.getInt(GameQuery.MAX_PLAYERS);
			PlayingTime = cursor.getInt(GameQuery.PLAYING_TIME);
			MinimumAge = cursor.getInt(GameQuery.MINIMUM_AGE);
			Description = cursor.getString(GameQuery.DESCRIPTION);
			UsersRated = cursor.getInt(GameQuery.STATS_USERS_RATED);
			Updated = cursor.getLong(GameQuery.UPDATED);
			Rank = cursor.getInt(GameQuery.GAME_RANK_VALUE);
		}

		public String getAgeDescription() {
			if (MinimumAge > 0) {
				return MinimumAge + " " + getResources().getString(R.string.age_suffix);
			}
			return getResources().getString(R.string.text_unknown);
		}

		public String getIdDescription() {
			return "ID: " + Id;
		}

		public String getNumberOfUsersRating() {
			return UsersRated + " Ratings";
		}

		private String getPlayerRangeDescription() {
			if (MinPlayers == 0 && MaxPlayers == 0) {
				return getResources().getString(R.string.text_unknown);
			} else if (MinPlayers >= MaxPlayers) {
				return String.valueOf(MinPlayers);
			} else {
				return String.valueOf(MinPlayers) + " - " + String.valueOf(MaxPlayers);
			}
		}

		private String getPlayingTimeDescription() {
			if (PlayingTime > 0) {
				return PlayingTime + " " + getResources().getString(R.string.time_suffix);
			}
			return getResources().getString(R.string.text_unknown);
		}

		private String getRankDescription() {
			if (Rank == 0) {
				return getString(R.string.text_not_available);
			} else {
				return String.valueOf(Rank);
			}
		}

		public String getRatingDescription() {
			return new DecimalFormat("#0.00").format(Rating) + " / 10";
		}

		public String getYearPublished() {
			if (YearPublished == 0) {
				return getResources().getString(R.string.text_unknown);
			}
			return String.valueOf(YearPublished);
		}
	}
}
