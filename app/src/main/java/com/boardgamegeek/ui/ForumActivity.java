package com.boardgamegeek.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;

import com.boardgamegeek.R;
import com.boardgamegeek.entities.ForumEntity.ForumType;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.util.ActivityUtils;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.fragment.app.Fragment;

public class ForumActivity extends SimpleSinglePaneActivity {
	private static final String KEY_FORUM_ID = "FORUM_ID";
	private static final String KEY_FORUM_TITLE = "FORUM_TITLE";
	private static final String KEY_OBJECT_TYPE = "OBJECT_TYPE";
	private static final String KEY_OBJECT_ID = "OBJECT_ID";
	private static final String KEY_OBJECT_NAME = "OBJECT_NAME";

	private ForumType objectType;
	private int objectId;
	private String objectName;
	private int forumId;
	private String forumTitle;

	public static void start(Context context, int forumId, String forumTitle, int objectId, String objectName, ForumType objectType) {
		Intent starter = createIntent(context, forumId, forumTitle, objectId, objectName, objectType);
		context.startActivity(starter);
	}

	public static void startUp(Context context, int forumId, String forumTitle, int objectId, String objectName, ForumType objectType) {
		Intent starter = createIntent(context, forumId, forumTitle, objectId, objectName, objectType);
		starter.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		context.startActivity(starter);
	}

	@NonNull
	private static Intent createIntent(Context context, int forumId, String forumTitle, int objectId, String objectName, ForumType objectType) {
		Intent starter = new Intent(context, ForumActivity.class);
		starter.putExtra(KEY_FORUM_ID, forumId);
		starter.putExtra(KEY_FORUM_TITLE, forumTitle);
		starter.putExtra(KEY_OBJECT_ID, objectId);
		starter.putExtra(KEY_OBJECT_NAME, objectName);
		starter.putExtra(KEY_OBJECT_TYPE, objectType);
		return starter;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (!TextUtils.isEmpty(forumTitle)) {
			final ActionBar actionBar = getSupportActionBar();
			if (actionBar != null) {
				if (!TextUtils.isEmpty(objectName)) {
					actionBar.setTitle(objectName);
				}
				actionBar.setSubtitle(forumTitle);
			}
		}
	}

	@Override
	protected void readIntent(Intent intent) {
		objectName = intent.getStringExtra(KEY_OBJECT_NAME);
		objectId = intent.getIntExtra(KEY_OBJECT_ID, BggContract.INVALID_ID);
		objectType = (ForumType) intent.getSerializableExtra(KEY_OBJECT_TYPE);
		forumId = intent.getIntExtra(KEY_FORUM_ID, BggContract.INVALID_ID);
		forumTitle = intent.getStringExtra(KEY_FORUM_TITLE);
	}

	@Override
	protected Fragment onCreatePane(Intent intent) {
		return ForumFragment.newInstance(forumId, forumTitle, objectId, objectName, objectType);
	}

	@Override
	protected int getOptionsMenuId() {
		return R.menu.view;
	}

	@Override
	public boolean onOptionsItemSelected(@NonNull MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				if (objectType == ForumType.REGION) {
					ForumsActivity.startUp(this);
				} else if (objectType == ForumType.GAME) {
					GameActivity.startUp(this, objectId, objectName);
				} else if (objectType == ForumType.ARTIST) {
					PersonActivity.startUpForArtist(this, objectId, objectName);
				} else if (objectType == ForumType.DESIGNER) {
					PersonActivity.startUpForDesigner(this, objectId, objectName);
				} else if (objectType == ForumType.PUBLISHER) {
					PersonActivity.startUpForPublisher(this, objectId, objectName);
				}
				finish();
				return true;
			case R.id.menu_view:
				ActivityUtils.linkToBgg(this, "forum/" + forumId);
				return true;
		}
		return super.onOptionsItemSelected(item);
	}
}
