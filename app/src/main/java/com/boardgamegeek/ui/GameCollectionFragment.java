package com.boardgamegeek.ui;


import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v4.widget.SwipeRefreshLayout.OnRefreshListener;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.tasks.sync.SyncCollectionByGameTask;
import com.boardgamegeek.ui.model.GameCollectionItem;
import com.boardgamegeek.ui.widget.GameCollectionRow;
import com.boardgamegeek.ui.widget.TimestampView;
import com.boardgamegeek.util.ActivityUtils;
import com.boardgamegeek.util.DateTimeUtils;
import com.boardgamegeek.util.PresentationUtils;
import com.boardgamegeek.util.TaskUtils;
import com.boardgamegeek.util.UIUtils;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import hugo.weaving.DebugLog;
import icepick.State;

public class GameCollectionFragment extends Fragment implements LoaderCallbacks<Cursor>, OnRefreshListener {
	private static final int AGE_IN_DAYS_TO_REFRESH = 3;

	private Uri gameUri;
	private String gameName;
	private boolean isRefreshing;
	@State boolean mightNeedRefreshing = true;

	Unbinder unbinder;
	@BindView(R.id.swipe_refresh) SwipeRefreshLayout swipeRefreshLayout;
	@BindView(R.id.collection_container) ViewGroup collectionContainer;
	@BindView(R.id.sync_timestamp) TimestampView syncTimestampView;

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

		swipeRefreshLayout.setOnRefreshListener(this);
		swipeRefreshLayout.setColorSchemeResources(PresentationUtils.getColorSchemeResources());

		getLoaderManager().restartLoader(0, null, this);

		return rootView;
	}

	@Override
	public void onStart() {
		super.onStart();
		EventBus.getDefault().register(this);
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
		int gameId = Games.getGameId(gameUri);
		return new CursorLoader(getContext(), GameCollectionItem.URI, GameCollectionItem.PROJECTION, GameCollectionItem.getSelection(), GameCollectionItem.getSelectionArgs(gameId), null);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		if (getActivity() == null) return;
		if (cursor != null && cursor.moveToFirst()) {
			long oldestSyncTimestamp = Long.MAX_VALUE;
			collectionContainer.removeAllViews();
			do {
				GameCollectionItem item = GameCollectionItem.fromCursor(getContext(), cursor);

				oldestSyncTimestamp = Math.min(item.getSyncTimestamp(), oldestSyncTimestamp);

				GameCollectionRow row = new GameCollectionRow(getContext());
				row.bind(item.getInternalId(), Games.getGameId(gameUri), gameName, item.getCollectionId(), item.getYearPublished(), item.getImageUrl());
				row.setThumbnail(item.getThumbnailUrl());
				row.setStatus(item.getStatuses(), item.getNumberOfPlays(), item.getRating(), item.getComment());
				row.setDescription(item.getCollectionName(), item.getCollectionYearPublished());
				row.setComment(item.getComment());
				row.setRating(item.getRating());

				collectionContainer.addView(row);
			} while (cursor.moveToNext());

			if (oldestSyncTimestamp != Long.MAX_VALUE) {
				syncTimestampView.setVisibility(View.VISIBLE);
				syncTimestampView.setTimestamp(oldestSyncTimestamp);
			} else {
				syncTimestampView.setVisibility(View.GONE);
			}

			if (mightNeedRefreshing) {
				mightNeedRefreshing = false;
				if (DateTimeUtils.howManyDaysOld(oldestSyncTimestamp) > AGE_IN_DAYS_TO_REFRESH)
					requestRefresh();
			}
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
			TaskUtils.executeAsyncTask(new SyncCollectionByGameTask(getContext(), Games.getGameId(gameUri)));
		} else {
			updateRefreshStatus(false);
		}
	}

	@SuppressWarnings("unused")
	@Subscribe(threadMode = ThreadMode.MAIN)
	public void onEvent(SyncCollectionByGameTask.CompletedEvent event) {
		if (event.getGameId() == Games.getGameId(gameUri)) {
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
}
