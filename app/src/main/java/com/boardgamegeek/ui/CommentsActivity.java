package com.boardgamegeek.ui;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.util.UIUtils;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.ContentViewEvent;

import icepick.Icepick;
import icepick.State;

public class CommentsActivity extends SimpleSinglePaneActivity {
	private static final String KEY_GAME_NAME = "GAME_NAME";
	private static final String KEY_SORT_TYPE = "SORT_TYPE";
	public static final int SORT_TYPE_USER = 0;
	public static final int SORT_TYPE_RATING = 1;

	private int gameId;
	private String gameName;
	@State int sortType;

	public static void startComments(Context context, Uri gameUri, String gameName) {
		Intent starter = new Intent(context, CommentsActivity.class);
		starter.setData(gameUri);
		starter.putExtra(KEY_GAME_NAME, gameName);
		starter.putExtra(KEY_SORT_TYPE, SORT_TYPE_USER);
		context.startActivity(starter);
	}

	public static void startRating(Context context, Uri gameUri, String gameName) {
		Intent starter = new Intent(context, CommentsActivity.class);
		starter.setData(gameUri);
		starter.putExtra(KEY_GAME_NAME, gameName);
		starter.putExtra(KEY_SORT_TYPE, SORT_TYPE_RATING);
		context.startActivity(starter);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Icepick.restoreInstanceState(this, savedInstanceState);

		updateActionBar();

		if (savedInstanceState == null) {
			Answers.getInstance().logContentView(new ContentViewEvent()
				.putContentType("GameComments")
				.putContentId(String.valueOf(gameId))
				.putContentName(gameName));
		}
	}

	@Override
	public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
		super.onSaveInstanceState(outState, outPersistentState);
		Icepick.saveInstanceState(this, outState);
	}

	private void updateActionBar() {
		ActionBar actionBar = getSupportActionBar();
		if (actionBar != null) {
			if (!TextUtils.isEmpty(gameName)) {
				actionBar.setTitle(gameName);
				actionBar.setSubtitle(sortType == SORT_TYPE_RATING ? R.string.title_ratings : R.string.title_comments);
			} else {
				actionBar.setTitle(sortType == SORT_TYPE_RATING ? R.string.title_ratings : R.string.title_comments);
			}
		}
	}

	@Override
	protected void readIntent(Intent intent) {
		gameId = BggContract.Games.getGameId(intent.getData());
		gameName = intent.getStringExtra(KEY_GAME_NAME);
		sortType = intent.getIntExtra(KEY_SORT_TYPE, SORT_TYPE_USER);
	}

	@Override
	protected Fragment onCreatePane(Intent intent) {
		return CommentsFragment.newInstance(gameId, sortType == SORT_TYPE_RATING);
	}

	@Override
	protected int getOptionsMenuId() {
		return R.menu.game_comments;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		if (sortType == SORT_TYPE_RATING) {
			UIUtils.checkMenuItem(menu, R.id.menu_sort_rating);
		} else {
			UIUtils.checkMenuItem(menu, R.id.menu_sort_comments);
		}
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				GameActivity.startUp(this, gameId, gameName);
				finish();
				return true;
			case R.id.menu_sort_comments:
				sortType = SORT_TYPE_USER;
				updateActionBar();
				supportInvalidateOptionsMenu();
				((CommentsFragment) getFragment()).setSort(sortType);
				return true;
			case R.id.menu_sort_rating:
				sortType = SORT_TYPE_RATING;
				updateActionBar();
				supportInvalidateOptionsMenu();
				((CommentsFragment) getFragment()).setSort(sortType);
				return true;
		}
		return super.onOptionsItemSelected(item);
	}
}
