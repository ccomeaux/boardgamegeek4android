package com.boardgamegeek.pref;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.util.TypedValue;

import com.boardgamegeek.R;
import com.boardgamegeek.export.JsonExportTask;
import com.boardgamegeek.util.TaskUtils;
import com.boardgamegeek.util.VersionUtils;

public class ExportDialogPreference extends DialogPreference {
	public ExportDialogPreference(Context context, AttributeSet attrs) {
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
		setDialogLayoutResource(R.layout.widget_dialogpreference_textview);
	}

	@Override
	public CharSequence getDialogMessage() {
		return getContext().getString(R.string.pref_advanced_export_message, JsonExportTask.getExportPath(false));
	}

	@Override
	protected void onDialogClosed(boolean positiveResult) {
		if (positiveResult) {
			JsonExportTask task = new JsonExportTask(getContext(), false);
			TaskUtils.executeAsyncTask(task);
			notifyChanged();
		}
	}
}
