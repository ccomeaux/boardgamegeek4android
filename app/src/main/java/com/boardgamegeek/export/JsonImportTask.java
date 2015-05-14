package com.boardgamegeek.export;

import android.content.Context;
import android.os.AsyncTask;
import android.widget.Toast;

import com.boardgamegeek.R;
import com.boardgamegeek.events.ImportFinishedEvent;
import com.boardgamegeek.events.ImportProgressEvent;
import com.boardgamegeek.util.FileUtils;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.stream.JsonReader;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import de.greenrobot.event.EventBus;
import timber.log.Timber;

public class JsonImportTask extends AsyncTask<Void, Integer, Integer> {
	private static final int ERROR_FILE_ACCESS = 1;
	private static final int SUCCESS = 0;
	private static final int ERROR = -1;
	private static final int ERROR_STORAGE_ACCESS = -2;
	private final Context mContext;
	private final boolean mIsAutoBackupMode;

	public JsonImportTask(Context context, boolean isAutoBackupMode) {
		mContext = context.getApplicationContext();
		mIsAutoBackupMode = isAutoBackupMode;
	}

	@Override
	protected Integer doInBackground(Void... params) {
		// Ensure external storage
		if (!FileUtils.isExtStorageAvailable()) {
			return ERROR_STORAGE_ACCESS;
		}

		// TODO: Ensure no large database ops are running?

		// Ensure the export directory exists
		File importPath = FileUtils.getExportPath(mIsAutoBackupMode);
		if (!importPath.exists()) {
			return ERROR_STORAGE_ACCESS;
		}

		List<ImporterExporter> importers = new ArrayList<>();
		importers.add(new CollectionViewImporterExporter());
		importers.add(new GameImporterExporter());

		for (ImporterExporter importer : importers) {
			int result = importFile(importPath, importer);
			if (result == ERROR || isCancelled()) {
				return ERROR;
			} else if (result < ERROR) {
				return result;
			}
		}

		return SUCCESS;
	}

	@Override
	protected void onProgressUpdate(Integer... values) {
		EventBus.getDefault().post(new ImportProgressEvent(values[0], values[1]));
	}

	@Override
	protected void onPostExecute(Integer result) {
		int messageId;
		switch (result) {
			case SUCCESS:
				messageId = R.string.pref_advanced_import_msg_success;
				break;
			case ERROR_STORAGE_ACCESS:
				messageId = R.string.pref_advanced_import_msg_failed_nosd;
				break;
			case ERROR_FILE_ACCESS:
				messageId = R.string.pref_advanced_import_msg_failed_nofile;
				break;
			default:
				messageId = R.string.pref_advanced_import_msg_failed;
				break;
		}
		Toast.makeText(mContext, messageId, Toast.LENGTH_LONG).show();
		EventBus.getDefault().post(new ImportFinishedEvent());
	}

	private int importFile(File importPath, ImporterExporter importer) {
		File file = new File(importPath, importer.getFileName());
		if (!file.exists() || !file.canRead()) {
			return ERROR_FILE_ACCESS;
		}

		importer.initializeImport(mContext);

		try {
			InputStream in = new FileInputStream(file);

			Gson gson = new Gson();

			JsonReader reader = new JsonReader(new InputStreamReader(in, "UTF-8"));
			reader.beginArray();

			while (reader.hasNext()) {
				importer.importRecord(mContext, gson, reader);
			}

			reader.endArray();
			reader.close();
		} catch (JsonParseException | IOException e) {
			// the given Json might not be valid or unreadable
			Timber.e(e, "JSON show import failed");
			return ERROR;
		}

		return SUCCESS;
	}
}
