package com.boardgamegeek.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.util.ActivityUtils;
import com.boardgamegeek.util.HelpUtils;

public class ThreadActivity extends SimpleSinglePaneActivity {
	private static final int HELP_VERSION = 1;

	private int threadId;
	private String threadSubject;
	private int forumId;
	private String forumTitle;
	private int gameId;
	private String gameName;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final Intent intent = getIntent();
		threadId = intent.getIntExtra(ActivityUtils.KEY_THREAD_ID, BggContract.INVALID_ID);
		threadSubject = intent.getStringExtra(ActivityUtils.KEY_THREAD_SUBJECT);
		forumId = intent.getIntExtra(ActivityUtils.KEY_FORUM_ID, BggContract.INVALID_ID);
		forumTitle = intent.getStringExtra(ActivityUtils.KEY_FORUM_TITLE);
		gameId = intent.getIntExtra(ActivityUtils.KEY_GAME_ID, BggContract.INVALID_ID);
		gameName = intent.getStringExtra(ActivityUtils.KEY_GAME_NAME);

		final ActionBar actionBar = getSupportActionBar();
		if (actionBar != null) {
			if (TextUtils.isEmpty(gameName)) {
				actionBar.setTitle(forumTitle);
				actionBar.setSubtitle(threadSubject);
			} else {
				actionBar.setTitle(threadSubject + " - " + forumTitle);
				actionBar.setSubtitle(gameName);
			}
		}

		HelpUtils.showHelpDialog(this, HelpUtils.HELP_THREAD_KEY, HELP_VERSION, R.string.help_thread);
	}

	@Override
	protected Fragment onCreatePane(Intent intent) {
		return new ThreadFragment();
	}

	@Override
	protected int getOptionsMenuId() {
		return R.menu.view_share;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				Intent intent = new Intent(this, ForumActivity.class);
				intent.putExtra(ActivityUtils.KEY_FORUM_ID, forumId);
				intent.putExtra(ActivityUtils.KEY_FORUM_TITLE, forumTitle);
				intent.putExtra(ActivityUtils.KEY_GAME_ID, gameId);
				intent.putExtra(ActivityUtils.KEY_GAME_NAME, gameName);
				intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				startActivity(intent);
				finish();
				return true;
			case R.id.menu_view:
				ActivityUtils.linkToBgg(this, "thread/" + threadId);
				return true;
			case R.id.menu_share:
				String description = String.format(getString(R.string.share_thread_text), threadSubject, forumTitle,
					gameName);
				String link = ActivityUtils.createBggUri("thread/" + threadId).toString();
				ActivityUtils.share(this, getString(R.string.share_thread_subject), description + "\n\n" + link,
					R.string.title_share_game);
				return true;
		}
		return super.onOptionsItemSelected(item);
	}

	public void onButtonClick(View v) {
		Intent intent = new Intent(this, ArticleActivity.class);
		Bundle b = (Bundle) v.getTag();
		b.putInt(ActivityUtils.KEY_THREAD_ID, threadId);
		b.putString(ActivityUtils.KEY_THREAD_SUBJECT, threadSubject);
		b.putInt(ActivityUtils.KEY_FORUM_ID, forumId);
		b.putString(ActivityUtils.KEY_FORUM_TITLE, forumTitle);
		b.putInt(ActivityUtils.KEY_GAME_ID, gameId);
		b.putString(ActivityUtils.KEY_GAME_NAME, gameName);
		intent.putExtras(b);
		startActivity(intent);
	}
}
