package com.boardgamegeek.provider;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;
import android.util.Log;

import com.boardgamegeek.provider.BggContract.Artists;
import com.boardgamegeek.provider.BggContract.BuddiesColumns;
import com.boardgamegeek.provider.BggContract.Categories;
import com.boardgamegeek.provider.BggContract.Collection;
import com.boardgamegeek.provider.BggContract.CollectionColumns;
import com.boardgamegeek.provider.BggContract.Designers;
import com.boardgamegeek.provider.BggContract.GamePollResults;
import com.boardgamegeek.provider.BggContract.GamePollResultsColumns;
import com.boardgamegeek.provider.BggContract.GamePollResultsResult;
import com.boardgamegeek.provider.BggContract.GamePollResultsResultColumns;
import com.boardgamegeek.provider.BggContract.GamePolls;
import com.boardgamegeek.provider.BggContract.GamePollsColumns;
import com.boardgamegeek.provider.BggContract.GameRanks;
import com.boardgamegeek.provider.BggContract.GameRanksColumns;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.provider.BggContract.GamesColumns;
import com.boardgamegeek.provider.BggContract.Mechanics;
import com.boardgamegeek.provider.BggContract.Publishers;
import com.boardgamegeek.provider.BggContract.SyncColumns;
import com.boardgamegeek.provider.BggContract.SyncListColumns;
import com.boardgamegeek.util.CreateTableBuilder;
import com.boardgamegeek.util.CreateTableBuilder.COLUMN_TYPE;

public class BggDatabase extends SQLiteOpenHelper {
	private static final String TAG = "BggDatabase";

	private static final String DATABASE_NAME = "bgg.db";

	// NOTE: carefully update onUpgrade() when bumping database versions to make
	// sure user data is saved.
	private static final int VER_INITIAL = 1;
	private static final int VER_WISHLIST_PRIORITY = 2;
	private static final int DATABASE_VERSION = VER_WISHLIST_PRIORITY;

	public interface GamesDesigners {
		String GAME_ID = Games.GAME_ID;
		String DESIGNER_ID = Designers.DESIGNER_ID;
	}

	public interface GamesArtists {
		String GAME_ID = Games.GAME_ID;
		String ARTIST_ID = Artists.ARTIST_ID;
	}

	public interface GamesPublishers {
		String GAME_ID = Games.GAME_ID;
		String PUBLISHER_ID = Publishers.PUBLISHER_ID;
	}

	public interface GamesMechanics {
		String GAME_ID = Games.GAME_ID;
		String MECHANIC_ID = Mechanics.MECHANIC_ID;
	}

	public interface GamesCategories {
		String GAME_ID = Games.GAME_ID;
		String CATEGORY_ID = Categories.CATEGORY_ID;
	}

	interface Tables {
		String DESIGNERS = "designers";
		String ARTISTS = "artists";
		String PUBLISHERS = "publishers";
		String MECHANICS = "mechanics";
		String CATEGORIES = "categories";
		String GAMES = "games";
		String GAME_RANKS = "game_ranks";
		String GAMES_DESIGNERS = "games_designers";
		String GAMES_ARTISTS = "games_artists";
		String GAMES_PUBLISHERS = "games_publishers";
		String GAMES_MECHANICS = "games_mechanics";
		String GAMES_CATEGORIES = "games_categories";
		String COLLECTION = "collection";
		String BUDDIES = "buddies";
		String GAME_POLLS = "game_polls";
		String GAME_POLL_RESULTS = "game_poll_results";
		String GAME_POLL_RESULTS_RESULT = "game_poll_results_result";

