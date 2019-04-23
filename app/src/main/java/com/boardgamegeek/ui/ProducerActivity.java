package com.boardgamegeek.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.ui.viewmodel.GameViewModel.ProducerType;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.ContentViewEvent;

import androidx.appcompat.app.ActionBar;
import androidx.fragment.app.Fragment;
import timber.log.Timber;

public class ProducerActivity extends SimpleSinglePaneActivity {
	private static final String KEY_TYPE = "TYPE";
	private static final String KEY_ID = "ID";
	private static final String KEY_TITLE = "TITLE";

	private ProducerType type;
	private int id;
	private String title;

	public static void start(Context context, ProducerType type, int id, String title) {
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
		type = (ProducerType) intent.getSerializableExtra(KEY_TYPE);
		id = intent.getIntExtra(KEY_ID, BggContract.INVALID_ID);
		title = intent.getStringExtra(KEY_TITLE);
	}

	private String getContentType(ProducerType type) {
		if (type == ProducerType.PUBLISHER) {
			return getString(R.string.title_publisher);
		}
		return "";
	}

	@Override
	protected Fragment onCreatePane(Intent intent) {
		return ProducerFragment.newInstance(type, id, title);
	}
}
