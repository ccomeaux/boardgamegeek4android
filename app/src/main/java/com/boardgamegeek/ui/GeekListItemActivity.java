package com.boardgamegeek.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.graphics.Palette;
import android.support.v7.graphics.Palette.Swatch;
import android.view.MenuItem;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.util.ActivityUtils;
import com.boardgamegeek.util.ImageUtils;
import com.boardgamegeek.util.PaletteUtils;
import com.boardgamegeek.util.ScrimUtils;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.ContentViewEvent;

public class GeekListItemActivity extends HeroActivity implements ImageUtils.Callback {
	private int geekListId;
	private String geekListTitle;
	private int objectId;
	private String objectName;
	private String url;
	private boolean isBoardGame;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final Intent intent = getIntent();
		geekListTitle = intent.getStringExtra(ActivityUtils.KEY_TITLE);
		geekListId = intent.getIntExtra(ActivityUtils.KEY_ID, BggContract.INVALID_ID);
		objectId = intent.getIntExtra(ActivityUtils.KEY_OBJECT_ID, BggContract.INVALID_ID);
		objectName = intent.getStringExtra(ActivityUtils.KEY_NAME);
		url = intent.getStringExtra(ActivityUtils.KEY_OBJECT_URL);
		isBoardGame = intent.getBooleanExtra(ActivityUtils.KEY_IS_BOARD_GAME, false);
		int imageId = intent.getIntExtra(ActivityUtils.KEY_IMAGE_ID, BggContract.INVALID_ID);

		safelySetTitle(objectName);

		if (savedInstanceState == null) {
			Answers.getInstance().logContentView(new ContentViewEvent()
				.putContentType("GeekListItem")
				.putContentId(String.valueOf(objectId))
				.putContentName(objectName));
		}

		ScrimUtils.applyInvertedScrim(scrimView);
		ImageUtils.safelyLoadImage(toolbarImage, imageId, this);
	}

	@Override
	protected Fragment onCreatePane() {
		return new GeekListItemFragment();
	}

	@Override
	protected int getOptionsMenuId() {
		return R.menu.view;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				if (geekListId != BggContract.INVALID_ID) {
					Intent intent = new Intent(this, GeekListActivity.class);
					intent.putExtra(ActivityUtils.KEY_ID, geekListId);
					intent.putExtra(ActivityUtils.KEY_TITLE, geekListTitle);
					intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
					startActivity(intent);
					finish();
				} else {
					onBackPressed();
				}
				return true;
			case R.id.menu_view:
				if (isBoardGame) {
					ActivityUtils.launchGame(this, objectId, objectName);
				} else {
					ActivityUtils.link(this, url);
				}
				return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected boolean isRefreshable() {
		return false;
	}

	@Override
	public void onSuccessfulImageLoad(Palette palette) {
		Swatch swatch = PaletteUtils.getInverseSwatch(palette, ContextCompat.getColor(this, R.color.info_background));
		((GeekListItemFragment) getFragment()).setSwatch(swatch);
	}

	@Override
	public void onFailedImageLoad() {
	}
}
