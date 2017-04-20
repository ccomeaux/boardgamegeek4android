package com.boardgamegeek.export;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import com.boardgamegeek.export.model.User;
import com.boardgamegeek.provider.BggContract.Buddies;
import com.google.gson.Gson;
import com.google.gson.stream.JsonWriter;

public class UserExportTask extends JsonExportTask<User> {
	public UserExportTask(Context context, Uri uri) {
		super(context, Constants.TYPE_USERS, uri);
	}

	@Override
	protected int getVersion() {
		return 1;
	}

	@Override
	protected Cursor getCursor(Context context) {
		return context.getContentResolver().query(
			Buddies.CONTENT_URI,
			User.PROJECTION,
			null, null, null);
	}

	@Override
	protected void writeJsonRecord(Context context, Cursor cursor, Gson gson, JsonWriter writer) {
		User user = User.fromCursor(cursor);
		user.addColors(context);
		if (user.hasColors()) {
			gson.toJson(user, User.class, writer);
		}
	}
}
