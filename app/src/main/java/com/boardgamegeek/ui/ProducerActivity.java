package com.boardgamegeek.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.text.TextUtils;
import android.widget.Toast;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Artists;
import com.boardgamegeek.provider.BggContract.Designers;
import com.boardgamegeek.provider.BggContract.Publishers;
import com.boardgamegeek.tasks.SyncPublisherTask;
import com.boardgamegeek.util.PreferencesUtils;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.ContentViewEvent;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import timber.log.Timber;

public class ProducerActivity extends SimpleSinglePaneActivity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Uri uri = getIntent().getData();
		String contentType = getContentType(uri);
		if (TextUtils.isEmpty(contentType)) {
			Timber.w("Unexpected URI: %s", uri);
			finish();
		}

		final ActionBar actionBar = getSupportActionBar();
		if (actionBar != null) actionBar.setTitle(contentType);

		if (savedInstanceState == null) {
			Answers.getInstance().logContentView(new ContentViewEvent()
				.putContentType(contentType)
				.putContentId(uri.getLastPathSegment()));
		}
	}

	private String getContentType(Uri uri) {
		if (Designers.isDesignerUri(uri)) {
			return getString(R.string.title_designer);
		} else if (Artists.isArtistUri(uri)) {
			return getString(R.string.title_artist);
		} else if (Publishers.isPublisherUri(uri)) {
			return getString(R.string.title_publisher);
		}
		return null;
	}

	@Override
	protected Fragment onCreatePane(Intent intent) {
		return new ProducerFragment();
	}

	@SuppressWarnings("unused")
	@Subscribe(threadMode = ThreadMode.MAIN)
	public void onEvent(SyncPublisherTask.Event event) {
		if (!TextUtils.isEmpty(event.getErrorMessage()) && PreferencesUtils.getSyncShowErrors(this)) {
			Toast.makeText(this, event.getErrorMessage(), Toast.LENGTH_LONG).show();
		}
	}
}
