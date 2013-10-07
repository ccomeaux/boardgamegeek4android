package com.boardgamegeek.pref;

import android.content.Context;
import android.os.AsyncTask;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.boardgamegeek.R;

public abstract class AsyncDialogPreference extends DialogPreference {

	protected abstract Task getTask();

	protected abstract int getInfoMessageResource();

	protected abstract int getSuccessMessageResource();

	protected abstract int getFailureMessageResource();

	public AsyncDialogPreference(Context context) {
		super(context, null);
		init(context);
	}

	public AsyncDialogPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}

	// @TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private void init(Context context) {
		// This caused the PreferencesActivity to crash on 4.2.1 (not sure why)
		// if (VersionUtils.hasHoneycomb()) {
		// setDialogIcon(android.R.attr.alertDialogIcon);
		// } else {
		setDialogIcon(android.R.drawable.ic_dialog_alert);
		// }
	}

	@Override
	protected View onCreateDialogView() {
		TextView tw = new TextView(getContext());
		tw.setText(getInfoMessageResource());
		int padding = (int) getContext().getResources().getDimension(R.dimen.padding_extra);
		tw.setPadding(padding, padding, padding, padding);
		return tw;
	}

	@Override
	protected void onDialogClosed(boolean positiveResult) {
		if (positiveResult) {
			getTask().execute();
			notifyChanged();
		}
	}

	protected abstract class Task extends AsyncTask<Void, Void, Boolean> {
		@Override
		protected void onPostExecute(Boolean result) {
			Toast.makeText(getContext(), result ? getSuccessMessageResource() : getFailureMessageResource(),
				Toast.LENGTH_LONG).show();
		}
	}
}
