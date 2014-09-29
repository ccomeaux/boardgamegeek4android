package com.boardgamegeek.ui;

import android.content.Intent;
import android.support.v4.app.Fragment;

public class PlayStatsActivity extends SimpleSinglePaneActivity {

	@Override
	protected Fragment onCreatePane(Intent intent) {
		return new PlayStatsFragment();
	}
}
