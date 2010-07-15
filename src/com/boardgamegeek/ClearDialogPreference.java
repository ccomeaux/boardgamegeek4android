package com.boardgamegeek;

import com.boardgamegeek.BoardGameGeekData.*;

import android.content.Context;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

public class ClearDialogPreference extends DialogPreference {

	private Context context;

	public ClearDialogPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		this.context = context;
	}

	public ClearDialogPreference(Context context) {
		super(context, null);
	}

	@Override
	protected void onDialogClosed(boolean positiveResult) {
		if (positiveResult) {
			// delete all tables. NOTE: deleting boardgames will delete its
			// child tables too (including thumbnails)
			context.getContentResolver().delete(BoardGames.CONTENT_URI, null, null);
			context.getContentResolver().delete(Artists.CONTENT_URI, null, null);
			context.getContentResolver().delete(Designers.CONTENT_URI, null, null);
			context.getContentResolver().delete(Publishers.CONTENT_URI, null, null);
			context.getContentResolver().delete(Categories.CONTENT_URI, null, null);
			context.getContentResolver().delete(Mechanics.CONTENT_URI, null, null);
		}
	}

	@Override
	protected View onCreateDialogView() {
		TextView tw = new TextView(context);
		tw.setText(R.string.pref_database_clear_message);
		tw.setPadding(10, 8, 10, 8);
		return tw;
	}
}
