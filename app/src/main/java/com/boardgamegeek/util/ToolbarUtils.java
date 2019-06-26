package com.boardgamegeek.util;

import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.boardgamegeek.R;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

/**
 * Helps creating and populating toolbars (and older action bars).
 */
public class ToolbarUtils {
	private ToolbarUtils() {
	}

	public static void setDoneCancelActionBarView(AppCompatActivity activity, View.OnClickListener listener) {
		Toolbar toolbar = activity.findViewById(R.id.toolbar_done_cancel);
		if (toolbar == null) return;
		toolbar.setContentInsetsAbsolute(0, 0);
		View cancelActionView = toolbar.findViewById(R.id.menu_cancel);
		cancelActionView.setOnClickListener(listener);
		View doneActionView = toolbar.findViewById(R.id.menu_done);
		doneActionView.setOnClickListener(listener);
		activity.setSupportActionBar(toolbar);
	}
}
