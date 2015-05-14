package com.boardgamegeek.export;

import android.content.Context;
import android.database.Cursor;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

public abstract class Exporter {

	public abstract String getFileName();

	public abstract Cursor getCursor(Context context);

	public abstract void writeJsonRecord(Context context, Cursor cursor, Gson gson, JsonWriter writer);

	public abstract void initializeImport(Context context);

	public abstract void importRecord(Context context, Gson gson, JsonReader reader);
}
