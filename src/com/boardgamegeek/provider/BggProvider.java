package com.boardgamegeek.provider;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.HashMap;

import android.app.SearchManager;
import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.BaseColumns;
import android.text.TextUtils;
import android.util.Log;

import com.boardgamegeek.provider.BggContract.Artists;
import com.boardgamegeek.provider.BggContract.Buddies;
import com.boardgamegeek.provider.BggContract.Categories;
import com.boardgamegeek.provider.BggContract.Collection;
import com.boardgamegeek.provider.BggContract.Designers;
import com.boardgamegeek.provider.BggContract.GamePollResults;
import com.boardgamegeek.provider.BggContract.GamePollResultsResult;
import com.boardgamegeek.provider.BggContract.GamePolls;
import com.boardgamegeek.provider.BggContract.GameRanks;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.provider.BggContract.Mechanics;
import com.boardgamegeek.provider.BggContract.Publishers;
import com.boardgamegeek.provider.BggContract.SyncColumns;
import com.boardgamegeek.provider.BggContract.SyncListColumns;
import com.boardgamegeek.provider.BggContract.Thumbnails;
import com.boardgamegeek.provider.BggDatabase.GamesArtists;
import com.boardgamegeek.provider.BggDatabase.GamesCategories;
import com.boardgamegeek.provider.BggDatabase.GamesDesigners;
import com.boardgamegeek.provider.BggDatabase.GamesMechanics;
import com.boardgamegeek.provider.BggDatabase.GamesPublishers;
import com.boardgamegeek.provider.BggDatabase.Tables;
import com.boardgamegeek.util.ImageCache;
import com.boardgamegeek.util.SelectionBuilder;

public class BggProvider extends ContentProvider {
	private static final String TAG = "BggProvider";
	private static final boolean LOGV = true; // Log.isLoggable(TAG,
												// Log.VERBOSE);

	private BggDatabase mOpenHelper;

	private static final UriMatcher sUriMatcher = buildUriMatcher();
	private static final HashMap<String, String> sSuggestionProjectionMap = buildSuggestionProjectionMap();

	private static final String DEFAULT_KEY = "X";

	private static final int GAMES = 100;
	private static final int GAMES_ID = 101;
	private static final int GAMES_RANKS = 102;
	private static final int GAMES_RANKS_ID = 103;
	private static final int GAMES_ID_RANKS = 104;
	private static final int GAMES_ID_DESIGNERS = 105;
	private static final int GAMES_ID_DESIGNERS_ID = 1051;
	private static final int GAMES_ID_ARTISTS = 106;
	private static final int GAMES_ID_ARTISTS_ID = 1061;
	private static final int GAMES_ID_PUBLISHERS = 107;
	private static final int GAMES_ID_PUBLISHERS_ID = 1071;
	private static final int GAMES_ID_MECHANICS = 108;
	private static final int GAMES_ID_MECHANICS_ID = 1081;
	private static final int GAMES_ID_CATEGORIES = 109;
	private static final int GAMES_ID_CATEGORIES_ID = 1091;
	private static final int GAMES_ID_POLLS = 110;
	private static final int GAMES_ID_POLLS_NAME = 1101;
	private static final int GAMES_ID_POLLS_NAME_RESULTS = 1102;
	private static final int GAMES_ID_POLLS_NAME_RESULTS_KEY = 1103;
	private static final int GAMES_ID_POLLS_NAME_RESULTS_KEY_RESULT = 1104;
	private static final int GAMES_ID_POLLS_NAME_RESULTS_KEY_RESULT_KEY = 1105;
	private static final int GAMES_DESIGNERS_ID = 401;
	private static final int GAMES_ARTISTS_ID = 403;
	private static final int GAMES_PUBLISHERS_ID = 405;
	private static final int GAMES_MECHANICS_ID = 407;
	private static final int GAMES_CATEGORIES_ID = 409;
	private static final int DESIGNERS = 301;
	private static final int DESIGNERS_ID = 302;
	private static final int ARTISTS = 303;
	private static final int ARTISTS_ID = 304;
	private static final int PUBLISHERS = 305;
	private static final int PUBLISHERS_ID = 306;
	private static final int MECHANICS = 307;
	private static final int MECHANICS_ID = 308;
	private static final int CATEGORIES = 309;
	private static final int CATEGORIES_ID = 310;
	private static final int COLLECTION = 200;
	private static final int COLLECTION_ID = 201;
	private static final int BUDDIES = 1000;
	private static final int BUDDIES_ID = 1001;
	private static final int SEARCH_SUGGEST = 9998;
	private static final int SHORTCUT_REFRESH = 9999;

