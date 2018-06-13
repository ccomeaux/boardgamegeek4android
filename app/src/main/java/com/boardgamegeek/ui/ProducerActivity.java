package com.boardgamegeek.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.text.TextUtils;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.ContentViewEvent;

import timber.log.Timber;

public class ProducerActivity extends SimpleSinglePaneActivity {
	private static final String KEY_TYPE = "TYPE";
	private static final String KEY_ID = "ID";
	private static final String KEY_TITLE = "TITLE";

	public static final int TYPE_UNKNOWN = 0;
	public static final int TYPE_DESIGNER = 1;
	public static final int TYPE_ARTIST = 2;
	public static final int TYPE_PUBLISHER = 3;

	private int type;
	private int id;
	private String title;

	public static void start(Context context, int type, int id, String title) {
		Intent starter = new Intent(context, ProducerActivity.class);
		starter.putExtra(KEY_TYPE, type);
		starter.putExtra(KEY_ID, id);
		starter.putExtra(KEY_TITLE, title);
		context.startActivity(starter);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		String contentType = getContentType(type);
		if (TextUtils.isEmpty(contentType)) {
			Timber.w("Unexpected type: %s", type);
			finish();
		}
		if (id == BggContract.INVALID_ID) {
			Timber.w("Invalid ID");
			finish();
		}

		final ActionBar actionBar = getSupportActionBar();
		if (actionBar != null) actionBar.setTitle(contentType);

		if (savedInstanceState == null) {
			ContentViewEvent event = new ContentViewEvent().putContentType(contentType);
			event.putContentId(String.valueOf(id));
			Answers.getInstance().logContentView(event);
		}
	}

	@Override
	protected void readIntent(Intent intent) {
		super.readIntent(intent);
		type = intent.getIntExtra(KEY_TYPE, TYPE_UNKNOWN);
		id = intent.getIntExtra(KEY_ID, BggContract.INVALID_ID);
		title = intent.getStringExtra(KEY_TITLE);
	}

	private String getContentType(int type) {
		switch (type) {
			case TYPE_DESIGNER:
				return getString(R.string.title_designer);
			case TYPE_ARTIST:
				return getString(R.string.title_artist);
			case TYPE_PUBLISHER:
				return getString(R.string.title_publisher);
			default:
				return "";
		}
	}

	@Override
	protected Fragment onCreatePane(Intent intent) {
		return ProducerFragment.newInstance(type, id, title);
	}
}
