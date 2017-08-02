package com.boardgamegeek.ui.dialog;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.ColorInt;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.ui.widget.StatBar;
import com.boardgamegeek.util.ActivityUtils;
import com.boardgamegeek.util.UIUtils;

import java.util.List;

import butterknife.BindView;
import butterknife.BindViews;
import butterknife.ButterKnife;
import butterknife.Unbinder;

public class GameUsersDialogFragment extends DialogFragment implements LoaderCallbacks<Cursor> {
	private Uri uri;
	@ColorInt private int barColor;
	private Unbinder unbinder;
	@BindView(R.id.users_owning_bar) StatBar numberOwningBar;
	@BindView(R.id.users_trading_bar) StatBar numberTradingBar;
	@BindView(R.id.users_wanting_bar) StatBar numberWantingBar;
	@BindView(R.id.users_wishing_bar) StatBar numberWishingBar;
	@BindViews({
		R.id.users_owning_bar,
		R.id.users_trading_bar,
		R.id.users_wanting_bar,
		R.id.users_wishing_bar
	}) List<StatBar> statBars;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final Intent intent = UIUtils.fragmentArgumentsToIntent(getArguments());
		int gameId = intent.getIntExtra(ActivityUtils.KEY_GAME_ID, BggContract.INVALID_ID);
		if (gameId == BggContract.INVALID_ID) dismiss();
		barColor = intent.getIntExtra(ActivityUtils.KEY_DARK_COLOR, Color.TRANSPARENT);
		uri = Games.buildGameUri(gameId);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		getDialog().setTitle(R.string.title_users);

		ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.dialog_game_users, container, false);
		unbinder = ButterKnife.bind(this, rootView);
		if (barColor != Color.TRANSPARENT) ButterKnife.apply(statBars, StatBar.colorSetter, barColor);
		return rootView;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		getLoaderManager().restartLoader(Query._TOKEN, null, this);
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		if (unbinder != null) unbinder.unbind();
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle data) {
		CursorLoader loader = null;
		if (id == Query._TOKEN) {
			loader = new CursorLoader(getActivity(), uri, Query.PROJECTION, null, null, null);
		}
		return loader;
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		if (getActivity() == null) return;
		if (loader == null) return;
		if (cursor == null) return;

		if (loader.getId() == Query._TOKEN) {
			if (cursor.moveToFirst()) {
				int numberWeights = cursor.getInt(Query.STATS_NUMBER_WEIGHTS);
				int numberOwned = cursor.getInt(Query.STATS_NUMBER_OWNED);
				int numberTrading = cursor.getInt(Query.STATS_NUMBER_TRADING);
				int numberWanting = cursor.getInt(Query.STATS_NUMBER_WANTING);
				int numberWishing = cursor.getInt(Query.STATS_NUMBER_WISHING);
				int numberRated = cursor.getInt(Query.STATS_USERS_RATED);
				int numberComments = cursor.getInt(Query.STATS_NUMBER_COMMENTS);

				int maxUsers = Math.max(numberWeights, numberOwned);
				maxUsers = Math.max(maxUsers, numberTrading);
				maxUsers = Math.max(maxUsers, numberWanting);
				maxUsers = Math.max(maxUsers, numberWishing);
				maxUsers = Math.max(maxUsers, numberRated);
				maxUsers = Math.max(maxUsers, numberComments);

				numberOwningBar.setBar(R.string.owning_meter_text, numberOwned, maxUsers);
				numberTradingBar.setBar(R.string.trading_meter_text, numberTrading, maxUsers);
				numberWantingBar.setBar(R.string.wanting_meter_text, numberWanting, maxUsers);
				numberWishingBar.setBar(R.string.wishing_meter_text, numberWishing, maxUsers);

			}
		} else {
			cursor.close();
		}

	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
	}

	private interface Query {
		int _TOKEN = 0x0;
		String[] PROJECTION = {
			Games.STATS_NUMBER_WEIGHTS,
			Games.STATS_NUMBER_OWNED,
			Games.STATS_NUMBER_TRADING,
			Games.STATS_NUMBER_WANTING,
			Games.STATS_NUMBER_WISHING,
			Games.STATS_USERS_RATED,
			Games.STATS_NUMBER_COMMENTS,
		};
		int STATS_NUMBER_WEIGHTS = 0;
		int STATS_NUMBER_OWNED = 1;
		int STATS_NUMBER_TRADING = 2;
		int STATS_NUMBER_WANTING = 3;
		int STATS_NUMBER_WISHING = 4;
		int STATS_USERS_RATED = 5;
		int STATS_NUMBER_COMMENTS = 6;
	}
}
