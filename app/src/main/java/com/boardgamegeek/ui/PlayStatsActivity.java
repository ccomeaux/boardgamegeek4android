package com.boardgamegeek.ui;

import android.support.v4.app.Fragment;

public class PlayStatsActivity extends TopLevelSinglePaneActivity {

	@Override
	protected Fragment onCreatePane() {
		return new PlayStatsFragment();
	}
}
