package com.boardgamegeek.provider;

import com.boardgamegeek.provider.BggContract.BuddiesColumns;
import com.boardgamegeek.provider.BggContract.SyncColumns;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;
import android.util.Log;

public class BggDatabase extends SQLiteOpenHelper {
	private static final String TAG = "BggDatabase";

	private static final String DATABASE_NAME = "bgg.db";

	private static final int DATABASE_VERSION = 1;

	interface Tables {
		String BUDDIES = "buddies";
	}

	public BggDatabase(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL("CREATE TABLE " + Tables.BUDDIES + " ("
			+ BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
			+ SyncColumns.UPDATED + " INTEGER NOT NULL,"
			+ BuddiesColumns.BUDDY_ID + " INTEGER NOT NULL,"
			+ BuddiesColumns.BUDDY_NAME + " TEXT NOT NULL,"
			+ "UNIQUE (" + BuddiesColumns.BUDDY_ID + ") ON CONFLICT REPLACE)");
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		Log.d(TAG, "onUpgrade() from " + oldVersion + " to " + newVersion);

		if (oldVersion != DATABASE_VERSION) {
			Log.w(TAG, "Destroying old data during upgrade");

			db.execSQL("DROP TABLE IF EXISTS " + Tables.BUDDIES);

			onCreate(db);
		}
	}
}
