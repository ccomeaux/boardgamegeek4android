package com.boardgamegeek;

import android.content.ContentResolver;
import android.content.Context;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import com.boardgamegeek.provider.BggContract.Artists;
import com.boardgamegeek.provider.BggContract.Buddies;
import com.boardgamegeek.provider.BggContract.Categories;
import com.boardgamegeek.provider.BggContract.Designers;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.provider.BggContract.Mechanics;
import com.boardgamegeek.provider.BggContract.Publishers;
import com.boardgamegeek.util.ImageCache;

public class ClearDialogPreference extends DialogPreference {

	private Context context;

	public ClearDialogPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		this.context = context;
		setDialogIcon(android.R.drawable.ic_dialog_alert);
	}

	public ClearDialogPreference(Context context) {
		super(context, null);
	}

	@Override
	protected void onDialogClosed(boolean positiveResult) {
		if (positiveResult) {
			// delete all tables.
			// NOTE: deleting games will delete its child tables too
			ContentResolver cr = context.getContentResolver();
			cr.delete(Games.CONTENT_URI, null, null);
			cr.delete(Artists.CONTENT_URI, null, null);
			cr.delete(Designers.CONTENT_URI, null, null);
			cr.delete(Publishers.CONTENT_URI, null, null);
			cr.delete(Categories.CONTENT_URI, null, null);
			cr.delete(Mechanics.CONTENT_URI, null, null);
			cr.delete(Buddies.CONTENT_URI, null, null);
			// TODO: delete thumbnails
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
