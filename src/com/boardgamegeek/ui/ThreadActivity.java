package com.boardgamegeek.ui;

import java.util.ArrayList;
import java.util.List;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.TextUtils;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.model.ThreadArticle;
import com.boardgamegeek.util.ForumsUtils;
import com.boardgamegeek.util.HttpUtils;
import com.boardgamegeek.util.UIUtils;

public class ThreadActivity extends ListActivity {
	private final String TAG = "ThreadActivity";

	public static final String KEY_THREAD_ID = "THREAD_ID";
	public static final String KEY_GAME_ID = "GAME_ID";
	public static final String KEY_GAME_NAME = "GAME_NAME";
	public static final String KEY_THREAD_SUBJECT = "THREAD_SUBJECT";
	public static final String KEY_ARTICLES = "ARTICLES";

	private List<ThreadArticle> mArticles = new ArrayList<ThreadArticle>();

	private String mThreadId;
	private int mGameId;
	private String mGameName;
	private String mThreadSubject;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_thread);

		if (savedInstanceState == null) {
			final Intent intent = getIntent();
			mThreadId = intent.getExtras().getString(KEY_THREAD_ID);
			mGameId = intent.getExtras().getInt(KEY_GAME_ID);
			mGameName = intent.getExtras().getString(KEY_GAME_NAME);
			mThreadSubject = intent.getExtras().getString(KEY_THREAD_SUBJECT);
		} else {
			mThreadId = savedInstanceState.getString(KEY_THREAD_ID);
			mGameId = savedInstanceState.getInt(KEY_GAME_ID);
			mGameName = savedInstanceState.getString(KEY_GAME_NAME);
			mThreadSubject = savedInstanceState.getString(KEY_THREAD_SUBJECT);
			mArticles = savedInstanceState.getParcelableArrayList(KEY_ARTICLES);
		}

		setSubjecxt();

		if (TextUtils.isEmpty(mGameName)) {
			findViewById(R.id.game_thumbnail).setClickable(false);
			findViewById(R.id.thread_game_header).setVisibility(View.GONE);
			findViewById(R.id.thread_header_divider).setVisibility(View.GONE);

		} else {
			findViewById(R.id.thread_game_header).setVisibility(View.VISIBLE);
			findViewById(R.id.thread_header_divider).setVisibility(View.VISIBLE);
			UIUtils.setGameHeader(this, mGameName, mGameId);
		}

		if (mArticles == null || mArticles.size() == 0) {
			ForumsUtils.ThreadTask task = new ForumsUtils.ThreadTask(this, mArticles,
				HttpUtils.constructThreadUrl(mThreadId), mThreadSubject, TAG);
			task.execute();
		} else {
			setListAdapter(new ForumsUtils.ThreadAdapter(this, mArticles));
		}
	}

	private void setSubjecxt() {
		UIUtils.setTitle(this, mThreadSubject);
		ListView lv = (ListView) findViewById(android.R.id.list);
		View v = getLayoutInflater().inflate(R.layout.thread_header, null);
		TextView tv = (TextView) v.findViewById(R.id.thread_header);
		tv.setText(mThreadSubject);
		lv.addHeaderView(v);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString(KEY_THREAD_ID, mThreadId);
		outState.putString(KEY_THREAD_SUBJECT, mThreadSubject);
		outState.putParcelableArrayList(KEY_ARTICLES, (ArrayList<? extends Parcelable>) mArticles);
	}

	public void onHomeClick(View v) {
		UIUtils.goHome(this);
	}

	public void onSearchClick(View v) {
		onSearchRequested();
	}
}
