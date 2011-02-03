package com.boardgamegeek.provider;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.HashMap;

import android.app.SearchManager;
import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.text.TextUtils;
import android.util.Log;

import com.boardgamegeek.provider.BggContract.Buddies;
import com.boardgamegeek.provider.BggContract.Collection;
import com.boardgamegeek.provider.BggContract.Designers;
import com.boardgamegeek.provider.BggContract.GameRanks;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.provider.BggContract.SyncColumns;
import com.boardgamegeek.provider.BggContract.SyncListColumns;
import com.boardgamegeek.provider.BggContract.Thumbnails;
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

	private static final int GAMES = 100;
	private static final int GAMES_ID = 101;
	private static final int GAMES_RANKS = 102;
	private static final int GAMES_RANKS_ID = 103;
	private static final int GAMES_ID_RANKS = 104;
	private static final int DESIGNERS = 110;
	private static final int DESIGNERS_ID = 111;
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
		matcher.addURI(authority, "designers", DESIGNERS);
		matcher.addURI(authority, "designers/#", DESIGNERS_ID);
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
			case DESIGNERS:
				return Designers.CONTENT_TYPE;
			case DESIGNERS_ID:
				return Designers.CONTENT_ITEM_TYPE;
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
		Uri newUri;
		switch (match) {
			case GAMES: {
				db.insertOrThrow(Tables.GAMES, null, values);
				newUri = Games.buildGameUri(values.getAsInteger(Games.GAME_ID));
				break;
			}
			case GAMES_ID_RANKS: {
				final int gameId = Games.getGameId(uri);
				values.put(GameRanks.GAME_ID, gameId);
				final long gameRankId = db.insertOrThrow(Tables.GAME_RANKS, null, values);
				// TODO: use a method from GameRanks
				newUri = ContentUris.withAppendedId(GameRanks.CONTENT_URI, gameRankId);
				break;
			}
			case DESIGNERS: {
				db.insertOrThrow(Tables.DESIGNERS, null, values);
				newUri = Designers.buildDesignerUri(values.getAsInteger(Designers.DESIGNER_ID));
				break;
			}
			case COLLECTION: {
				db.insertOrThrow(Tables.COLLECTION, null, values);
				newUri = Collection.buildItemUri(values.getAsInteger(Collection.COLLECTION_ID));
				break;
			}
			case BUDDIES: {
				db.insertOrThrow(Tables.BUDDIES, null, values);
				newUri = Buddies.buildBuddyUri(values.getAsInteger(Buddies.BUDDY_ID));
				break;
			}
			default: {
				throw new UnsupportedOperationException("Unknown uri: " + uri);
			}
		}
		getContext().getContentResolver().notifyChange(newUri, null);
		return uri;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		if (LOGV) {
			Log.v(TAG, "update(uri=" + uri + ", values=" + values.toString() + ")");
		}

		final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		final SelectionBuilder builder = buildSimpleSelection(uri);
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
		final SelectionBuilder builder = buildSimpleSelection(uri).where(selection, selectionArgs);

		final int match = sUriMatcher.match(uri);
		if (match == GAMES_ID || match == GAMES) {
			deleteGameChildren(db, builder);
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

	private SelectionBuilder buildSimpleSelection(Uri uri) {
		final SelectionBuilder builder = new SelectionBuilder();
		final int match = sUriMatcher.match(uri);

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
				return builder.table(Tables.GAME_RANKS).where(Games.GAME_ID + "=?", "" + gameId);
			}
			case DESIGNERS:
				return builder.table(Tables.DESIGNERS);
			case DESIGNERS_ID:
				final int designerId = Designers.getDesignerId(uri);
				return builder.table(Tables.DESIGNERS).where(Designers.DESIGNER_ID + "=?", "" + designerId);
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
			default:
				throw new UnsupportedOperationException("Unknown uri: " + uri);
		}
	}

	private SelectionBuilder buildExpandedSelection(Uri uri, int match) {
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
				// TODO: join with game table?
			}
			case GAMES_ID_RANKS: {
				final int gameId = Games.getGameId(uri);
				return builder.table(Tables.GAME_RANKS).where(Games.GAME_ID + "=?", "" + gameId);
				// TODO: join with game table?
			}
			case DESIGNERS:
				return builder.table(Tables.DESIGNERS);
			case DESIGNERS_ID:
				final int designerId = Designers.getDesignerId(uri);
				return builder.table(Tables.DESIGNERS).where(Designers.DESIGNER_ID + "=?", "" + designerId);
			case COLLECTION:
				return builder.table(Tables.COLLECTION_JOIN_GAMES).mapToTable(Collection._ID, Tables.COLLECTION)
						.mapToTable(Collection.GAME_ID, Tables.COLLECTION);
			case COLLECTION_ID:
				final int itemId = Collection.getItemId(uri);
				return builder.table(Tables.COLLECTION_JOIN_GAMES).mapToTable(Collection._ID, Tables.COLLECTION)
						.mapToTable(Collection.GAME_ID, Tables.COLLECTION)
						.where(Tables.COLLECTION + "." + Collection.COLLECTION_ID + "=?", "" + itemId);
			case BUDDIES:
				return builder.table(Tables.BUDDIES);
			case BUDDIES_ID:
				final int blockId = Buddies.getBuddyId(uri);
				return builder.table(Tables.BUDDIES).where(Buddies.BUDDY_ID + "=?", "" + blockId);
			default:
				throw new UnsupportedOperationException("Unknown uri: " + uri);
		}
	}

	private void deleteGameChildren(final SQLiteDatabase db, final SelectionBuilder builder) {
		Cursor c = builder.query(db, new String[] { Games.GAME_ID }, null);
		try {
			while (c.moveToNext()) {
				int gameId = c.getInt(0);
				String[] gameArg = new String[] { "" + gameId };
				getContext().getContentResolver().delete(GameRanks.CONTENT_URI, GameRanks.GAME_ID + "=?", gameArg);
				getContext().getContentResolver().delete(Collection.CONTENT_URI, Collection.GAME_ID + "=?", gameArg);
			}
		} finally {
			c.close();
		}
	}
}
