package com.boardgamegeek.pref;

import android.content.Context;
import android.util.AttributeSet;

import com.boardgamegeek.R;
import com.boardgamegeek.export.JsonImportTask;
import com.boardgamegeek.util.FileUtils;
import com.boardgamegeek.util.TaskUtils;

public class ImportDialogPreference extends ConfirmDialogPreference {
	public ImportDialogPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	protected void execute() {
		TaskUtils.executeAsyncTask(new JsonImportTask(getContext(), false));
	}

	@Override
	public CharSequence getDialogMessage() {
		return getContext().getString(R.string.pref_advanced_import_message, FileUtils.getExportPath(false));
	}
}
