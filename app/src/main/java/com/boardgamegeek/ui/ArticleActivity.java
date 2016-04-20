package com.boardgamegeek.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.text.TextUtils;
import android.view.MenuItem;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.util.ActivityUtils;

public class ArticleActivity extends SimpleSinglePaneActivity {
	private String threadId;
	private String threadSubject;
	private String forumId;
	private String forumTitle;
	private int gameId;
	private String gameName;
	private String link;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final Intent intent = getIntent();
		threadId = intent.getStringExtra(ActivityUtils.KEY_THREAD_ID);
		threadSubject = intent.getStringExtra(ActivityUtils.KEY_THREAD_SUBJECT);
		forumId = intent.getStringExtra(ActivityUtils.KEY_FORUM_ID);
		forumTitle = intent.getStringExtra(ActivityUtils.KEY_FORUM_TITLE);
		gameId = intent.getIntExtra(ActivityUtils.KEY_GAME_ID, BggContract.INVALID_ID);
		gameName = intent.getStringExtra(ActivityUtils.KEY_GAME_NAME);
		link = intent.getStringExtra(ActivityUtils.KEY_LINK);

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
	}

	@Override
	protected Fragment onCreatePane(Intent intent) {
		return new ArticleFragment();
	}

	@Override
	protected int getOptionsMenuId() {
		return R.menu.view_share;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				Intent intent = new Intent(this, ThreadActivity.class);
				intent.putExtra(ActivityUtils.KEY_THREAD_ID, threadId);
				intent.putExtra(ActivityUtils.KEY_THREAD_SUBJECT, threadSubject);
				intent.putExtra(ActivityUtils.KEY_FORUM_ID, forumId);
				intent.putExtra(ActivityUtils.KEY_FORUM_TITLE, forumTitle);
				intent.putExtra(ActivityUtils.KEY_GAME_ID, gameId);
				intent.putExtra(ActivityUtils.KEY_GAME_NAME, gameName);
				intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				startActivity(intent);
				finish();
				return true;
			case R.id.menu_view:
				ActivityUtils.link(this, link);
				return true;
			case R.id.menu_share:
				String description = String.format(getString(R.string.share_thread_article_text), threadSubject,
					forumTitle, gameName);
				ActivityUtils.share(this, getString(R.string.share_thread_subject), description + "\n\n" + link,
					R.string.title_share);
				return true;
		}
		return super.onOptionsItemSelected(item);
	}
}
