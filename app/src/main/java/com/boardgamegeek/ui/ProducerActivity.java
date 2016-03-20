package com.boardgamegeek.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Artists;
import com.boardgamegeek.provider.BggContract.Designers;
import com.boardgamegeek.provider.BggContract.Publishers;

import timber.log.Timber;

public class ProducerActivity extends SimpleSinglePaneActivity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Uri uri = getIntent().getData();

		final ActionBar actionBar = getSupportActionBar();
		if (actionBar != null) {
			if (Designers.isDesignerUri(uri)) {
				actionBar.setTitle(R.string.title_designer);
			} else if (Artists.isArtistUri(uri)) {
				actionBar.setTitle(R.string.title_artist);
			} else if (Publishers.isPublisherUri(uri)) {
				actionBar.setTitle(R.string.title_publisher);
			} else {
				Timber.w("Unexpected URI: " + uri);
				finish();
			}
		}
	}

	@Override
	protected Fragment onCreatePane(Intent intent) {
		return new ProducerFragment();
	}
}
