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

	protected Context mContext;
	protected abstract Task getTask();
	protected abstract int getInfoMessageResource();
	protected abstract int getConfirmMessageResource();

	public AsyncDialogPreference(Context context) {
		super(context, null);
		init(context);
	}

	public AsyncDialogPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}

	private void init(Context context) {
		mContext = context;
		setDialogIcon(android.R.drawable.ic_dialog_alert);
	}

	@Override
	protected View onCreateDialogView() {
		TextView tw = new TextView(mContext);
		tw.setText(getInfoMessageResource());
		int padding = (int) getContext().getResources().getDimension(R.dimen.padding_extra);
		tw.setPadding(padding, padding, padding, padding);
		return tw;
	}

	@Override
	protected void onDialogClosed(boolean positiveResult) {
		if (positiveResult) {
			getTask().execute();
		}
		notifyChanged();
	}
	
	protected abstract class Task extends AsyncTask<Void, Void, Void> {
		@Override
		protected void onPostExecute(Void result) {
			Toast.makeText(getContext(), getConfirmMessageResource(), Toast.LENGTH_LONG).show();
		}
	}
}
