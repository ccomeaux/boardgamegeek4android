package com.boardgamegeek.ui;


import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.ui.model.GameCollectionItem;
import com.boardgamegeek.ui.widget.GameCollectionRow;
import com.boardgamegeek.util.ActivityUtils;
import com.boardgamegeek.util.UIUtils;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import hugo.weaving.DebugLog;

public class GameCollectionFragment extends Fragment implements LoaderCallbacks<Cursor> {
	private Uri gameUri;
	private String gameName;

	Unbinder unbinder;
	@BindView(R.id.collection_container) ViewGroup collectionContainer;

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
		View rootView = inflater.inflate(R.layout.fragment_game_collection, container, false);
		unbinder = ButterKnife.bind(this, rootView);

		getLoaderManager().restartLoader(0, null, this);

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
		return new CursorLoader(getActivity(), GameCollectionItem.URI, GameCollectionItem.PROJECTION, GameCollectionItem.getSelection(), GameCollectionItem.getSelectionArgs(gameId), null);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		if (getActivity() == null) return;
		if (cursor != null && cursor.moveToFirst()) {
			collectionContainer.removeAllViews();
			do {
				GameCollectionRow row = new GameCollectionRow(getActivity());
				GameCollectionItem item = GameCollectionItem.fromCursor(getContext(), cursor);
				row.bind(item.getInternalId(), Games.getGameId(gameUri), gameName, item.getCollectionId(), item.getYearPublished(), item.getImageUrl());
				row.setThumbnail(item.getThumbnailUrl());
				row.setStatus(item.getStatuses(), item.getNumberOfPlays(), item.getRating(), item.getComment());
				row.setDescription(item.getCollectionName(), item.getCollectionYearPublished());
				row.setComment(item.getComment());
				row.setRating(item.getRating());

				collectionContainer.addView(row);
			} while (cursor.moveToNext());
		}
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
	}
}
