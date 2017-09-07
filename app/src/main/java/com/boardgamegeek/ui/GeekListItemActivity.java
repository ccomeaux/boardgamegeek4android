package com.boardgamegeek.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.view.MenuItem;

import com.boardgamegeek.R;
import com.boardgamegeek.model.GeekListComment;
import com.boardgamegeek.model.GeekListItem;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.ui.model.GeekList;
import com.boardgamegeek.util.ActivityUtils;
import com.boardgamegeek.util.ImageUtils;
import com.boardgamegeek.util.ScrimUtils;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.ContentViewEvent;

import java.util.ArrayList;

public class GeekListItemActivity extends HeroTabActivity {
	private static final String KEY_ID = "GEEK_LIST_ID";
	private static final String KEY_ORDER = "GEEK_LIST_ORDER";
	private static final String KEY_TITLE = "GEEK_LIST_TITLE";
	private static final String KEY_TYPE = "GEEK_LIST_TYPE";
	private static final String KEY_USERNAME = "GEEK_LIST_USERNAME";
	private static final String KEY_NAME = "GEEK_LIST_NAME";
	private static final String KEY_THUMBS = "GEEK_LIST_THUMBS";
	private static final String KEY_IMAGE_ID = "GEEK_LIST_IMAGE_ID";
	private static final String KEY_POSTED_DATE = "GEEK_LIST_POSTED_DATE";
	private static final String KEY_EDITED_DATE = "GEEK_LIST_EDITED_DATE";
	private static final String KEY_BODY = "GEEK_LIST_BODY";
	private static final String KEY_OBJECT_ID = "GEEK_LIST_OBJECT_ID";
	private static final String KEY_OBJECT_URL = "GEEK_LIST_OBJECT_URL";
	private static final String KEY_IS_BOARD_GAME = "GEEK_LIST_IS_BOARD_GAME";
	private static final String KEY_COMMENTS = "GEEK_LIST_COMMENTS";

	private int geekListId;
	private String geekListTitle;
	private int objectId;
	private String objectName;
	private String url;
	private boolean isBoardGame;
	private int order;
	private String type;
	private String username;
	private int numberOfThumbs;
	private long postedDate;
	private long editedDate;
	private String body;
	private ArrayList<GeekListComment> comments;

	public static void start(Context context, GeekList geekList, GeekListItem item, int order) {
		Intent starter = new Intent(context, GeekListItemActivity.class);
		starter.putExtra(KEY_ID, geekList.getId());
		starter.putExtra(KEY_TITLE, geekList.getTitle());
		starter.putExtra(KEY_ORDER, order);
		starter.putExtra(KEY_NAME, item.getObjectName());
		if (item.getObjectTypeResId() != GeekListItem.INVALID_OBJECT_TYPE_RES_ID) {
			starter.putExtra(KEY_TYPE, context.getString(item.getObjectTypeResId()));
		}
		starter.putExtra(KEY_IMAGE_ID, item.imageId());
		starter.putExtra(KEY_USERNAME, item.getUsername());
		starter.putExtra(KEY_THUMBS, item.getThumbCount());
		starter.putExtra(KEY_POSTED_DATE, item.getPostDate());
		starter.putExtra(KEY_EDITED_DATE, item.getEditDate());
		starter.putExtra(KEY_BODY, item.getBody());
		starter.putExtra(KEY_OBJECT_URL, item.getObjectUrl());
		starter.putExtra(KEY_OBJECT_ID, item.getObjectId());
		starter.putExtra(KEY_IS_BOARD_GAME, item.isBoardGame());
		starter.putParcelableArrayListExtra(KEY_COMMENTS, item.getComments());
		context.startActivity(starter);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final Intent intent = getIntent();
		geekListTitle = intent.getStringExtra(KEY_TITLE);
		geekListId = intent.getIntExtra(KEY_ID, BggContract.INVALID_ID);
		objectId = intent.getIntExtra(KEY_OBJECT_ID, BggContract.INVALID_ID);
		objectName = intent.getStringExtra(KEY_NAME);
		url = intent.getStringExtra(KEY_OBJECT_URL);
		isBoardGame = intent.getBooleanExtra(KEY_IS_BOARD_GAME, false);
		order = intent.getIntExtra(KEY_ORDER, 0);
		type = intent.getStringExtra(KEY_TYPE);
		username = intent.getStringExtra(KEY_USERNAME);
		numberOfThumbs = intent.getIntExtra(KEY_THUMBS, 0);
		postedDate = intent.getLongExtra(KEY_POSTED_DATE, 0);
		editedDate = intent.getLongExtra(KEY_EDITED_DATE, 0);
		body = intent.getStringExtra(KEY_BODY);
		comments = intent.getParcelableArrayListExtra(KEY_COMMENTS);
		int imageId = intent.getIntExtra(KEY_IMAGE_ID, BggContract.INVALID_ID);

		initializeViewPager();

		safelySetTitle(objectName);

		if (savedInstanceState == null) {
			Answers.getInstance().logContentView(new ContentViewEvent()
				.putContentType("GeekListItem")
				.putContentId(String.valueOf(objectId))
				.putContentName(objectName));
		}

		ScrimUtils.applyDarkScrim(scrimView);
		ImageUtils.safelyLoadImage(toolbarImage, imageId, null);
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
					GeekListActivity.startUp(this, geekListId, geekListTitle);
					finish();
				} else {
					onBackPressed();
				}
				return true;
			case R.id.menu_view:
				if (isBoardGame) {
					GameActivity.start(this, objectId, objectName);
				} else {
					ActivityUtils.link(this, url);
				}
				return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void setUpViewPager() {
		GeekListItemPagerAdapter adapter = new GeekListItemPagerAdapter(getSupportFragmentManager());
		viewPager.setAdapter(adapter);
	}

	private final class GeekListItemPagerAdapter extends FragmentPagerAdapter {
		public GeekListItemPagerAdapter(FragmentManager fragmentManager) {
			super(fragmentManager);
		}

		@Override
		public CharSequence getPageTitle(int position) {
			if (position == 0) return getString(R.string.title_description);
			if (position == 1) return getString(R.string.title_comments);
			return "";
		}

		@Override
		public Fragment getItem(int position) {
			if (position == 0)
				return GeekListItemFragment.newInstance(order, geekListTitle, type, username, numberOfThumbs, postedDate, editedDate, body);
			if (position == 1)
				return GeekListCommentsFragment.newInstance(comments);
			return null;
		}

		@Override
		public int getCount() {
			return 2;
		}
	}
}