	private static UriMatcher buildUriMatcher() {
		final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
		final String authority = BggContract.CONTENT_AUTHORITY;

		matcher.addURI(authority, "games", GAMES);
		matcher.addURI(authority, "games/#", GAMES_ID);
		matcher.addURI(authority, "games/ranks", GAMES_RANKS);
		matcher.addURI(authority, "games/ranks/#", GAMES_RANKS_ID);
		matcher.addURI(authority, "games/#/ranks", GAMES_ID_RANKS);
		matcher.addURI(authority, "games/#/designers", GAMES_ID_DESIGNERS);
		matcher.addURI(authority, "games/#/designers/#", GAMES_ID_DESIGNERS_ID);
		matcher.addURI(authority, "games/#/artists", GAMES_ID_ARTISTS);
		matcher.addURI(authority, "games/#/artists/#", GAMES_ID_ARTISTS_ID);
		matcher.addURI(authority, "games/#/publishers", GAMES_ID_PUBLISHERS);
		matcher.addURI(authority, "games/#/publishers/#", GAMES_ID_PUBLISHERS_ID);
		matcher.addURI(authority, "games/#/mechanics", GAMES_ID_MECHANICS);
		matcher.addURI(authority, "games/#/mechanics/#", GAMES_ID_MECHANICS_ID);
		matcher.addURI(authority, "games/#/categories", GAMES_ID_CATEGORIES);
		matcher.addURI(authority, "games/#/categories/#", GAMES_ID_CATEGORIES_ID);
		matcher.addURI(authority, "games/#/polls", GAMES_ID_POLLS);
		matcher.addURI(authority, "games/#/polls/*", GAMES_ID_POLLS_NAME);
		matcher.addURI(authority, "games/#/polls/*/results", GAMES_ID_POLLS_NAME_RESULTS);
		matcher.addURI(authority, "games/#/polls/*/results/*", GAMES_ID_POLLS_NAME_RESULTS_KEY);
		matcher.addURI(authority, "games/#/polls/*/results/*/result", GAMES_ID_POLLS_NAME_RESULTS_KEY_RESULT);
		matcher.addURI(authority, "games/#/polls/*/results/*/result/*", GAMES_ID_POLLS_NAME_RESULTS_KEY_RESULT_KEY);
		matcher.addURI(authority, "games/designers/#", GAMES_DESIGNERS_ID);
		matcher.addURI(authority, "games/artists/#", GAMES_ARTISTS_ID);
		matcher.addURI(authority, "games/publishers/#", GAMES_PUBLISHERS_ID);
		matcher.addURI(authority, "games/mechanics/#", GAMES_MECHANICS_ID);
		matcher.addURI(authority, "games/categories/#", GAMES_CATEGORIES_ID);
		matcher.addURI(authority, "designers", DESIGNERS);
		matcher.addURI(authority, "designers/#", DESIGNERS_ID);
		matcher.addURI(authority, "artists", ARTISTS);
		matcher.addURI(authority, "artists/#", ARTISTS_ID);
		matcher.addURI(authority, "publishers", PUBLISHERS);
		matcher.addURI(authority, "publishers/#", PUBLISHERS_ID);
		matcher.addURI(authority, "mechanics", MECHANICS);
		matcher.addURI(authority, "mechanics/#", MECHANICS_ID);
		matcher.addURI(authority, "categories", CATEGORIES);
		matcher.addURI(authority, "categories/#", CATEGORIES_ID);
		matcher.addURI(authority, "collection", COLLECTION);
		matcher.addURI(authority, "collection/#", COLLECTION_ID);
		matcher.addURI(authority, "buddies", BUDDIES);
		matcher.addURI(authority, "buddies/#", BUDDIES_ID);
		matcher.addURI(authority, SearchManager.SUGGEST_URI_PATH_QUERY, SEARCH_SUGGEST);
		matcher.addURI(authority, SearchManager.SUGGEST_URI_PATH_QUERY + "/*", SEARCH_SUGGEST);
		matcher.addURI(authority, SearchManager.SUGGEST_URI_PATH_SHORTCUT, SHORTCUT_REFRESH);
		matcher.addURI(authority, SearchManager.SUGGEST_URI_PATH_SHORTCUT + "/#", SHORTCUT_REFRESH);

		return matcher;
	}

	private static HashMap<String, String> buildSuggestionProjectionMap() {
		HashMap<String, String> map = new HashMap<String, String>();
		map.put(Games._ID, Games._ID);
		map.put(SearchManager.SUGGEST_COLUMN_TEXT_1, Games.GAME_NAME + " AS " + SearchManager.SUGGEST_COLUMN_TEXT_1);
		map.put(SearchManager.SUGGEST_COLUMN_TEXT_2, Games.YEAR_PUBLISHED + " AS "
				+ SearchManager.SUGGEST_COLUMN_TEXT_2);
		map.put(SearchManager.SUGGEST_COLUMN_INTENT_DATA_ID, Tables.GAMES + "." + Games.GAME_ID + " AS "
				+ SearchManager.SUGGEST_COLUMN_INTENT_DATA_ID);
		map.put(SearchManager.SUGGEST_COLUMN_SHORTCUT_ID, Tables.GAMES + "." + Games.GAME_ID + " AS "
				+ SearchManager.SUGGEST_COLUMN_SHORTCUT_ID);
		map.put(SearchManager.SUGGEST_COLUMN_ICON_1, "0 AS " + SearchManager.SUGGEST_COLUMN_ICON_1);
		map.put(SearchManager.SUGGEST_COLUMN_ICON_2, "'" + Thumbnails.CONTENT_URI + "/' || " + Tables.GAMES + "."
				+ Games.THUMBNAIL_URL + " AS " + SearchManager.SUGGEST_COLUMN_ICON_2);
		map.put(Games.GAME_SORT_NAME, "(CASE WHEN " + Games.GAME_SORT_NAME + " IS NULL THEN " + Games.GAME_SORT_NAME
				+ " ELSE " + Games.GAME_SORT_NAME + " END) AS " + Games.GAME_SORT_NAME);
		return map;
	}

	private interface Qualified {
		String GAMES_DESIGNERS_GAME_ID = Tables.GAMES_DESIGNERS + "." + GamesDesigners.GAME_ID;
		String GAMES_ARTISTS_GAME_ID = Tables.GAMES_ARTISTS + "." + GamesArtists.GAME_ID;
		String GAMES_PUBLISHERS_GAME_ID = Tables.GAMES_PUBLISHERS + "." + GamesPublishers.GAME_ID;
		String GAMES_MECHANICS_GAME_ID = Tables.GAMES_MECHANICS + "." + GamesMechanics.GAME_ID;
		String GAMES_CATEGORIES_GAME_ID = Tables.GAMES_CATEGORIES + "." + GamesCategories.GAME_ID;
	}

