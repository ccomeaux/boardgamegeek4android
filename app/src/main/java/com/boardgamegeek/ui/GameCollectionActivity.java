package com.boardgamegeek.ui;

import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.graphics.Palette;
import android.view.MenuItem;
import android.widget.Toast;

import com.boardgamegeek.R;
import com.boardgamegeek.events.CollectionItemChangedEvent;
import com.boardgamegeek.events.CollectionItemDeletedEvent;
import com.boardgamegeek.events.UpdateCompleteEvent;
import com.boardgamegeek.events.UpdateEvent;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.service.SyncService;
import com.boardgamegeek.service.UpdateService;
import com.boardgamegeek.tasks.DeleteCollectionItemTask;
import com.boardgamegeek.util.ActivityUtils;
import com.boardgamegeek.util.DialogUtils;
import com.boardgamegeek.util.ImageUtils;
import com.boardgamegeek.util.ImageUtils.Callback;
import com.boardgamegeek.util.ScrimUtils;
import com.boardgamegeek.util.TaskUtils;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import hugo.weaving.DebugLog;

public class GameCollectionActivity extends HeroActivity implements Callback {
	private long internalId;
	private int gameId;
	private String gameName;
	private String imageUrl;

	@DebugLog
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final Intent intent = getIntent();
		gameId = intent.getIntExtra(ActivityUtils.KEY_GAME_ID, BggContract.INVALID_ID);
		internalId = intent.getLongExtra(ActivityUtils.KEY_INTERNAL_ID, BggContract.INVALID_ID);
		gameName = intent.getStringExtra(ActivityUtils.KEY_GAME_NAME);
		String collectionName = intent.getStringExtra(ActivityUtils.KEY_COLLECTION_NAME);
		imageUrl = intent.getStringExtra(ActivityUtils.KEY_IMAGE_URL);

		safelySetTitle(collectionName);
	}

	@DebugLog
	@Override
	protected Fragment onCreatePane(Intent intent) {
		return new GameCollectionFragment();
	}

	@Override
	protected int getOptionsMenuId() {
		return R.menu.game_collection;
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
			case R.id.menu_view_image:
				ActivityUtils.startImageActivity(this, imageUrl);
				return true;
			case R.id.menu_delete:
				DialogUtils.createConfirmationDialog(this,
					R.string.are_you_sure_delete_collection_item,
					new OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							TaskUtils.executeAsyncTask(new DeleteCollectionItemTask(GameCollectionActivity.this, internalId));
							finish();
						}
					}).show();
				return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@SuppressWarnings("unused")
	@DebugLog
	@Subscribe(threadMode = ThreadMode.MAIN)
	public void onEvent(CollectionItemChangedEvent event) {
		safelySetTitle(event.getCollectionName());
		ScrimUtils.applyInvertedScrim(scrimView);
		ImageUtils.safelyLoadImage(toolbarImage, event.getImageUrl(), this);
	}

	@DebugLog
	@Override
	public void onSuccessfulLoad(Palette palette) {
		((GameCollectionFragment) getFragment()).onPaletteGenerated(palette);
	}

	@DebugLog
	@Override
	public void onRefresh() {
		((GameCollectionFragment) getFragment()).triggerRefresh();
	}

	@SuppressWarnings("unused")
	@DebugLog
	@Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
	public void onEvent(UpdateEvent event) {
		updateRefreshStatus(event.getType() == UpdateService.SYNC_TYPE_GAME_COLLECTION);
	}

	@SuppressWarnings({ "unused", "UnusedParameters" })
	@DebugLog
	@Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
	public void onEvent(UpdateCompleteEvent event) {
		updateRefreshStatus(false);
	}

	@Subscribe(threadMode = ThreadMode.MAIN)
	public void onEvent(CollectionItemDeletedEvent event) {
		Toast.makeText(this, R.string.msg_collection_item_deleted, Toast.LENGTH_LONG).show();
		SyncService.sync(this, SyncService.FLAG_SYNC_COLLECTION_UPLOAD);
	}
}
