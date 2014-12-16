package com.boardgamegeek.ui;

import static com.boardgamegeek.util.LogUtils.LOGW;
import static com.boardgamegeek.util.LogUtils.makeLogTag;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Artists;
import com.boardgamegeek.provider.BggContract.Designers;
import com.boardgamegeek.provider.BggContract.Publishers;

public class ProducerActivity extends SimpleSinglePaneActivity {
	private static final String TAG = makeLogTag(ProducerActivity.class);

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Uri uri = getIntent().getData();

		if (Designers.isDesignerUri(uri)) {
			getSupportActionBar().setTitle(R.string.title_designer);
		} else if (Artists.isArtistUri(uri)) {
			getSupportActionBar().setTitle(R.string.title_artist);
		} else if (Publishers.isPublisherUri(uri)) {
			getSupportActionBar().setTitle(R.string.title_publisher);
		} else {
			LOGW(TAG, "Unexpected URI: " + uri);
			finish();
		}
	}

	@Override
	protected Fragment onCreatePane(Intent intent) {
		return new ProducerFragment();
	}

	@Override
	protected int getOptionsMenuId() {
		return R.menu.search_only;
	}
}
