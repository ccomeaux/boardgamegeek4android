package com.boardgamegeek.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.graphics.Palette;
import android.view.MenuItem;

import com.boardgamegeek.events.CollectionItemChangedEvent;
import com.boardgamegeek.events.UpdateCompleteEvent;
import com.boardgamegeek.events.UpdateEvent;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.service.UpdateService;
import com.boardgamegeek.util.ActivityUtils;
import com.boardgamegeek.util.ImageUtils;
import com.boardgamegeek.util.ImageUtils.Callback;
import com.boardgamegeek.util.ScrimUtils;

import hugo.weaving.DebugLog;

public class GameCollectionActivity extends HeroActivity implements Callback {
	private int gameId;
	private String gameName;

	@DebugLog
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		gameId = getIntent().getIntExtra(ActivityUtils.KEY_GAME_ID, BggContract.INVALID_ID);
		gameName = getIntent().getStringExtra(ActivityUtils.KEY_GAME_NAME);
		String collectionName = getIntent().getStringExtra(ActivityUtils.KEY_COLLECTION_NAME);

		safelySetTitle(collectionName);
	}

	@DebugLog
	@Override
	protected Fragment onCreatePane(Intent intent) {
		return new GameCollectionFragment();
	}

	@DebugLog
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				if (gameId == BggContract.INVALID_ID) {
					onBackPressed();
				} else {
					ActivityUtils.navigateUpToGame(this, gameId, gameName);
				}
				finish();
				return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@SuppressWarnings("unused")
	@DebugLog
	public void onEventMainThread(CollectionItemChangedEvent event) {
		safelySetTitle(event.getCollectionName());
		ScrimUtils.applyInvertedScrim(scrimView);
		ImageUtils.safelyLoadImage(toolbarImage, event.getImageUrl(), this);
	}

	@DebugLog
	@Override
	public void onPaletteGenerated(Palette palette) {
		((GameCollectionFragment) getFragment()).onPaletteGenerated(palette);
	}

	@DebugLog
	@Override
	public void onRefresh() {
		((GameCollectionFragment) getFragment()).triggerRefresh();
	}

	@SuppressWarnings("unused")
	@DebugLog
	public void onEventMainThread(UpdateEvent event) {
		updateRefreshStatus(event.getType() == UpdateService.SYNC_TYPE_GAME_COLLECTION);
	}

	@SuppressWarnings("unused")
	@DebugLog
	public void onEventMainThread(@SuppressWarnings("UnusedParameters") UpdateCompleteEvent event) {
		updateRefreshStatus(false);
	}
}
