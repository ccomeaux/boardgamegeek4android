package com.boardgamegeek.pref;

import android.content.ContentResolver;
import android.content.Context;
import android.os.AsyncTask;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.boardgamegeek.BggApplication;
import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Artists;
import com.boardgamegeek.provider.BggContract.Buddies;
import com.boardgamegeek.provider.BggContract.Categories;
import com.boardgamegeek.provider.BggContract.Designers;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.provider.BggContract.Mechanics;
import com.boardgamegeek.provider.BggContract.Publishers;
import com.boardgamegeek.util.ImageCache;

public class ClearDialogPreference extends DialogPreference {
	private static final String TAG = "ClearDialogPreference";

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
			new ClearTask().execute("");
		}
		notifyChanged();
	}

	private class ClearTask extends AsyncTask<String, Void, Void> {

		private ContentResolver mResolver;

		@Override
		protected void onPreExecute() {
			mResolver = context.getContentResolver();
		}

		@Override
		protected Void doInBackground(String... params) {
			BggApplication.getInstance().putCollectionFullSyncTimestamp(0);
			BggApplication.getInstance().putCollectionPartSyncTimestamp(0);

			int count = 0;
			count += mResolver.delete(Games.CONTENT_URI, null, null); 
			count += mResolver.delete(Artists.CONTENT_URI, null, null);
			count += mResolver.delete(Designers.CONTENT_URI, null, null);
			count += mResolver.delete(Publishers.CONTENT_URI, null, null);
			count += mResolver.delete(Categories.CONTENT_URI, null, null);
			count += mResolver.delete(Mechanics.CONTENT_URI, null, null);
			count += mResolver.delete(Buddies.CONTENT_URI, null, null);
			Log.d(TAG, "Removed " + count + " records");

			if (ImageCache.clear()) {
				Log.d(TAG, "Cleared image cache");
			} else {
				Log.d(TAG, "Unable to clear image cache (expected)");
			}

			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			Toast.makeText(getContext(), "Clear complete.", Toast.LENGTH_SHORT).show();
		}
	}

	@Override
	protected View onCreateDialogView() {
		TextView tw = new TextView(context);
		tw.setText(R.string.pref_database_clear_message);
		int padding = (int) getContext().getResources().getDimension(R.dimen.padding_extra);
		tw.setPadding(padding, padding, padding, padding);
		return tw;
	}
}
