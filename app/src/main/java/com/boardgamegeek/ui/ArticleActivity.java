package com.boardgamegeek.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;

import com.boardgamegeek.R;
import com.boardgamegeek.entities.ForumEntity.ForumType;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.ui.model.Article;
import com.boardgamegeek.util.ActivityUtils;

import org.jetbrains.annotations.NotNull;

import androidx.appcompat.app.ActionBar;
import androidx.fragment.app.Fragment;

public class ArticleActivity extends SimpleSinglePaneActivity {
	private static final String KEY_FORUM_ID = "FORUM_ID";
	private static final String KEY_FORUM_TITLE = "FORUM_TITLE";
	private static final String KEY_OBJECT_ID = "OBJECT_ID";
	private static final String KEY_OBJECT_NAME = "OBJECT_NAME";
	private static final String KEY_OBJECT_TYPE = "OBJECT_TYPE";
	private static final String KEY_USER = "USER";
	private static final String KEY_THREAD_ID = "THREAD_ID";
	private static final String KEY_THREAD_SUBJECT = "THREAD_SUBJECT";
	private static final String KEY_ARTICLE_ID = "ARTICLE_ID";
	private static final String KEY_POST_DATE = "POST_DATE";
	private static final String KEY_EDIT_DATE = "EDIT_DATE";
	private static final String KEY_EDIT_COUNT = "EDIT_COUNT";
	private static final String KEY_BODY = "BODY";
	private static final String KEY_LINK = "LINK";

	private int threadId;
	private String threadSubject;
	private int forumId;
	private String forumTitle;
	private int objectId;
	private String objectName;
	private ForumType objectType;
	private String link;
	private int articleId;
	private String user;
	private long postDate;
	private long editDate;
	private int editCount;
	private String body;

	public static void start(Context context, int threadId, String threadSubject, int forumId, String forumTitle, int objectId, String objectName, ForumType objectType, Article article) {
		Intent starter = new Intent(context, ArticleActivity.class);
		starter.putExtra(KEY_THREAD_ID, threadId);
		starter.putExtra(KEY_THREAD_SUBJECT, threadSubject);
		starter.putExtra(KEY_FORUM_ID, forumId);
		starter.putExtra(KEY_FORUM_TITLE, forumTitle);
		starter.putExtra(KEY_OBJECT_ID, objectId);
		starter.putExtra(KEY_OBJECT_NAME, objectName);
		starter.putExtra(KEY_OBJECT_TYPE, objectType);
		starter.putExtra(KEY_USER, article.getUsername());
		starter.putExtra(KEY_POST_DATE, article.getPostTicks());
		starter.putExtra(KEY_EDIT_DATE, article.getEditTicks());
		starter.putExtra(KEY_EDIT_COUNT, article.getNumberOfEdits());
		starter.putExtra(KEY_BODY, article.getBody());
		starter.putExtra(KEY_LINK, article.getLink());
		starter.putExtra(KEY_ARTICLE_ID, article.getId());
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
		link = intent.getStringExtra(KEY_LINK);
		articleId = intent.getIntExtra(KEY_ARTICLE_ID, BggContract.INVALID_ID);
		user = intent.getStringExtra(KEY_USER);
		postDate = intent.getLongExtra(KEY_POST_DATE, 0);
		editDate = intent.getLongExtra(KEY_EDIT_DATE, 0);
		editCount = intent.getIntExtra(KEY_EDIT_COUNT, 0);
		body = intent.getStringExtra(KEY_BODY);
	}

	@Override
	protected Fragment onCreatePane(Intent intent) {
		return ArticleFragment.newInstance(user, postDate, editDate, editCount, body);
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
				ActivityUtils.link(this, link);
				return true;
			case R.id.menu_share:
				String description = TextUtils.isEmpty(objectName) ?
					String.format(getString(R.string.share_thread_article_text), threadSubject, forumTitle) :
					String.format(getString(R.string.share_thread_article_object_text), threadSubject, forumTitle, objectName);
				ActivityUtils.share(this, getString(R.string.share_thread_subject), description + "\n\n" + link, R.string.title_share);
				String contentName = TextUtils.isEmpty(objectName) ?
					String.format("%s | %s", forumTitle, threadSubject) :
					String.format("%s | %s | %s", objectName, forumTitle, threadSubject);
				return true;
		}
		return super.onOptionsItemSelected(item);
	}
}
