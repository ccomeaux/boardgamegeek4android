package com.boardgamegeek.pref;

import android.content.Context;
import android.util.AttributeSet;

import com.boardgamegeek.R;
import com.boardgamegeek.export.JsonExportTask;
import com.boardgamegeek.util.FileUtils;
import com.boardgamegeek.util.TaskUtils;

public class ExportDialogPreference extends ConfirmDialogPreference {
	public ExportDialogPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	protected void execute() {
		TaskUtils.executeAsyncTask(new JsonExportTask(getContext(), false));
	}

	@Override
	public CharSequence getDialogMessage() {
		return getContext().getString(R.string.pref_advanced_export_message, FileUtils.getExportPath(false));
	}
}
