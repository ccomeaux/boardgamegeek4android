package com.boardgamegeek.export;

import android.content.Context;
import android.database.Cursor;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

public interface Step {
	String PREFERENCE_KEY_PREFIX = "key_export_";

	String getFileName();

	String getPreferenceKey();

	String getDescription(Context context);

	Cursor getCursor(Context context);

	void writeJsonRecord(Context context, Cursor cursor, Gson gson, JsonWriter writer);

	void initializeImport(Context context);

	void importRecord(Context context, Gson gson, JsonReader reader);
}
