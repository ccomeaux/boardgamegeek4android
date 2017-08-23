package com.boardgamegeek.ui;

import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.graphics.Palette;
import android.text.TextUtils;
import android.view.MenuItem;
import android.widget.Toast;

import com.boardgamegeek.R;
import com.boardgamegeek.events.CollectionItemChangedEvent;
import com.boardgamegeek.events.CollectionItemDeletedEvent;
import com.boardgamegeek.events.CollectionItemUpdatedEvent;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.service.SyncService;
import com.boardgamegeek.tasks.DeleteCollectionItemTask;
import com.boardgamegeek.tasks.ResetCollectionItemTask;
import com.boardgamegeek.tasks.sync.SyncCollectionByGameTask;
import com.boardgamegeek.util.ActivityUtils;
import com.boardgamegeek.util.DialogUtils;
import com.boardgamegeek.util.DialogUtils.OnDiscardListener;
import com.boardgamegeek.util.ImageUtils;
import com.boardgamegeek.util.ImageUtils.Callback;
import com.boardgamegeek.util.PaletteUtils;
import com.boardgamegeek.util.PresentationUtils;
import com.boardgamegeek.util.ScrimUtils;
import com.boardgamegeek.util.TaskUtils;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.ContentViewEvent;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import butterknife.OnClick;
import hugo.weaving.DebugLog;
import icepick.Icepick;
import icepick.State;

public class GameCollectionItemActivity extends HeroActivity implements Callback {
	private long internalId;
	private int gameId;
	private String gameName;
	private String imageUrl;
	@State boolean isInEditMode;
	private boolean isItemUpdated;

	@DebugLog
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final Intent intent = getIntent();
		int collectionId = intent.getIntExtra(ActivityUtils.KEY_COLLECTION_ID, BggContract.INVALID_ID);
		gameId = intent.getIntExtra(ActivityUtils.KEY_GAME_ID, BggContract.INVALID_ID);
		internalId = intent.getLongExtra(ActivityUtils.KEY_INTERNAL_ID, BggContract.INVALID_ID);
		gameName = intent.getStringExtra(ActivityUtils.KEY_GAME_NAME);
		String collectionName = intent.getStringExtra(ActivityUtils.KEY_COLLECTION_NAME);
		imageUrl = intent.getStringExtra(ActivityUtils.KEY_IMAGE_URL);

		Icepick.restoreInstanceState(this, savedInstanceState);

		safelySetTitle(collectionName);

		if (savedInstanceState == null) {
			Answers.getInstance().logContentView(new ContentViewEvent()
				.putContentType("GameCollection")
				.putContentId(String.valueOf(collectionId))
				.putContentName(collectionName));
		}
		PresentationUtils.ensureFabIsShown(fab);
	}

	@Override
	protected void onResume() {
		super.onResume();
		displayEditMode();
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		Icepick.saveInstanceState(this, outState);
	}

	@Override
	public void onBackPressed() {
		if (isInEditMode) {
			if (isItemUpdated) {
				DialogUtils.createDiscardDialog(this, R.string.collection_item, false, false, new OnDiscardListener() {
					public void onDiscard() {
						TaskUtils.executeAsyncTask(new ResetCollectionItemTask(GameCollectionItemActivity.this, internalId));
						toggleEditMode();
					}
				}).show();
			} else {
				toggleEditMode();
			}
		} else {
			super.onBackPressed();
		}
	}

	@DebugLog
	@Override
	protected Fragment onCreatePane() {
		return new GameCollectionItemFragment();
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
					GameActivity.startUp(this, gameId, gameName);
				}
				finish();
				return true;
			case R.id.menu_view_image:
				ActivityUtils.startImageActivity(this, imageUrl);
				return true;
			case R.id.menu_delete:
				DialogUtils.createThemedBuilder(this)
					.setMessage(R.string.are_you_sure_delete_collection_item)
					.setPositiveButton(R.string.delete, new OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							TaskUtils.executeAsyncTask(new DeleteCollectionItemTask(GameCollectionItemActivity.this, internalId));
							finish();
						}
					})
					.setNegativeButton(R.string.cancel, null)
					.setCancelable(true)
					.show();
				return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@SuppressWarnings("unused")
	@DebugLog
	@Subscribe(threadMode = ThreadMode.MAIN)
	public void onEvent(CollectionItemChangedEvent event) {
		safelySetTitle(event.getCollectionName());
		ScrimUtils.applyDarkScrim(scrimView);
		ImageUtils.safelyLoadImage(toolbarImage, event.getImageUrl(), this);
	}

	@DebugLog
	@Override
	public void onSuccessfulImageLoad(Palette palette) {
		((GameCollectionItemFragment) getFragment()).onPaletteGenerated(palette);
		fab.setBackgroundTintList(ColorStateList.valueOf(PaletteUtils.getIconSwatch(palette).getRgb()));
		fab.show();
	}

	@Override
	public void onFailedImageLoad() {
		fab.show();
	}

	@DebugLog
	@Override
	public void onRefresh() {
		if (isInEditMode) {
			updateRefreshStatus(false);
		} else if (((GameCollectionItemFragment) getFragment()).triggerRefresh()) {
			updateRefreshStatus(true);
		}
	}

	@SuppressWarnings("unused")
	@Subscribe(threadMode = ThreadMode.MAIN)
	public void onEvent(SyncCollectionByGameTask.CompletedEvent event) {
		if (event.getGameId() == gameId) {
			updateRefreshStatus(false);
			if (!TextUtils.isEmpty(event.getErrorMessage())) {
				Toast.makeText(this, event.getErrorMessage(), Toast.LENGTH_LONG).show();
			}
		}
	}

	@SuppressWarnings("unused")
	@Subscribe(threadMode = ThreadMode.MAIN)
	public void onEvent(CollectionItemDeletedEvent event) {
		if (internalId == event.getInternalId()) {
			Toast.makeText(this, R.string.msg_collection_item_deleted, Toast.LENGTH_LONG).show();
			SyncService.sync(this, SyncService.FLAG_SYNC_COLLECTION_UPLOAD);
			isItemUpdated = false;
		}
	}

	@SuppressWarnings("unused")
	@Subscribe(threadMode = ThreadMode.MAIN)
	public void onEvent(CollectionItemUpdatedEvent event) {
		if (internalId == event.getInternalId()) {
			isItemUpdated = true;
		}
	}

	@DebugLog
	@OnClick(R.id.fab)
	public void onFabClicked() {
		if (isInEditMode) ((GameCollectionItemFragment) getFragment()).syncChanges();
		toggleEditMode();
	}

	private void toggleEditMode() {
		isInEditMode = !isInEditMode;
		displayEditMode();
	}

	private void displayEditMode() {
		((GameCollectionItemFragment) getFragment()).enableEditMode(isInEditMode);
		fab.setImageResource(isInEditMode ? R.drawable.fab_done : R.drawable.fab_edit);
	}
}