	@Override
	public boolean onCreate() {
		final Context context = getContext();
		mOpenHelper = new BggDatabase(context);
		return true;
	}

	@Override
	public String getType(Uri uri) {
		final int match = sUriMatcher.match(uri);
		switch (match) {
			case GAMES:
				return Games.CONTENT_TYPE;
			case GAMES_ID:
				return Games.CONTENT_ITEM_TYPE;
			case GAMES_RANKS:
				return GameRanks.CONTENT_TYPE;
			case GAMES_RANKS_ID:
				return GameRanks.CONTENT_ITEM_TYPE;
			case GAMES_ID_RANKS:
				return GameRanks.CONTENT_TYPE;
			case GAMES_ID_POLLS:
				return GamePolls.CONTENT_TYPE;
			case GAMES_ID_POLLS_NAME:
				return GamePolls.CONTENT_ITEM_TYPE;
			case GAMES_ID_POLLS_NAME_RESULTS:
				return GamePollResults.CONTENT_TYPE;
			case GAMES_ID_POLLS_NAME_RESULTS_KEY:
				return GamePollResults.CONTENT_ITEM_TYPE;
			case GAMES_ID_POLLS_NAME_RESULTS_KEY_RESULT:
				return GamePollResultsResult.CONTENT_TYPE;
			case GAMES_ID_POLLS_NAME_RESULTS_KEY_RESULT_KEY:
				return GamePollResultsResult.CONTENT_ITEM_TYPE;
			case GAMES_ID_DESIGNERS:
				return Designers.CONTENT_TYPE;
			case GAMES_ID_DESIGNERS_ID:
				return Designers.CONTENT_ITEM_TYPE;
			case GAMES_ID_ARTISTS:
				return Artists.CONTENT_TYPE;
			case GAMES_ID_ARTISTS_ID:
				return Artists.CONTENT_ITEM_TYPE;
			case GAMES_ID_PUBLISHERS:
				return Publishers.CONTENT_TYPE;
			case GAMES_ID_PUBLISHERS_ID:
				return Publishers.CONTENT_ITEM_TYPE;
			case GAMES_ID_MECHANICS:
				return Mechanics.CONTENT_TYPE;
			case GAMES_ID_MECHANICS_ID:
				return Mechanics.CONTENT_ITEM_TYPE;
			case GAMES_ID_CATEGORIES:
				return Categories.CONTENT_TYPE;
			case GAMES_ID_CATEGORIES_ID:
				return Categories.CONTENT_ITEM_TYPE;
			case GAMES_DESIGNERS_ID:
				return Designers.CONTENT_ITEM_TYPE;
			case GAMES_ARTISTS_ID:
				return Artists.CONTENT_ITEM_TYPE;
			case GAMES_PUBLISHERS_ID:
				return Publishers.CONTENT_ITEM_TYPE;
			case GAMES_MECHANICS_ID:
				return Mechanics.CONTENT_ITEM_TYPE;
			case GAMES_CATEGORIES_ID:
				return Categories.CONTENT_ITEM_TYPE;
			case DESIGNERS:
				return Designers.CONTENT_TYPE;
			case DESIGNERS_ID:
				return Designers.CONTENT_ITEM_TYPE;
			case ARTISTS:
				return Artists.CONTENT_TYPE;
			case ARTISTS_ID:
				return Artists.CONTENT_ITEM_TYPE;
			case PUBLISHERS:
				return Publishers.CONTENT_TYPE;
			case MECHANICS:
				return Mechanics.CONTENT_TYPE;
			case MECHANICS_ID:
				return Mechanics.CONTENT_ITEM_TYPE;
			case CATEGORIES:
				return Categories.CONTENT_TYPE;
			case CATEGORIES_ID:
				return Categories.CONTENT_ITEM_TYPE;
			case PUBLISHERS_ID:
				return Publishers.CONTENT_ITEM_TYPE;
			case COLLECTION:
				return Collection.CONTENT_TYPE;
			case COLLECTION_ID:
				return Collection.CONTENT_ITEM_TYPE;
			case BUDDIES:
				return Buddies.CONTENT_TYPE;
			case BUDDIES_ID:
				return Buddies.CONTENT_ITEM_TYPE;
			case SEARCH_SUGGEST:
				return SearchManager.SUGGEST_MIME_TYPE;
			case SHORTCUT_REFRESH:
				return SearchManager.SHORTCUT_MIME_TYPE;
			default:
				throw new UnsupportedOperationException("Unknown uri: " + uri);
		}
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		if (LOGV) {
			Log.v(TAG, "query(uri=" + uri + ", proj=" + Arrays.toString(projection) + ")");
		}
		final SQLiteDatabase db = mOpenHelper.getReadableDatabase();

		final int match = sUriMatcher.match(uri);
		switch (match) {
			case SEARCH_SUGGEST: {
				String query = null;
				if (uri.getPathSegments().size() > 1) {
					query = uri.getLastPathSegment().toLowerCase();
				}
				if (query == null) {
					return null;
				} else {
					query = URLEncoder.encode(query);
					final SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
					qb.setTables(Tables.GAMES);
					qb.setProjectionMap(sSuggestionProjectionMap);
					qb.appendWhere("(" + Tables.GAMES + "." + Games.GAME_NAME + " like '" + query + "%' OR "
							+ Tables.GAMES + "." + Games.GAME_NAME + " like '% " + query + "%')");
					String orderBy;
					if (TextUtils.isEmpty(sortOrder)) {
						orderBy = Games.DEFAULT_SORT;
					} else {
						orderBy = sortOrder;
					}
					Cursor c = qb.query(db, projection, selection, selectionArgs, null, null, orderBy);
					c.setNotificationUri(getContext().getContentResolver(), uri);
					Log.d(TAG, "Queried URI " + uri);
					return c;
				}
			}
			case SHORTCUT_REFRESH: {
				String shortcutId = null;
				if (uri.getPathSegments().size() > 1) {
					shortcutId = uri.getLastPathSegment();
				}
				if (TextUtils.isEmpty(shortcutId)) {
					return null;
				} else {
					final SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
					qb.setTables(Tables.GAMES);
					qb.setProjectionMap(sSuggestionProjectionMap);
					qb.appendWhere(SearchManager.SUGGEST_COLUMN_SHORTCUT_ID + "=" + uri.getPathSegments().get(1));
					Cursor c = qb.query(db, projection, selection, selectionArgs, null, null, sortOrder);
					c.setNotificationUri(getContext().getContentResolver(), uri);
					Log.d(TAG, "Queried URI " + uri);
					return c;
				}
			}
			default: {
				final SelectionBuilder builder = buildExpandedSelection(uri, match);
				// TODO: move this to selection builder map
				if (match == COLLECTION_ID) {
					for (int i = 0; i < projection.length; i++) {
						if (SyncColumns.UPDATED.equals(projection[i])
								|| SyncListColumns.UPDATED_LIST.equals(projection[i])) {
							builder.mapToTable(projection[i], Tables.COLLECTION);
						}
					}
				}
				return builder.where(selection, selectionArgs).query(db, projection, sortOrder);
			}
		}
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		if (LOGV) {
			Log.v(TAG, "insert(uri=" + uri + ", values=" + values.toString() + ")");
		}

		final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		final int match = sUriMatcher.match(uri);
		Uri newUri = null;
		long rowId = -1;

		switch (match) {
			case GAMES: {
				rowId = db.insertOrThrow(Tables.GAMES, null, values);
				newUri = Games.buildGameUri(values.getAsInteger(Games.GAME_ID));
				break;
			}
			case GAMES_ID_RANKS: {
				final int gameId = Games.getGameId(uri);
				values.put(GameRanks.GAME_ID, gameId);
				rowId = db.insertOrThrow(Tables.GAME_RANKS, null, values);
				newUri = GameRanks.buildGameRankUri((int) rowId);
				break;
			}
			case GAMES_ID_DESIGNERS: {
				rowId = db.insertOrThrow(Tables.GAMES_DESIGNERS, null, values);
				newUri = Games.buildDesignersUri(rowId);
				break;
			}
			case GAMES_ID_ARTISTS: {
				rowId = db.insertOrThrow(Tables.GAMES_ARTISTS, null, values);
				newUri = Games.buildArtistUri(rowId);
				break;
			}
			case GAMES_ID_PUBLISHERS: {
				rowId = db.insertOrThrow(Tables.GAMES_PUBLISHERS, null, values);
				newUri = Games.buildPublisherUri(rowId);
				break;
			}
			case GAMES_ID_MECHANICS: {
				rowId = db.insertOrThrow(Tables.GAMES_MECHANICS, null, values);
				newUri = Games.buildMechanicUri(rowId);
				break;
			}
			case GAMES_ID_CATEGORIES: {
				rowId = db.insertOrThrow(Tables.GAMES_CATEGORIES, null, values);
				newUri = Games.buildCategoryUri(rowId);
				break;
			}
			case GAMES_ID_POLLS: {
				final int gameId = Games.getGameId(uri);
				values.put(GamePolls.GAME_ID, gameId);
				rowId = db.insertOrThrow(Tables.GAME_POLLS, null, values);
				String pollName = values.getAsString(GamePolls.POLL_NAME);
				newUri = Games.buildPollsUri(gameId, pollName);
				break;
			}
			case GAMES_ID_POLLS_NAME_RESULTS: {
				final int gameId = Games.getGameId(uri);
				final String pollName = Games.getPollName(uri);

				int id = queryInt(Games.buildPollsUri(gameId, pollName), GamePolls._ID);
				values.put(GamePollResults.POLL_ID, id);

				String key = values.getAsString(GamePollResults.POLL_RESULTS_PLAYERS);
				if (TextUtils.isEmpty(key)) {
					key = DEFAULT_KEY;
				}
				values.put(GamePollResults.POLL_RESULTS_KEY, key);

				rowId = db.insertOrThrow(Tables.GAME_POLL_RESULTS, null, values);
				newUri = Games.buildPollResultsUri(gameId, pollName,
						values.getAsString(GamePollResults.POLL_RESULTS_PLAYERS));
				break;
			}
			case GAMES_ID_POLLS_NAME_RESULTS_KEY_RESULT: {
				final int gameId = Games.getGameId(uri);
				final String pollName = Games.getPollName(uri);
				final String players = Games.getPollResultsKey(uri);

				int id = queryInt(Games.buildPollResultsUri(gameId, pollName, players), GamePollResultsResult._ID);
				values.put(GamePollResultsResult.POLL_RESULTS_ID, id);

				String key = values.getAsString(GamePollResultsResult.POLL_RESULTS_RESULT_LEVEL);
				if (TextUtils.isEmpty(key)) {
					key = values.getAsString(GamePollResultsResult.POLL_RESULTS_RESULT_VALUE);
					int index = key.indexOf(" ");
					if (index > -1) {
						key = key.substring(0, index - 1);
					}
				}
				values.put(GamePollResultsResult.POLL_RESULTS_RESULT_KEY, key);

				rowId = db.insertOrThrow(Tables.GAME_POLL_RESULTS_RESULT, null, values);
				newUri = Games.buildPollResultsResultUri(gameId, pollName, players,
						values.getAsString(GamePollResults.POLL_RESULTS_PLAYERS));
				break;
			}
			case DESIGNERS: {
				rowId = db.insertOrThrow(Tables.DESIGNERS, null, values);
				newUri = Designers.buildDesignerUri(values.getAsInteger(Designers.DESIGNER_ID));
				break;
			}
			case ARTISTS: {
				rowId = db.insertOrThrow(Tables.ARTISTS, null, values);
				newUri = Artists.buildArtistUri(values.getAsInteger(Artists.ARTIST_ID));
				break;
			}
			case PUBLISHERS: {
				rowId = db.insertOrThrow(Tables.PUBLISHERS, null, values);
				newUri = Publishers.buildPublisherUri(values.getAsInteger(Publishers.PUBLISHER_ID));
				break;
			}
			case MECHANICS: {
				rowId = db.insertOrThrow(Tables.MECHANICS, null, values);
				newUri = Mechanics.buildMechanicUri(values.getAsInteger(Mechanics.MECHANIC_ID));
				break;
			}
			case CATEGORIES: {
				rowId = db.insertOrThrow(Tables.CATEGORIES, null, values);
				newUri = Categories.buildCategoryUri(values.getAsInteger(Categories.CATEGORY_ID));
				break;
			}
			case COLLECTION: {
				rowId = db.insertOrThrow(Tables.COLLECTION, null, values);
				newUri = Collection.buildItemUri(values.getAsInteger(Collection.COLLECTION_ID));
				break;
			}
			case BUDDIES: {
				rowId = db.insertOrThrow(Tables.BUDDIES, null, values);
				newUri = Buddies.buildBuddyUri(values.getAsInteger(Buddies.BUDDY_ID));
				break;
			}
			default: {
				throw new UnsupportedOperationException("Unknown uri: " + uri);
			}
		}

		if (newUri != null) {
			getContext().getContentResolver().notifyChange(newUri, null);
		}
		return newUri;
	}

