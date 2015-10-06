package com.boardgamegeek.export;

import android.Manifest.permission;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.support.annotation.StringRes;
import android.support.v4.content.ContextCompat;

import com.boardgamegeek.R;
import com.boardgamegeek.events.ExportFinishedEvent;
import com.boardgamegeek.events.ExportProgressEvent;
import com.boardgamegeek.util.FileUtils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.stream.JsonWriter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import de.greenrobot.event.EventBus;
import timber.log.Timber;

public class JsonExportTask extends ImporterExporterTask {
	public JsonExportTask(Context context, boolean isAutoBackupMode) {
		super(context, isAutoBackupMode);
	}

	@Override
	protected Integer doInBackground(Void... params) {
		int permissionCheck = ContextCompat.checkSelfPermission(mContext, permission.WRITE_EXTERNAL_STORAGE);
		if (permissionCheck == PackageManager.PERMISSION_DENIED) {
			Timber.i("No permissions to write to external storage");
			return ERROR_STORAGE_ACCESS;
		}

		// Ensure external storage is available
		if (!FileUtils.isExtStorageAvailable()) {
			Timber.i("External storage is unavailable");
			return ERROR_STORAGE_ACCESS;
		}

		// Ensure the export directory exists
		File exportPath = FileUtils.getExportPath(mIsAutoBackupMode);
		if (!exportPath.exists()) {
			if (!exportPath.mkdirs()) {
				Timber.i("Export path %s can't be created", exportPath);
				return ERROR_STORAGE_ACCESS;
			}
		}

		for (Step exporter : mSteps) {
			int result = export(exportPath, exporter);
			if (result == ERROR || isCancelled()) {
				return ERROR;
			}
		}

		//TODO: auto-backup
		//		if (mIsAutoBackupMode) {
		//			// store current time = last backup time
		//			final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
		//			prefs.edit().putLong(KEY_LAST_BACKUP, System.currentTimeMillis()).commit();
		//		}

		return SUCCESS;
	}

	@Override
	protected void onProgressUpdate(Integer... values) {
		EventBus.getDefault().post(new ExportProgressEvent(values[0], values[1]));
	}

	@Override
	protected void onPostExecute(Integer result) {
		@StringRes int messageId;
		switch (result) {
			case SUCCESS:
				messageId = R.string.msg_export_success;
				break;
			case ERROR_STORAGE_ACCESS:
				messageId = R.string.msg_export_failed_nosd;
				break;
			default:
				messageId = R.string.msg_export_failed;
				break;
		}
		EventBus.getDefault().post(new ExportFinishedEvent(messageId));
	}

	private int export(File exportPath, Step step) {
		final Cursor cursor = step.getCursor(mContext);

		if (cursor == null) {
			return ERROR;
		}
		if (cursor.getCount() == 0) {
			cursor.close();
			return SUCCESS;
		}

		publishProgress(cursor.getCount(), 0);

		File backup = new File(exportPath, step.getFileName());
		try {
			OutputStream out = new FileOutputStream(backup);
			writeJsonStream(out, cursor, step);
		} catch (JsonIOException | IOException e) {
			Timber.e(e, "JSON export failed");
			return ERROR;
		} finally {
			cursor.close();
		}

		return SUCCESS;
	}

	private void writeJsonStream(OutputStream out, Cursor cursor, Step exporter) throws IOException {
		int numTotal = cursor.getCount();
		int numExported = 0;

		Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
		JsonWriter writer = new JsonWriter(new OutputStreamWriter(out, "UTF-8"));
		writer.setIndent("  ");
		writer.beginArray();

		while (cursor.moveToNext()) {
			if (isCancelled()) {
				break;
			}

			exporter.writeJsonRecord(mContext, cursor, gson, writer);
			publishProgress(numTotal, ++numExported);
		}

		writer.endArray();
		writer.close();
	}
}
