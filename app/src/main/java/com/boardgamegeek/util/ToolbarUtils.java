package com.boardgamegeek.util;

import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.boardgamegeek.R;

/**
 * Helps creating and populating toolbars (and older action bars).
 */
public class ToolbarUtils {
	private ToolbarUtils() {
	}

	public static void setActionBarText(Menu menu, int id, String text) {
		setActionBarText(menu, id, text, null);
	}

	public static void setActionBarText(Menu menu, int id, String text1, String text2) {
		MenuItem item = menu.findItem(id);
		if (item != null) {
			View actionView = item.getActionView();
			if (actionView != null) {
				TextView tv1 = actionView.findViewById(android.R.id.text1);
				if (tv1 != null) {
					tv1.setText(text1);
				}
				TextView tv2 = actionView.findViewById(android.R.id.text2);
				if (tv2 != null) {
					tv2.setText(text2);
				}
			}
		}
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