	private int queryInt(Uri uri, String columnName) {
		int id = 0;
		Cursor c = query(uri, new String[] { columnName }, null, null, null);
		try {
			if (c.moveToFirst()) {
				id = c.getInt(0);
			}
		} finally {
			if (c != null) {
				c.close();
			}
		}
		return id;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		if (LOGV) {
			Log.v(TAG, "update(uri=" + uri + ", values=" + values.toString() + ")");
		}

		final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		final int match = sUriMatcher.match(uri);

		final SelectionBuilder builder = buildSimpleSelection(uri, match);
		final int rowCount = builder.where(selection, selectionArgs).update(db, values);

		if (LOGV) {
			Log.v(TAG, "updated " + rowCount + " rows");
		}

		getContext().getContentResolver().notifyChange(uri, null);

		return rowCount;
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		if (LOGV) {
			Log.v(TAG, "delete(uri=" + uri + ")");
		}

		int rowCount = 0;
		final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		final int match = sUriMatcher.match(uri);

		final SelectionBuilder builder = buildSimpleSelection(uri, match).where(selection, selectionArgs);
		switch (match) {
			case GAMES:
			case GAMES_ID:
				deleteGameChildren(db, builder);
				break;
//			case GAMES_ID_POLLS: {
//				int gameId = Games.getGameId(uri);
//				delete(Games.buildPollsUri(gameId), null, null);
//				break;
//			}
//			case GAMES_ID_POLLS_NAME:
//			case GAMES_ID_POLLS_NAME_RESULTS: {
//				// TODO: delete poll children
//				int gameId = Games.getGameId(uri);
//				String pollName = Games.getPollName(uri);
//				delete(Games.buildPollResultsUri(gameId, pollName), null, null);
//				break;
//			}
//			case GAMES_ID_POLLS_NAME_RESULTS_KEY:
//			case GAMES_ID_POLLS_NAME_RESULTS_KEY_RESULT: {
//				// TODO: delete poll children
//				int gameId = Games.getGameId(uri);
//				String pollName = Games.getPollName(uri);
//				String key = Games.getPollResultsKey(uri);
//				delete(Games.buildPollResultsResultUri(gameId, pollName, key), null, null);
//				break;
//			}
			default:
				break;
		}
		rowCount = builder.delete(db);

		if (LOGV) {
			Log.v(TAG, "deleted " + rowCount + " rows");
		}

		getContext().getContentResolver().notifyChange(uri, null);

		return rowCount;
	}

