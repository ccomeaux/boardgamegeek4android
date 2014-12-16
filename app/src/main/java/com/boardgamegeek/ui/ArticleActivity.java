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
import com.boardgamegeek.util.ForumsUtils;

public class ArticleActivity extends SimpleSinglePaneActivity {
	private String mThreadId;
	private String mThreadSubject;
	private String mForumId;
	private String mForumTitle;
	private int mGameId;
	private String mGameName;
	private String mLink;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final Intent intent = getIntent();
		mThreadId = intent.getStringExtra(ForumsUtils.KEY_THREAD_ID);
		mThreadSubject = intent.getStringExtra(ForumsUtils.KEY_THREAD_SUBJECT);
		mForumId = intent.getStringExtra(ForumsUtils.KEY_FORUM_ID);
		mForumTitle = intent.getStringExtra(ForumsUtils.KEY_FORUM_TITLE);
		mGameId = intent.getIntExtra(ForumsUtils.KEY_GAME_ID, BggContract.INVALID_ID);
		mGameName = intent.getStringExtra(ForumsUtils.KEY_GAME_NAME);
		mLink = intent.getStringExtra(ForumsUtils.KEY_LINK);

		final ActionBar actionBar = getSupportActionBar();
		if (TextUtils.isEmpty(mGameName)) {
			actionBar.setTitle(mForumTitle);
			actionBar.setSubtitle(mThreadSubject);
		} else {
			actionBar.setTitle(mThreadSubject + " - " + mForumTitle);
			actionBar.setSubtitle(mGameName);
		}
	}

	@Override
	protected Fragment onCreatePane(Intent intent) {
		return new ArticleFragment();
	}

	@Override
	protected int getOptionsMenuId() {
		return R.menu.search_view_share;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				Intent intent = new Intent(this, ThreadActivity.class);
				intent.putExtra(ForumsUtils.KEY_THREAD_ID, mThreadId);
				intent.putExtra(ForumsUtils.KEY_THREAD_SUBJECT, mThreadSubject);
				intent.putExtra(ForumsUtils.KEY_FORUM_ID, mForumId);
				intent.putExtra(ForumsUtils.KEY_FORUM_TITLE, mForumTitle);
				intent.putExtra(ForumsUtils.KEY_GAME_ID, mGameId);
				intent.putExtra(ForumsUtils.KEY_GAME_NAME, mGameName);
				intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				startActivity(intent);
				finish();
				return true;
			case R.id.menu_view:
				ActivityUtils.link(this, mLink);
				return true;
			case R.id.menu_share:
				String description = String.format(getString(R.string.share_thread_article_text), mThreadSubject,
					mForumTitle, mGameName);
				ActivityUtils.share(this, getString(R.string.share_thread_subject), description + "\n\n" + mLink,
					R.string.title_share);
				return true;
		}
		return super.onOptionsItemSelected(item);
	}
}
