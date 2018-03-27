package com.boardgamegeek.ui;

import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.graphics.Palette;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.boardgamegeek.R;
import com.boardgamegeek.events.CollectionItemChangedEvent;
import com.boardgamegeek.events.CollectionItemDeletedEvent;
import com.boardgamegeek.events.CollectionItemUpdatedEvent;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.provider.BggContract.Collection;
import com.boardgamegeek.service.SyncService;
import com.boardgamegeek.tasks.DeleteCollectionItemTask;
import com.boardgamegeek.tasks.ResetCollectionItemTask;
import com.boardgamegeek.tasks.sync.SyncCollectionByGameTask;
import com.boardgamegeek.util.DialogUtils;
import com.boardgamegeek.util.DialogUtils.OnDiscardListener;
import com.boardgamegeek.util.ImageUtils;
import com.boardgamegeek.util.ImageUtils.Callback;
import com.boardgamegeek.util.PaletteUtils;
import com.boardgamegeek.util.PresentationUtils;
import com.boardgamegeek.util.ScrimUtils;
import com.boardgamegeek.util.TaskUtils;
import com.boardgamegeek.util.UIUtils;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.ContentViewEvent;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.jetbrains.annotations.NotNull;

import butterknife.OnClick;
import hugo.weaving.DebugLog;
import icepick.Icepick;
import icepick.State;

public class GameCollectionItemActivity extends HeroActivity {
	private static final String KEY_INTERNAL_ID = "_ID";
	private static final String KEY_GAME_ID = "GAME_ID";
	private static final String KEY_GAME_NAME = "GAME_NAME";
	private static final String KEY_COLLECTION_ID = "COLLECTION_ID";
	private static final String KEY_COLLECTION_NAME = "COLLECTION_NAME";
	private static final String KEY_IMAGE_URL = "IMAGE_URL";
	private static final String KEY_YEAR_PUBLISHED = "YEAR_PUBLISHED";
	private static final String KEY_COLLECTION_YEAR_PUBLISHED = "COLLECTION_YEAR_PUBLISHED";
	private static final int INVALID_YEAR = 0;
	private long internalId;
	private int gameId;
	private String gameName;
	private int collectionId;
	private String collectionName;
	private String imageUrl;
	private String thumbnailUrl;
	private String heroImageUrl;
	private int yearPublished;
	private int collectionYearPublished;
	@State boolean isInEditMode;
	private boolean isItemUpdated;

	public static void start(Context context, long internalId, int gameId, String gameName, int collectionId, String collectionName, String imageUrl, int yearPublished, int collectionYearPublished) {
		Intent starter = new Intent(context, GameCollectionItemActivity.class);
		starter.putExtra(KEY_INTERNAL_ID, internalId);
		starter.putExtra(KEY_GAME_ID, gameId);
		starter.putExtra(KEY_GAME_NAME, gameName);
		starter.putExtra(KEY_COLLECTION_ID, collectionId);
		starter.putExtra(KEY_COLLECTION_NAME, collectionName);
		starter.putExtra(KEY_IMAGE_URL, imageUrl);
		starter.putExtra(KEY_YEAR_PUBLISHED, yearPublished);
		starter.putExtra(KEY_COLLECTION_YEAR_PUBLISHED, collectionYearPublished);
		context.startActivity(starter);
	}

	@DebugLog
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Icepick.restoreInstanceState(this, savedInstanceState);

		safelySetTitle();

