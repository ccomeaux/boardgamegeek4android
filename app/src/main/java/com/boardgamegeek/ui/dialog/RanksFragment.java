package com.boardgamegeek.ui.dialog;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.io.BggService;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.provider.BggContract.GameRanks;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.ui.widget.GameRankRow;
import com.boardgamegeek.util.ActivityUtils;
import com.boardgamegeek.util.PresentationUtils;
import com.boardgamegeek.util.UIUtils;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import timber.log.Timber;

public class RanksFragment extends DialogFragment implements LoaderCallbacks<Cursor> {
	private Uri uri;
	private Unbinder unbinder;
	@BindView(R.id.unranked) TextView unrankedView;
	@BindView(R.id.subtypes) ViewGroup subtypesView;
	@BindView(R.id.families) ViewGroup familiesView;
	@BindView(R.id.standard_deviation) TextView standardDeviationView;
	@BindView(R.id.votes) TextView votesView;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final Intent intent = UIUtils.fragmentArgumentsToIntent(getArguments());
		int gameId = intent.getIntExtra(ActivityUtils.KEY_GAME_ID, BggContract.INVALID_ID);
		if (gameId == BggContract.INVALID_ID) dismiss();
		uri = Games.buildRanksUri(gameId);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		getDialog().setTitle(R.string.title_ranks_ratings);

		ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.dialog_game_ranks, container, false);
		unbinder = ButterKnife.bind(this, rootView);
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
			subtypesView.removeAllViews();
			familiesView.removeAllViews();

			boolean hasRankedSubtype = false;
			CharSequence unrankedSubtype = "Game";
			if (cursor.moveToFirst()) {
				double standardDeviation = cursor.getDouble(Query.STATS_STANDARD_DEVIATION);
				int voteCount = cursor.getInt(Query.STATS_USERS_RATED);

				votesView.setText(PresentationUtils.getQuantityText(getContext(), R.plurals.votes_suffix, voteCount, voteCount));
				if (voteCount == 0) {
					standardDeviationView.setVisibility(View.GONE);
				} else {
					standardDeviationView.setText(PresentationUtils.getText(getContext(), R.string.standard_deviation_prefix, standardDeviation));
					standardDeviationView.setVisibility(View.VISIBLE);
				}

				do {
					String type = cursor.getString(Query.GAME_RANK_TYPE);
					CharSequence name = PresentationUtils.describeRankName(getContext(), type, cursor.getString(Query.GAME_RANK_NAME));
					int rank = cursor.getInt(Query.GAME_RANK_VALUE);
					double average = cursor.getDouble(Query.GAME_RANK_BAYES_AVERAGE);
					boolean isFamily = BggService.RANK_TYPE_FAMILY.equals(type);

					if (PresentationUtils.isRankValid(rank)) {
						GameRankRow row = new GameRankRow(getContext(), isFamily);
						row.setRank(rank);
						row.setName(name);
						row.setRatingView(average);
						switch (type) {
							case BggService.RANK_TYPE_SUBTYPE:
								subtypesView.addView(row);
								subtypesView.setVisibility(View.VISIBLE);
								unrankedView.setVisibility(View.GONE);
								hasRankedSubtype = true;
								break;
							case BggService.RANK_TYPE_FAMILY:
								familiesView.addView(row);
								familiesView.setVisibility(View.VISIBLE);
								break;
							default:
								Timber.i("Invalid rank type: %s", type);
								break;
						}
					} else if (BggService.RANK_TYPE_SUBTYPE.equals(type)) {
						unrankedSubtype = name;
					}
				} while (cursor.moveToNext());
			}
			if (!hasRankedSubtype && !TextUtils.isEmpty(unrankedSubtype)) {
				unrankedView.setText(PresentationUtils.getText(getContext(), R.string.unranked_prefix, unrankedSubtype));
				unrankedView.setVisibility(View.VISIBLE);
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
			GameRanks.GAME_RANK_NAME,
			GameRanks.GAME_RANK_VALUE,
			GameRanks.GAME_RANK_TYPE,
			GameRanks.GAME_RANK_BAYES_AVERAGE,
			Games.STATS_USERS_RATED,
			Games.STATS_STANDARD_DEVIATION
		};
		int GAME_RANK_NAME = 0;
		int GAME_RANK_VALUE = 1;
		int GAME_RANK_TYPE = 2;
		int GAME_RANK_BAYES_AVERAGE = 3;
		int STATS_USERS_RATED = 4;
		int STATS_STANDARD_DEVIATION = 5;
	}
}
