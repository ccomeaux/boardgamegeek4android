package com.boardgamegeek.export;

import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Environment;
import android.widget.Toast;

import com.boardgamegeek.R;
import com.boardgamegeek.events.ExportFinishedEvent;
import com.boardgamegeek.events.ExportProgressEvent;
import com.boardgamegeek.export.model.CollectionView;
import com.boardgamegeek.provider.BggContract.CollectionViews;
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

public class JsonExportTask extends AsyncTask<Void, Integer, Integer> {
	private static final String EXPORT_FOLDER = "bgg4android";
	private static final String EXPORT_FOLDER_AUTO = "bgg4android" + File.separator + "AutoBackup";
	public static final String EXPORT_JSON_FILE_COLLECTION_VIEWS = "export-collectionviews.json";
	private static final int SUCCESS = 1;
	private static final int ERROR = 0;
	private static final int ERROR_STORAGE_ACCESS = -1;

	private final Context mContext;
	private final boolean mIsAutoBackupMode;

	public static File getExportPath(boolean isAutoBackupMode) {
		return new File(
			Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
			isAutoBackupMode ? EXPORT_FOLDER_AUTO : EXPORT_FOLDER);
	}

	public JsonExportTask(Context context, boolean isAutoBackupMode) {
		mContext = context.getApplicationContext();
		mIsAutoBackupMode = isAutoBackupMode;
	}

	@Override
	protected Integer doInBackground(Void... params) {
		// Ensure external storage is available
		if (!FileUtils.isExtStorageAvailable()) {
			return ERROR_STORAGE_ACCESS;
		}

		// Ensure the export directory exists
		File exportPath = getExportPath(mIsAutoBackupMode);
		exportPath.mkdirs();

		int result = exportCollectionViews(exportPath);
		if (result == ERROR || isCancelled()) {
			return ERROR;
		}

//		if (mIsAutoBackupMode) {
//			// store current time = last backup time
//			final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
//			prefs.edit().putLong(KEY_LASTBACKUP, System.currentTimeMillis()).commit();
//		}

		return SUCCESS;
	}

	@Override
	protected void onProgressUpdate(Integer... values) {
		EventBus.getDefault().post(new ExportProgressEvent(values[0], values[1]));
	}

	@Override
	protected void onPostExecute(Integer result) {
		if (!mIsAutoBackupMode) {
			int messageId;
			switch (result) {
				case SUCCESS:
					messageId = R.string.pref_advanced_export_msg_success;
					break;
				case ERROR_STORAGE_ACCESS:
					messageId = R.string.pref_advanced_export_msg_failed_nosd;
					break;
				default:
					messageId = R.string.pref_advanced_export_msg_failed;
					break;
			}
			Toast.makeText(mContext, messageId, Toast.LENGTH_LONG).show();
		}

		EventBus.getDefault().post(new ExportFinishedEvent());
	}

	private int exportCollectionViews(File exportPath) {
		final Cursor views = mContext.getContentResolver().query(
			CollectionViews.CONTENT_URI,
			CollectionView.PROJECTION,
			null, null, null);

		if (views == null) {
			return ERROR;
		}
		if (views.getCount() == 0) {
			views.close();
			return SUCCESS;
		}

		publishProgress(views.getCount(), 0);

		File backup = new File(exportPath, EXPORT_JSON_FILE_COLLECTION_VIEWS);
		try {
			OutputStream out = new FileOutputStream(backup);

			writeJsonStreamShows(out, views);
		} catch (JsonIOException | IOException e) {
			Timber.e(e, "JSON collection views export failed");
			return ERROR;
		} finally {
			views.close();
		}

		return SUCCESS;
	}

	private void writeJsonStreamShows(OutputStream out, Cursor cursor) throws IOException {
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

			CollectionView cv = CollectionView.fromCursor(cursor);
			cv.addFilters(mContext);

			gson.toJson(cv, CollectionView.class, writer);
			publishProgress(numTotal, ++numExported);
		}

		writer.endArray();
		writer.close();
	}
}