		if (savedInstanceState == null) {
			Answers.getInstance().logContentView(new ContentViewEvent()
				.putContentType("GameCollection")
				.putContentId(String.valueOf(collectionId))
				.putContentName(collectionName));
		}
		PresentationUtils.ensureFabIsShown(fab);
	}

	@Override
	protected void readIntent(Intent intent) {
		internalId = intent.getLongExtra(KEY_INTERNAL_ID, BggContract.INVALID_ID);
		gameId = intent.getIntExtra(KEY_GAME_ID, BggContract.INVALID_ID);
		gameName = intent.getStringExtra(KEY_GAME_NAME);
		collectionId = intent.getIntExtra(KEY_COLLECTION_ID, BggContract.INVALID_ID);
		collectionName = intent.getStringExtra(KEY_COLLECTION_NAME);
		imageUrl = intent.getStringExtra(KEY_IMAGE_URL);
		yearPublished = intent.getIntExtra(KEY_YEAR_PUBLISHED, INVALID_YEAR);
		collectionYearPublished = intent.getIntExtra(KEY_COLLECTION_YEAR_PUBLISHED, INVALID_YEAR);
		if (imageUrl == null) imageUrl = "";
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
		return GameCollectionItemFragment.newInstance(gameId, collectionId);
	}

	@Override
	protected int getOptionsMenuId() {
		return R.menu.game_collection;
	}

	@Override
	public boolean onPrepareOptionsMenu(@NotNull Menu menu) {
		UIUtils.enableMenuItem(menu, R.id.menu_view_image, !TextUtils.isEmpty(imageUrl));
		return super.onPrepareOptionsMenu(menu);
	}

	@DebugLog
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				if (gameId == BggContract.INVALID_ID) {
					onBackPressed();
				} else {
					GameActivity.startUp(this, gameId, gameName, imageUrl);
				}
				finish();
				return true;
			case R.id.menu_view_image:
				ImageActivity.start(this, imageUrl);
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

	private void safelySetTitle() {
		if (collectionYearPublished == INVALID_YEAR || collectionYearPublished == yearPublished)
			safelySetTitle(collectionName);
		else
			safelySetTitle(String.format("%s (%s)", collectionName, collectionYearPublished));
	}

	@SuppressWarnings("unused")
	@DebugLog
	@Subscribe(threadMode = ThreadMode.MAIN)
	public void onEvent(CollectionItemChangedEvent event) {
		collectionName = event.getCollectionName();
		collectionYearPublished = event.getYearPublished();
		if (!event.getImageUrl().equals(imageUrl) ||
			!event.getThumbnailUrl().equals(thumbnailUrl) ||
			!event.getHeroImageUrl().equals(heroImageUrl)) {
			imageUrl = event.getImageUrl();
			thumbnailUrl = event.getThumbnailUrl();
			heroImageUrl = event.getHeroImageUrl();
			ImageUtils.safelyLoadImage(toolbarImage, imageUrl, thumbnailUrl, heroImageUrl, imageLoadCallback);
		} else {
			imageUrl = event.getImageUrl();
			thumbnailUrl = event.getThumbnailUrl();
			heroImageUrl = event.getHeroImageUrl();
		}
		safelySetTitle();
	}

	private final Callback imageLoadCallback = new Callback() {
		@DebugLog
		@Override
		public void onSuccessfulImageLoad(Palette palette) {
			ScrimUtils.applyDarkScrim(scrimView);
			if (palette != null) {
				((GameCollectionItemFragment) getFragment()).onPaletteGenerated(palette);
				PresentationUtils.colorFab(fab, PaletteUtils.getIconSwatch(palette).getRgb());
				fab.show();
			}

			new Handler().post(new Runnable() {
				@Override
				public void run() {
					final String url = (String) toolbarImage.getTag(R.id.url);
					if (!TextUtils.isEmpty(url) &&
						!url.equals(imageUrl) &&
						!url.equals(thumbnailUrl) &&
						!url.equals(heroImageUrl)) {
						ContentValues values = new ContentValues();
						values.put(Collection.COLLECTION_HERO_IMAGE_URL, url);
						getContentResolver().update(Collection.CONTENT_URI,
							values,
							Collection.COLLECTION_ID + "=?",
							new String[] { String.valueOf(collectionId) });
					}
				}
			});
		}

		@Override
		public void onFailedImageLoad() {
			fab.show();
		}
	};

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
		swipeRefreshLayout.setEnabled(!isInEditMode);
		((GameCollectionItemFragment) getFragment()).enableEditMode(isInEditMode);
		fab.setImageResource(isInEditMode ? R.drawable.fab_done : R.drawable.fab_edit);
	}
}