		String GAMES_DESIGNERS_JOIN_DESIGNERS = createJoin(GAMES_DESIGNERS, DESIGNERS, Designers.DESIGNER_ID);
		String GAMES_ARTISTS_JOIN_ARTISTS = createJoin(GAMES_ARTISTS, ARTISTS, Artists.ARTIST_ID);
		String GAMES_PUBLISHERS_JOIN_PUBLISHERS = createJoin(GAMES_PUBLISHERS, PUBLISHERS, Publishers.PUBLISHER_ID);
		String GAMES_MECHANICS_JOIN_MECHANICS = createJoin(GAMES_MECHANICS, MECHANICS, Mechanics.MECHANIC_ID);
		String GAMES_CATEGORIES_JOIN_CATEGORIES = createJoin(GAMES_CATEGORIES, CATEGORIES, Categories.CATEGORY_ID);
		String POLLS_JOIN_POLL_RESULTS = createJoin(GAME_POLLS, GAME_POLL_RESULTS, GamePolls._ID, GamePollResults.POLL_ID);
		String POLL_RESULTS_JOIN_POLL_RESULTS_RESULT = createJoin(GAME_POLL_RESULTS, GAME_POLL_RESULTS_RESULT, GamePollResults._ID, GamePollResultsResult.POLL_RESULTS_ID);
		String COLLECTION_JOIN_GAMES =createJoin(COLLECTION, GAMES, Collection.GAME_ID);
	}

	private static String createJoin(String table1, String table2, String column) {
		return table1 + " LEFT OUTER JOIN " + table2 + " ON " + table1 + "." + column + "=" + table2 + "." + column;
	}

	private static String createJoin(String table1, String table2, String column1, String column2) {
		return table1 + " LEFT OUTER JOIN " + table2 + " ON " + table1 + "." + column1 + "=" + table2 + "." + column2;
	}

	private interface References {
		String GAME_ID = "REFERENCES " + Tables.GAMES + "(" + Games.GAME_ID + ")";
		String DESIGNER_ID = "REFERENCES " + Tables.DESIGNERS + "(" + Designers.DESIGNER_ID + ")";
		String ARTIST_ID = "REFERENCES " + Tables.ARTISTS + "(" + Artists.ARTIST_ID + ")";
	}

