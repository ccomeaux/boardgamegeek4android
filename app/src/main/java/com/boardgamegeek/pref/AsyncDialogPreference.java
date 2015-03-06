package com.boardgamegeek.pref;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.widget.Toast;

import com.boardgamegeek.R;
import com.boardgamegeek.util.VersionUtils;

public abstract class AsyncDialogPreference extends DialogPreference {

	protected abstract Task getTask();

	protected abstract int getSuccessMessageResource();

	protected abstract int getFailureMessageResource();

	public AsyncDialogPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private void init() {
		int dialogResourceId = android.R.drawable.ic_dialog_alert;
		if (VersionUtils.hasHoneycomb()) {
			TypedValue typedValue = new TypedValue();
			getContext().getTheme().resolveAttribute(android.R.attr.alertDialogIcon, typedValue, true);
			dialogResourceId = typedValue.resourceId;
		}
		setDialogIcon(dialogResourceId);
		setDialogLayoutResource(R.layout.textview_dialogpreference);
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
