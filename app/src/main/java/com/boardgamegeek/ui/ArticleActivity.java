package com.boardgamegeek.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;

import com.boardgamegeek.R;
import com.boardgamegeek.entities.ForumEntity.ForumType;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.entities.ArticleEntity;
import com.boardgamegeek.util.ActivityUtils;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.ContentViewEvent;
import com.crashlytics.android.answers.ShareEvent;

import org.jetbrains.annotations.NotNull;

import androidx.appcompat.app.ActionBar;
import androidx.fragment.app.Fragment;

public class ArticleActivity extends SimpleSinglePaneActivity {
	private static final String KEY_FORUM_ID = "FORUM_ID";
	private static final String KEY_FORUM_TITLE = "FORUM_TITLE";
	private static final String KEY_OBJECT_ID = "OBJECT_ID";
	private static final String KEY_OBJECT_NAME = "OBJECT_NAME";
	private static final String KEY_OBJECT_TYPE = "OBJECT_TYPE";
	private static final String KEY_THREAD_ID = "THREAD_ID";
	private static final String KEY_THREAD_SUBJECT = "THREAD_SUBJECT";
	private static final String KEY_ARTICLE = "ARTICLE";

	private int threadId;
	private String threadSubject;
	private int forumId;
	private String forumTitle;
	private int objectId;
	private String objectName;
	private ForumType objectType;
	private ArticleEntity article;

	public static void start(Context context, int threadId, String threadSubject, int forumId, String forumTitle, int objectId, String objectName, ForumType objectType, ArticleEntity article) {
		Intent starter = new Intent(context, ArticleActivity.class);
		starter.putExtra(KEY_THREAD_ID, threadId);
		starter.putExtra(KEY_THREAD_SUBJECT, threadSubject);
		starter.putExtra(KEY_FORUM_ID, forumId);
		starter.putExtra(KEY_FORUM_TITLE, forumTitle);
		starter.putExtra(KEY_OBJECT_ID, objectId);
		starter.putExtra(KEY_OBJECT_NAME, objectName);
		starter.putExtra(KEY_OBJECT_TYPE, objectType);
		starter.putExtra(KEY_ARTICLE, article);
		context.startActivity(starter);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final ActionBar actionBar = getSupportActionBar();
		if (actionBar != null) {
			if (TextUtils.isEmpty(objectName)) {
				actionBar.setTitle(forumTitle);
				actionBar.setSubtitle(threadSubject);
			} else {
				actionBar.setTitle(threadSubject + " - " + forumTitle);
				actionBar.setSubtitle(objectName);
			}
		}
		if (savedInstanceState == null) {
			Answers.getInstance().logContentView(new ContentViewEvent()
				.putContentType("Article")
				.putContentId(String.valueOf(article.getId()))
				.putContentName(threadSubject));
		}
	}

	@Override
	protected void readIntent(Intent intent) {
		threadId = intent.getIntExtra(KEY_THREAD_ID, BggContract.INVALID_ID);
		threadSubject = intent.getStringExtra(KEY_THREAD_SUBJECT);
		forumId = intent.getIntExtra(KEY_FORUM_ID, BggContract.INVALID_ID);
		forumTitle = intent.getStringExtra(KEY_FORUM_TITLE);
		objectId = intent.getIntExtra(KEY_OBJECT_ID, BggContract.INVALID_ID);
		objectName = intent.getStringExtra(KEY_OBJECT_NAME);
		objectType = (ForumType) intent.getSerializableExtra(KEY_OBJECT_TYPE);
		article = intent.getParcelableExtra(KEY_ARTICLE);
	}

	@NotNull
	@Override
	protected Fragment onCreatePane(@NotNull Intent intent) {
		return ArticleFragment.newInstance(article);
	}

	@Override
	protected int getOptionsMenuId() {
		return R.menu.view_share;
	}

	@Override
	public boolean onOptionsItemSelected(@NotNull MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				ThreadActivity.startUp(this, threadId, threadSubject, forumId, forumTitle, objectId, objectName, objectType);
				finish();
				return true;
			case R.id.menu_view:
				ActivityUtils.link(this, article.getLink());
				return true;
			case R.id.menu_share:
				String description = TextUtils.isEmpty(objectName) ?
					String.format(getString(R.string.share_thread_article_text), threadSubject, forumTitle) :
					String.format(getString(R.string.share_thread_article_object_text), threadSubject, forumTitle, objectName);
				ActivityUtils.share(this, getString(R.string.share_thread_subject), description + "\n\n" + article.getLink(), R.string.title_share);
				String contentName = TextUtils.isEmpty(objectName) ?
					String.format("%s | %s", forumTitle, threadSubject) :
					String.format("%s | %s | %s", objectName, forumTitle, threadSubject);
				Answers.getInstance().logShare(new ShareEvent()
					.putContentType("Article")
					.putContentName(contentName)
					.putContentId(String.valueOf(article.getId())));
				return true;
		}
		return super.onOptionsItemSelected(item);
	}
}