	public BggDatabase(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		CreateTableBuilder builder = new CreateTableBuilder();

		db.execSQL("CREATE TABLE " + Tables.DESIGNERS + " ("
			+ BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
			+ SyncColumns.UPDATED + " INTEGER,"
			+ Designers.DESIGNER_ID + " INTEGER NOT NULL,"
			+ Designers.DESIGNER_NAME + " TEXT NOT NULL,"
			+ Designers.DESIGNER_DESCRIPTION + " TEXT,"
			+ "UNIQUE (" + Designers.DESIGNER_ID + ") ON CONFLICT IGNORE)");

		db.execSQL("CREATE TABLE " + Tables.ARTISTS + " ("
			+ BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
			+ SyncColumns.UPDATED + " INTEGER,"
			+ Artists.ARTIST_ID + " INTEGER NOT NULL,"
			+ Artists.ARTIST_NAME + " TEXT NOT NULL,"
			+ Artists.ARTIST_DESCRIPTION + " TEXT,"
			+ "UNIQUE (" + Artists.ARTIST_ID + ") ON CONFLICT IGNORE)");

		builder.reset().table(Tables.PUBLISHERS).defaultPrimaryKey()
			.column(SyncColumns.UPDATED, COLUMN_TYPE.INTEGER)
			.column(Publishers.PUBLISHER_ID, COLUMN_TYPE.INTEGER, true, true)
			.column(Publishers.PUBLISHER_NAME, COLUMN_TYPE.TEXT, true)
			.column(Publishers.PUBLISHER_DESCRIPTION, COLUMN_TYPE.TEXT)
			.create(db);

		builder.reset().table(Tables.MECHANICS).defaultPrimaryKey()
			.column(Mechanics.MECHANIC_ID, COLUMN_TYPE.INTEGER, true, true)
			.column(Mechanics.MECHANIC_NAME, COLUMN_TYPE.TEXT, true)
			.create(db);

		builder.reset().table(Tables.CATEGORIES).defaultPrimaryKey()
			.column(Categories.CATEGORY_ID, COLUMN_TYPE.INTEGER, true, true)
			.column(Categories.CATEGORY_NAME, COLUMN_TYPE.TEXT, true)
			.create(db);

		db.execSQL("CREATE TABLE " + Tables.GAMES + " ("
			+ BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
			+ SyncColumns.UPDATED + " INTEGER,"
			+ SyncListColumns.UPDATED_LIST + " INTEGER NOT NULL,"
			+ GamesColumns.GAME_ID + " INTEGER NOT NULL,"
			+ GamesColumns.GAME_NAME + " TEXT NOT NULL,"
			+ GamesColumns.GAME_SORT_NAME + " TEXT NOT NULL,"
			+ GamesColumns.YEAR_PUBLISHED + " INTEGER,"
			+ GamesColumns.IMAGE_URL + " TEXT,"
			+ GamesColumns.THUMBNAIL_URL + " TEXT,"
			+ GamesColumns.MIN_PLAYERS + " INTEGER,"
			+ GamesColumns.MAX_PLAYERS + " INTEGER,"
			+ GamesColumns.PLAYING_TIME + " INTEGER,"
			+ GamesColumns.NUM_OWNED + " INTEGER,"
			+ GamesColumns.NUM_PLAYS + " INTEGER NOT NULL DEFAULT 0,"
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

		db.execSQL("CREATE TABLE " + Tables.GAME_RANKS + " ("
			+ BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
			+ GameRanks.GAME_ID + " INTEGER NOT NULL " + References.GAME_ID + ","
			+ GameRanksColumns.GAME_RANK_ID + " INTEGER NOT NULL,"
			+ GameRanksColumns.GAME_RANK_TYPE + " TEXT NOT NULL,"
			+ GameRanksColumns.GAME_RANK_NAME + " TEXT NOT NULL,"
			+ GameRanksColumns.GAME_RANK_FRIENDLY_NAME + " TEXT NOT NULL,"
			+ GameRanksColumns.GAME_RANK_VALUE + " INTEGER NOT NULL,"
			+ GameRanksColumns.GAME_RANK_BAYES_AVERAGE + " REAL,"
			+ "UNIQUE (" + GameRanksColumns.GAME_RANK_ID + "," + GameRanks.GAME_ID + ") ON CONFLICT REPLACE)");

		db.execSQL("CREATE TABLE " + Tables.GAMES_DESIGNERS + " ("
			+ BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
			+ GamesDesigners.GAME_ID + " INTEGER NOT NULL " + References.GAME_ID + ","
			+ GamesDesigners.DESIGNER_ID + " INTEGER NOT NULL " + References.DESIGNER_ID + ","
			+ "UNIQUE (" + GamesDesigners.GAME_ID + "," + GamesDesigners.DESIGNER_ID + ") ON CONFLICT IGNORE)");

		db.execSQL("CREATE TABLE " + Tables.GAMES_ARTISTS + " ("
			+ BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
			+ GamesArtists.GAME_ID + " INTEGER NOT NULL " + References.GAME_ID + ","
			+ GamesArtists.ARTIST_ID + " INTEGER NOT NULL " + References.ARTIST_ID + ","
			+ "UNIQUE (" + GamesArtists.GAME_ID + "," + GamesArtists.ARTIST_ID + ") ON CONFLICT IGNORE)");

		builder.reset().table(Tables.GAMES_PUBLISHERS).defaultPrimaryKey()
			.column(GamesPublishers.GAME_ID, COLUMN_TYPE.INTEGER, true, true, Tables.GAMES, Games.GAME_ID)
			.column(GamesPublishers.PUBLISHER_ID, COLUMN_TYPE.INTEGER, true, true, Tables.PUBLISHERS, Publishers.PUBLISHER_ID)
			.create(db);

		builder.reset().table(Tables.GAMES_MECHANICS).defaultPrimaryKey()
			.column(GamesMechanics.GAME_ID, COLUMN_TYPE.INTEGER, true, true, Tables.GAMES, Games.GAME_ID)
			.column(GamesMechanics.MECHANIC_ID, COLUMN_TYPE.INTEGER, true, true, Tables.MECHANICS, Mechanics.MECHANIC_ID)
			.create(db);

		builder.reset().table(Tables.GAMES_CATEGORIES).defaultPrimaryKey()
			.column(GamesCategories.GAME_ID, COLUMN_TYPE.INTEGER, true, true, Tables.GAMES, Games.GAME_ID)
			.column(GamesCategories.CATEGORY_ID, COLUMN_TYPE.INTEGER, true, true, Tables.CATEGORIES, Categories.CATEGORY_ID)
			.create(db);

		db.execSQL("CREATE TABLE " + Tables.COLLECTION + " ("
			+ BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
			+ SyncColumns.UPDATED + " INTEGER,"
			+ SyncListColumns.UPDATED_LIST + " INTEGER NOT NULL,"
			+ Collection.GAME_ID + " TEXT " + References.GAME_ID + ","
			+ CollectionColumns.COLLECTION_ID + " INTEGER NOT NULL,"
			+ CollectionColumns.COLLECTION_NAME + " TEXT NOT NULL,"
			+ CollectionColumns.COLLECTION_SORT_NAME + " TEXT NOT NULL,"
			+ CollectionColumns.STATUS_OWN + " INTEGER NOT NULL DEFAULT 0,"
			+ CollectionColumns.STATUS_PREVIOUSLY_OWNED + " INTEGER NOT NULL DEFAULT 0,"
			+ CollectionColumns.STATUS_FOR_TRADE + " INTEGER NOT NULL DEFAULT 0,"
			+ CollectionColumns.STATUS_WANT + " INTEGER NOT NULL DEFAULT 0,"
			+ CollectionColumns.STATUS_WANT_TO_PLAY + " INTEGER NOT NULL DEFAULT 0,"
			+ CollectionColumns.STATUS_WANT_TO_BUY + " INTEGER NOT NULL DEFAULT 0,"
			+ CollectionColumns.STATUS_WISHLIST_PRIORITY + " INTEGER,"
			+ CollectionColumns.STATUS_WISHLIST + " INTEGER NOT NULL DEFAULT 0,"
			+ CollectionColumns.STATUS_PREORDERED + " INTEGER NOT NULL DEFAULT 0,"
			+ CollectionColumns.COMMENT + " TEXT,"
			+ CollectionColumns.PRIVATE_INFO_PRICE_PAID_CURRENCY + " TEXT,"
			+ CollectionColumns.PRIVATE_INFO_PRICE_PAID + " REAL,"
			+ CollectionColumns.PRIVATE_INFO_CURRENT_VALUE_CURRENCY + " TEXT,"
			+ CollectionColumns.PRIVATE_INFO_CURRENT_VALUE + " REAL,"
			+ CollectionColumns.PRIVATE_INFO_QUANTITY + " INTEGER,"
			+ CollectionColumns.PRIVATE_INFO_ACQUISITION_DATE + " TEXT,"
			+ CollectionColumns.PRIVATE_INFO_ACQUIRED_FROM + " TEXT,"
			+ CollectionColumns.PRIVATE_INFO_COMMENT + " TEXT,"
			+ "UNIQUE (" + CollectionColumns.COLLECTION_ID + ") ON CONFLICT REPLACE)");

		db.execSQL("CREATE TABLE " + Tables.BUDDIES + " ("
			+ BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
			+ SyncColumns.UPDATED + " INTEGER,"
			+ SyncListColumns.UPDATED_LIST + " INTEGER NOT NULL,"
			+ BuddiesColumns.BUDDY_ID + " INTEGER NOT NULL,"
			+ BuddiesColumns.BUDDY_NAME + " TEXT NOT NULL,"
			+ BuddiesColumns.BUDDY_FIRSTNAME + " TEXT,"
			+ BuddiesColumns.BUDDY_LASTNAME + " TEXT,"
			+ BuddiesColumns.AVATAR_URL + " TEXT,"
			+ "UNIQUE (" + BuddiesColumns.BUDDY_ID + ") ON CONFLICT REPLACE)");

		builder.reset().table(Tables.GAME_POLLS).defaultPrimaryKey()
			.column(GamesColumns.GAME_ID, COLUMN_TYPE.INTEGER, true, true, Tables.GAMES, GamesColumns.GAME_ID)
			.column(GamePollsColumns.POLL_NAME, COLUMN_TYPE.TEXT, true, true)
			.column(GamePollsColumns.POLL_TITLE, COLUMN_TYPE.TEXT, true)
			.column(GamePollsColumns.POLL_TOTAL_VOTES, COLUMN_TYPE.INTEGER, true)
			.create(db);

		builder.reset().table(Tables.GAME_POLL_RESULTS).defaultPrimaryKey()
			.column(GamePollResultsColumns.POLL_ID, COLUMN_TYPE.INTEGER, true, true, Tables.GAME_POLLS, BaseColumns._ID)
			.column(GamePollResultsColumns.POLL_RESULTS_KEY, COLUMN_TYPE.TEXT, true, true)
			.column(GamePollResultsColumns.POLL_RESULTS_PLAYERS, COLUMN_TYPE.TEXT)
			.column(GamePollResultsColumns.POLL_RESULTS_SORT_INDEX, COLUMN_TYPE.INTEGER, true)
			.create(db);

		builder.reset().table(Tables.GAME_POLL_RESULTS_RESULT).defaultPrimaryKey()
			.column(GamePollResultsResultColumns.POLL_RESULTS_ID, COLUMN_TYPE.INTEGER, true, true, Tables.GAME_POLL_RESULTS, BaseColumns._ID)
			.column(GamePollResultsResultColumns.POLL_RESULTS_RESULT_KEY, COLUMN_TYPE.TEXT, true, true)
			.column(GamePollResultsResultColumns.POLL_RESULTS_RESULT_LEVEL, COLUMN_TYPE.INTEGER)
			.column(GamePollResultsResultColumns.POLL_RESULTS_RESULT_VALUE, COLUMN_TYPE.TEXT, true)
			.column(GamePollResultsResultColumns.POLL_RESULTS_RESULT_VOTES, COLUMN_TYPE.INTEGER, true)
			.column(GamePollResultsResultColumns.POLL_RESULTS_RESULT_SORT_INDEX, COLUMN_TYPE.INTEGER, true)
			.create(db);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		Log.d(TAG, "onUpgrade() from " + oldVersion + " to " + newVersion);

		// NOTE: This switch statement is designed to handle cascading database
		// updates, starting at the current version and falling through to all
		// future upgrade cases. Only use "break;" when you want to drop and
		// recreate the entire database.
		int version = oldVersion;

		switch (version) {
			case VER_INITIAL:
				// Version 2 added column for collection wishlist priority.
				db.execSQL("ALTER TABLE " + Tables.COLLECTION + " ADD COLUMN "
						+ CollectionColumns.STATUS_WISHLIST_PRIORITY + " INTEGER");
				version = VER_WISHLIST_PRIORITY;
		}

		if (version != DATABASE_VERSION) {
			Log.w(TAG, "Destroying old data during upgrade");

			db.execSQL("DROP TABLE IF EXISTS " + Tables.DESIGNERS);
			db.execSQL("DROP TABLE IF EXISTS " + Tables.ARTISTS);
			db.execSQL("DROP TABLE IF EXISTS " + Tables.PUBLISHERS);
			db.execSQL("DROP TABLE IF EXISTS " + Tables.MECHANICS);
			db.execSQL("DROP TABLE IF EXISTS " + Tables.CATEGORIES);
			db.execSQL("DROP TABLE IF EXISTS " + Tables.GAMES);
			db.execSQL("DROP TABLE IF EXISTS " + Tables.GAME_RANKS);
			db.execSQL("DROP TABLE IF EXISTS " + Tables.GAMES_DESIGNERS);
			db.execSQL("DROP TABLE IF EXISTS " + Tables.GAMES_ARTISTS);
			db.execSQL("DROP TABLE IF EXISTS " + Tables.GAMES_PUBLISHERS);
			db.execSQL("DROP TABLE IF EXISTS " + Tables.GAMES_MECHANICS);
			db.execSQL("DROP TABLE IF EXISTS " + Tables.GAMES_CATEGORIES);
			db.execSQL("DROP TABLE IF EXISTS " + Tables.COLLECTION);
			db.execSQL("DROP TABLE IF EXISTS " + Tables.BUDDIES);
			db.execSQL("DROP TABLE IF EXISTS " + Tables.GAME_POLLS);
			db.execSQL("DROP TABLE IF EXISTS " + Tables.GAME_POLL_RESULTS);
			db.execSQL("DROP TABLE IF EXISTS " + Tables.GAME_POLL_RESULTS_RESULT);

			onCreate(db);
		}
	}
}