	@Override
	public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
		// TODO: fix this to not include the entire thumbnail URL in the URI
		// TODO: test for a URI match
		String fileName = uri.getLastPathSegment();
		final File file = ImageCache.getExistingImageFile(fileName);
		if (file != null) {
			return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
		}
		return null;
	}

	private SelectionBuilder buildSimpleSelection(Uri uri, int match) {
		final SelectionBuilder builder = new SelectionBuilder();

		switch (match) {
			case GAMES:
				return builder.table(Tables.GAMES);
			case GAMES_ID: {
				final int gameId = Games.getGameId(uri);
				return builder.table(Tables.GAMES).where(Games.GAME_ID + "=?", "" + gameId);
			}
			case GAMES_RANKS:
				return builder.table(Tables.GAME_RANKS);
			case GAMES_RANKS_ID: {
				final int rankId = GameRanks.getRankId(uri);
				return builder.table(Tables.GAME_RANKS).where(GameRanks.GAME_RANK_ID + "=?", "" + rankId);
			}
			case GAMES_ID_RANKS: {
				final int gameId = Games.getGameId(uri);
				return builder.table(Tables.GAME_RANKS).where(GameRanks.GAME_ID + "=?", "" + gameId);
			}
			case GAMES_ID_DESIGNERS: {
				final int gameId = Games.getGameId(uri);
				return builder.table(Tables.GAMES_DESIGNERS).where(GamesDesigners.GAME_ID + "=?", "" + gameId);
			}
			case GAMES_ID_DESIGNERS_ID: {
				final int gameId = Games.getGameId(uri);
				final long designerId = ContentUris.parseId(uri);
				return builder.table(Tables.GAMES_DESIGNERS).where(GamesDesigners.GAME_ID + "=?", "" + gameId)
						.where(GamesDesigners.DESIGNER_ID + "=?", "" + designerId);
			}
			case GAMES_ID_ARTISTS: {
				final int gameId = Games.getGameId(uri);
				return builder.table(Tables.GAMES_ARTISTS).where(GamesArtists.GAME_ID + "=?", "" + gameId);
			}
			case GAMES_ID_ARTISTS_ID: {
				final int gameId = Games.getGameId(uri);
				final long artistId = ContentUris.parseId(uri);
				return builder.table(Tables.GAMES_ARTISTS).where(GamesArtists.GAME_ID + "=?", "" + gameId)
						.where(GamesArtists.ARTIST_ID + "=?", "" + artistId);
			}
			case GAMES_ID_PUBLISHERS: {
				final int gameId = Games.getGameId(uri);
				return builder.table(Tables.GAMES_PUBLISHERS).where(GamesPublishers.GAME_ID + "=?", "" + gameId);
			}
			case GAMES_ID_PUBLISHERS_ID: {
				final int gameId = Games.getGameId(uri);
				final long publisherId = ContentUris.parseId(uri);
				return builder.table(Tables.GAMES_PUBLISHERS).where(GamesPublishers.GAME_ID + "=?", "" + gameId)
						.where(GamesPublishers.PUBLISHER_ID + "=?", "" + publisherId);
			}
			case GAMES_ID_MECHANICS: {
				final int gameId = Games.getGameId(uri);
				return builder.table(Tables.GAMES_MECHANICS).where(GamesMechanics.GAME_ID + "=?", "" + gameId);
			}
			case GAMES_ID_MECHANICS_ID: {
				final int gameId = Games.getGameId(uri);
				final long mechanicId = ContentUris.parseId(uri);
				return builder.table(Tables.GAMES_MECHANICS).where(GamesMechanics.GAME_ID + "=?", "" + gameId)
						.where(GamesMechanics.MECHANIC_ID + "=?", "" + mechanicId);
			}
			case GAMES_ID_CATEGORIES: {
				final int gameId = Games.getGameId(uri);
				return builder.table(Tables.GAMES_CATEGORIES).where(GamesCategories.GAME_ID + "=?", "" + gameId);
			}
			case GAMES_ID_CATEGORIES_ID: {
				final int gameId = Games.getGameId(uri);
				final long categoryId = ContentUris.parseId(uri);
				return builder.table(Tables.GAMES_CATEGORIES).where(GamesCategories.GAME_ID + "=?", "" + gameId)
						.where(GamesCategories.CATEGORY_ID + "=?", "" + categoryId);
			}
			case GAMES_DESIGNERS_ID: {
				final long id = ContentUris.parseId(uri);
				return builder.table(Tables.GAMES_DESIGNERS).where(BaseColumns._ID + "=?", "" + id);
			}
			case GAMES_ARTISTS_ID: {
				final long id = ContentUris.parseId(uri);
				return builder.table(Tables.GAMES_ARTISTS).where(BaseColumns._ID + "=?", "" + id);
			}
			case GAMES_PUBLISHERS_ID: {
				final long id = ContentUris.parseId(uri);
				return builder.table(Tables.GAMES_PUBLISHERS).where(BaseColumns._ID + "=?", "" + id);
			}
			case GAMES_MECHANICS_ID: {
				final long id = ContentUris.parseId(uri);
				return builder.table(Tables.GAMES_MECHANICS).where(BaseColumns._ID + "=?", "" + id);
			}
			case GAMES_CATEGORIES_ID: {
				final long id = ContentUris.parseId(uri);
				return builder.table(Tables.GAMES_CATEGORIES).where(BaseColumns._ID + "=?", "" + id);
			}
			case DESIGNERS:
				return builder.table(Tables.DESIGNERS);
			case DESIGNERS_ID:
				final int designerId = Designers.getDesignerId(uri);
				return builder.table(Tables.DESIGNERS).where(Designers.DESIGNER_ID + "=?", "" + designerId);
			case ARTISTS:
				return builder.table(Tables.ARTISTS);
			case ARTISTS_ID:
				final int artistId = Artists.getArtistId(uri);
				return builder.table(Tables.ARTISTS).where(Artists.ARTIST_ID + "=?", "" + artistId);
			case PUBLISHERS:
				return builder.table(Tables.PUBLISHERS);
			case PUBLISHERS_ID:
				final int publisherId = Publishers.getPublisherId(uri);
				return builder.table(Tables.PUBLISHERS).where(Publishers.PUBLISHER_ID + "=?", "" + publisherId);
			case MECHANICS:
				return builder.table(Tables.MECHANICS);
			case MECHANICS_ID:
				final int mechanicId = Mechanics.getMechanicId(uri);
				return builder.table(Tables.MECHANICS).where(Mechanics.MECHANIC_ID + "=?", "" + mechanicId);
			case CATEGORIES:
				return builder.table(Tables.CATEGORIES);
			case CATEGORIES_ID:
				final int categoryId = Categories.getCategoryId(uri);
				return builder.table(Tables.CATEGORIES).where(Categories.CATEGORY_ID + "=?", "" + categoryId);
			case COLLECTION:
				return builder.table(Tables.COLLECTION);
			case COLLECTION_ID:
				final int itemId = Collection.getItemId(uri);
				return builder.table(Tables.COLLECTION).where(Collection.COLLECTION_ID + "=?", "" + itemId);
			case BUDDIES:
				return builder.table(Tables.BUDDIES);
			case BUDDIES_ID:
				final int blockId = Buddies.getBuddyId(uri);
				return builder.table(Tables.BUDDIES).where(Buddies.BUDDY_ID + "=?", "" + blockId);
			case GAMES_ID_POLLS: {
				final int gameId = Games.getGameId(uri);
				return builder.table(Tables.GAME_POLLS).where(GamePolls.GAME_ID + "=?", "" + gameId);
			}
			case GAMES_ID_POLLS_NAME: {
				final int gameId = Games.getGameId(uri);
				final String pollName = uri.getLastPathSegment();
				return builder.table(Tables.GAME_POLLS)
					.where(GamePolls.GAME_ID + "=?", "" + gameId)
					.where(GamePolls.POLL_NAME + "=?", pollName);
			}
			case GAMES_ID_POLLS_NAME_RESULTS: {
				final int gameId = Games.getGameId(uri);
				final String pollName = Games.getPollName(uri);
				return builder.table(Tables.GAME_POLL_RESULTS)
					.mapToTable(BaseColumns._ID, Tables.GAME_POLL_RESULTS)
					.where("poll_id = (SELECT game_polls._id FROM game_polls WHERE game_id=? AND poll_name=?)", "" + gameId, pollName);
			}
			case GAMES_ID_POLLS_NAME_RESULTS_KEY: {
				final int gameId = Games.getGameId(uri);
				final String pollName = Games.getPollName(uri);
				final String key = Games.getPollResultsKey(uri);
				return builder.table(Tables.GAME_POLL_RESULTS)
					.mapToTable(BaseColumns._ID, Tables.GAME_POLL_RESULTS)
					.where("poll_id = (SELECT game_polls._id FROM game_polls WHERE game_id=? AND poll_name=?)", "" + gameId, pollName)
					.where(GamePollResults.POLL_RESULTS_PLAYERS + "=?", key);
			}
			case GAMES_ID_POLLS_NAME_RESULTS_KEY_RESULT: {
				final int gameId = Games.getGameId(uri);
				final String pollName = Games.getPollName(uri);
				final String key = Games.getPollResultsKey(uri);
				return builder.table(Tables.GAME_POLL_RESULTS_RESULT)
					.mapToTable(BaseColumns._ID, Tables.GAME_POLL_RESULTS)
					.where("game_poll_results._id FROM game_poll_results WHERE game_poll_results.poll_id =(SELECT game_poll_results._id FROM game_poll_results WHERE game_poll_results.poll_id = (SELECT game_polls._id FROM game_polls WHERE game_id=? AND poll_name=?)", "" + gameId, pollName)
					.where(GamePollResults.POLL_RESULTS_KEY + "=?", key);
			}
			case GAMES_ID_POLLS_NAME_RESULTS_KEY_RESULT_KEY: {
				final int gameId = Games.getGameId(uri);
				final String pollName = Games.getPollName(uri);
				final String key = Games.getPollResultsKey(uri);
				final String key2 = Games.getPollResultsResultKey(uri);
				return builder.table(Tables.GAME_POLL_RESULTS_RESULT)
					.mapToTable(BaseColumns._ID, Tables.GAME_POLL_RESULTS)
					.where("pollresults_id = (SELECT game_poll_results._id FROM game_poll_results WHERE game_poll_results.pollresults_key=? AND game_poll_results.poll_id =(SELECT game_poll_results._id FROM game_poll_results WHERE game_poll_results.poll_id = (SELECT game_polls._id FROM game_polls WHERE game_id=? AND poll_name=?)))", key, "" + gameId, pollName)
					.where(GamePollResultsResult.POLL_RESULTS_RESULT_KEY + "=?", key2);
			}
			default:
				throw new UnsupportedOperationException("Unknown uri: " + uri);
		}
	}

	private SelectionBuilder buildExpandedSelection(Uri uri, int match) {
		final SelectionBuilder builder = new SelectionBuilder();
		switch (match) {
			case COLLECTION:
				return builder.table(Tables.COLLECTION_JOIN_GAMES)
					.mapToTable(Collection._ID, Tables.COLLECTION)
					.mapToTable(Collection.GAME_ID, Tables.COLLECTION);
			case COLLECTION_ID:
				final int itemId = Collection.getItemId(uri);
				return builder.table(Tables.COLLECTION_JOIN_GAMES)
					.mapToTable(Collection._ID, Tables.COLLECTION)
					.mapToTable(Collection.GAME_ID, Tables.COLLECTION)
					.where(Tables.COLLECTION + "." + Collection.COLLECTION_ID + "=?", "" + itemId);
			case GAMES_ID_DESIGNERS: {
				final int gameId = Games.getGameId(uri);
				return builder.table(Tables.GAMES_DESIGNERS_JOIN_DESIGNERS)
					.mapToTable(Designers._ID, Tables.DESIGNERS)
					.mapToTable(Designers.DESIGNER_ID, Tables.DESIGNERS)
					.mapToTable(SyncColumns.UPDATED, Tables.DESIGNERS)
					.where(Qualified.GAMES_DESIGNERS_GAME_ID + "=?", "" + gameId);
			}
			case GAMES_ID_ARTISTS: {
				final int gameId = Games.getGameId(uri);
				return builder.table(Tables.GAMES_ARTISTS_JOIN_ARTISTS)
					.mapToTable(Artists._ID, Tables.ARTISTS)
					.mapToTable(Artists.ARTIST_ID, Tables.ARTISTS)
					.mapToTable(SyncColumns.UPDATED, Tables.ARTISTS)
					.where(Qualified.GAMES_ARTISTS_GAME_ID + "=?", "" + gameId);
			}
			case GAMES_ID_PUBLISHERS: {
				final int gameId = Games.getGameId(uri);
				return builder.table(Tables.GAMES_PUBLISHERS_JOIN_PUBLISHERS)
					.mapToTable(Publishers._ID, Tables.PUBLISHERS)
					.mapToTable(Publishers.PUBLISHER_ID, Tables.PUBLISHERS)
					.mapToTable(SyncColumns.UPDATED, Tables.PUBLISHERS)
					.where(Qualified.GAMES_PUBLISHERS_GAME_ID + "=?", "" + gameId);
			}
			case GAMES_ID_MECHANICS: {
				final int gameId = Games.getGameId(uri);
				return builder.table(Tables.GAMES_MECHANICS_JOIN_MECHANICS)
					.mapToTable(Mechanics._ID, Tables.MECHANICS)
					.mapToTable(Mechanics.MECHANIC_ID, Tables.MECHANICS)
					.where(Qualified.GAMES_MECHANICS_GAME_ID + "=?", "" + gameId);
			}
			case GAMES_ID_CATEGORIES: {
				final int gameId = Games.getGameId(uri);
				return builder.table(Tables.GAMES_CATEGORIES_JOIN_CATEGORIES)
					.mapToTable(Categories._ID, Tables.CATEGORIES)
					.mapToTable(Categories.CATEGORY_ID, Tables.CATEGORIES)
					.where(Qualified.GAMES_CATEGORIES_GAME_ID + "=?", "" + gameId);
			}
			case GAMES_ID_POLLS_NAME_RESULTS: {
				final int gameId = Games.getGameId(uri);
				final String pollName = Games.getPollName(uri);
				return builder.table(Tables.POLLS_JOIN_POLL_RESULTS)
					.mapToTable(BaseColumns._ID, Tables.GAME_POLL_RESULTS)
					.where(GamePolls.GAME_ID + "=?", "" + gameId)
					.where(GamePolls.POLL_NAME + "=?", pollName);
			}
			case GAMES_ID_POLLS_NAME_RESULTS_KEY: {
				final int gameId = Games.getGameId(uri);
				final String pollName = Games.getPollName(uri);
				final String players = Games.getPollResultsKey(uri);
				return builder.table(Tables.POLLS_JOIN_POLL_RESULTS)
					.mapToTable(BaseColumns._ID, Tables.GAME_POLL_RESULTS)
					.where(GamePolls.GAME_ID + "=?", "" + gameId)
					.where(GamePolls.POLL_NAME + "=?", pollName)
					.where(GamePollResults.POLL_RESULTS_PLAYERS + "=?", players);
			}
			case GAMES_ID_POLLS_NAME_RESULTS_KEY_RESULT: {
				final int gameId = Games.getGameId(uri);
				final String pollName = Games.getPollName(uri);
				final String players = Games.getPollResultsKey(uri);
				return builder.table(Tables.POLL_RESULTS_JOIN_POLL_RESULTS_RESULT)
					.mapToTable(BaseColumns._ID, Tables.GAME_POLL_RESULTS_RESULT)
					.where("poll_id = (SELECT game_polls._id FROM game_polls WHERE game_id=? AND poll_name=?)", "" + gameId, pollName)
					.where(GamePollResults.POLL_RESULTS_PLAYERS + "=?", players);
			}
			default:
				return buildSimpleSelection(uri, match);
		}
	}

	private void deleteGameChildren(final SQLiteDatabase db, final SelectionBuilder builder) {
		Cursor c = builder.query(db, new String[] { Games.GAME_ID }, null);
		try {
			ContentResolver cr = getContext().getContentResolver();
			while (c.moveToNext()) {
				int gameId = c.getInt(0);
				String[] gameArg = new String[] { "" + gameId };
				cr.delete(GameRanks.CONTENT_URI, GameRanks.GAME_ID + "=?", gameArg);
				cr.delete(Collection.CONTENT_URI, Collection.GAME_ID + "=?", gameArg);
				cr.delete(Games.buildDesignersUri(gameId), null, null);
				cr.delete(Games.buildArtistsUri(gameId), null, null);
				cr.delete(Games.buildPublishersUri(gameId), null, null);
				cr.delete(Games.buildMechanicsUri(gameId), null, null);
				cr.delete(Games.buildCategoriesUri(gameId), null, null);
				//cr.delete(Games.buildPollsUri(gameId), null, null);
				// TODO: delete pollResults and pollResult rows
				//db.delete(Tables.GAME_POLL_RESULTS_RESULT,
				//	"pollresults_id IN (SELECT game_poll_results._id from game_poll_results WHERE game_poll_results.poll_id IN (SELECT game_polls._id FROM game_polls WHERE game_id=?))",
				//	new String[] { "" + gameId });
				//db.delete(Tables.POLLS_JOIN_POLL_RESULTS, GamePolls.GAME_ID + "=?", new String[] { "" + gameId });
				//db.delete(Tables.GAME_POLLS, GamePolls.GAME_ID + "=?", new String[] { "" + gameId });
			}
		} finally {
			c.close();
		}
	}
}
