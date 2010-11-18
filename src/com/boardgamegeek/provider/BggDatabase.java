package com.boardgamegeek.provider;

import com.boardgamegeek.provider.BggContract.BuddiesColumns;
import com.boardgamegeek.provider.BggContract.GamesColumns;
import com.boardgamegeek.provider.BggContract.SyncColumns;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;
import android.util.Log;

public class BggDatabase extends SQLiteOpenHelper {
	private static final String TAG = "BggDatabase";

	private static final String DATABASE_NAME = "bgg.db";

	private static final int DATABASE_VERSION = 11;

	interface Tables {
		String GAMES = "games";
		String BUDDIES = "buddies";
	}

	public BggDatabase(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL("CREATE TABLE " + Tables.GAMES + " ("
			+ BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
			+ SyncColumns.UPDATED_LIST + " INTEGER NOT NULL,"
			+ SyncColumns.UPDATED_DETAIL + " INTEGER,"
			+ GamesColumns.GAME_ID + " INTEGER NOT NULL,"
			+ GamesColumns.GAME_NAME + " TEXT NOT NULL,"
			+ GamesColumns.GAME_NAME_2 + " TEXT,"
			+ GamesColumns.GAME_SORT_NAME + " TEXT NOT NULL,"
			+ GamesColumns.YEAR_PUBLISHED + " INTEGER,"
			+ GamesColumns.IMAGE_URL + " TEXT,"
			+ GamesColumns.THUMBNAIL_URL + " TEXT,"
			+ GamesColumns.MIN_PLAYERS + " INTEGER,"
			+ GamesColumns.MAX_PLAYERS + " INTEGER,"
			+ GamesColumns.PLAYING_TIME + " INTEGER,"
			+ GamesColumns.NUM_OWNED + " INTEGER,"
			+ GamesColumns.NUM_PLAYS + " INTEGER NOT NULL DEFAULT 0,"
			+ GamesColumns.PRIVATE_INFO_PRICE_PAID_CURRENCY + " TEXT,"
			+ GamesColumns.PRIVATE_INFO_PRICE_PAID + " REAL,"
			+ GamesColumns.PRIVATE_INFO_CURRENT_VALUE_CURRENCY + " TEXT,"
			+ GamesColumns.PRIVATE_INFO_CURRENT_VALUE + " REAL,"
			+ GamesColumns.PRIVATE_INFO_QUANTITY + " INTEGER,"
			+ GamesColumns.PRIVATE_INFO_ACQUISITION_DATE + " TEXT,"
			+ GamesColumns.PRIVATE_INFO_ACQUIRED_FROM + " TEXT,"
			+ GamesColumns.PRIVATE_INFO_COMMENT + " TEXT,"
			+ GamesColumns.MINIMUM_AGE + " INTEGER,"
			+ GamesColumns.DESCRIPTION + " TEXT,"
			+ GamesColumns.STATS_USERS_RATED + " INTEGER,"
			+ GamesColumns.STATS_AVERAGE + " REAL,"
			+ GamesColumns.STATS_BAYES_AVERAGE + " REAL,"
			+ GamesColumns.STATS_STANDARD_DEVIATION + " REAL,"
			+ GamesColumns.STATS_MEDIAN + " INT,"
			+ GamesColumns.STATS_NUMBER_OWNED + " INT,"
			+ GamesColumns.STATS_NUMBER_TRADING + " INT,"
			+ GamesColumns.STATS_NUMBER_WANTING + " INT,"
			+ GamesColumns.STATS_NUMBER_WISHING + " INT,"
			+ GamesColumns.STATS_NUMBER_COMMENTS + " INT,"
			+ GamesColumns.STATS_NUMBER_WEIGHTS + " INT,"
			+ GamesColumns.STATS_AVERAGE_WEIGHT + " REAL,"
			+ "UNIQUE (" + GamesColumns.GAME_ID + ") ON CONFLICT REPLACE)");
		
//		+ GamesColumns.COLLECTION_ID + " INTEGER,"
//		+ GamesColumns.STATUS_OWN + " INTEGER NOT NULL DEFAULT 0,"
//		+ GamesColumns.STATUS_PREVIOUSLY_OWNED + " INTEGER NOT NULL DEFAULT 0,"
//		+ GamesColumns.STATUS_FOR_TRADE + " INTEGER NOT NULL DEFAULT 0,"
//		+ GamesColumns.STATUS_WANT + " INTEGER NOT NULL DEFAULT 0,"
//		+ GamesColumns.STATUS_WANT_TO_PLAY + " INTEGER NOT NULL DEFAULT 0,"
//		+ GamesColumns.STATUS_WANT_TO_BUY + " INTEGER NOT NULL DEFAULT 0,"
//		+ GamesColumns.STATUS_WISHLIST + " INTEGER NOT NULL DEFAULT 0,"
//		+ GamesColumns.STATUS_PREORDERED + " INTEGER NOT NULL DEFAULT 0,"
//		+ GamesColumns.COMMENT + " TEXT,"
		
		db.execSQL("CREATE TABLE " + Tables.BUDDIES + " ("
			+ BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
			+ SyncColumns.UPDATED_LIST + " INTEGER NOT NULL,"
			+ SyncColumns.UPDATED_DETAIL + " INTEGER,"
			+ BuddiesColumns.BUDDY_ID + " INTEGER NOT NULL,"
			+ BuddiesColumns.BUDDY_NAME + " TEXT NOT NULL,"
			+ BuddiesColumns.BUDDY_FIRSTNAME + " TEXT,"
			+ BuddiesColumns.BUDDY_LASTNAME + " TEXT,"
			+ BuddiesColumns.AVATAR_URL + " TEXT,"
			+ "UNIQUE (" + BuddiesColumns.BUDDY_ID + ") ON CONFLICT REPLACE)");
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		Log.d(TAG, "onUpgrade() from " + oldVersion + " to " + newVersion);

		if (oldVersion != DATABASE_VERSION) {
			Log.w(TAG, "Destroying old data during upgrade");

			db.execSQL("DROP TABLE IF EXISTS " + Tables.GAMES);
			db.execSQL("DROP TABLE IF EXISTS " + Tables.BUDDIES);

			onCreate(db);
		}
	}
}
