package com.boardgamegeek.provider;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.HashMap;

import android.annotation.SuppressLint;
import android.app.SearchManager;
import android.content.ContentProvider;
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
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.provider.BggContract.Thumbnails;
import com.boardgamegeek.provider.BggDatabase.Tables;
import com.boardgamegeek.util.ImageCache;
import com.boardgamegeek.util.SelectionBuilder;

public class BggProvider extends ContentProvider {
	private static final String TAG = "BggProvider";
	private static final boolean LOGV = Log.isLoggable(TAG, Log.VERBOSE);

	private BggDatabase mOpenHelper;

	private static UriMatcher sUriMatcher = buildUriMatcher();
	private static HashMap<Integer, BaseProvider> providers = buildProviderMap();
	private static final HashMap<String, String> sSuggestionProjectionMap = buildSuggestionProjectionMap();

	private static final int BUDDIES = 1000;
	private static final int BUDDIES_ID = 1001;
	private static final int SEARCH_SUGGEST = 9998;
	private static final int SHORTCUT_REFRESH = 9999;

	private static UriMatcher buildUriMatcher() {
		final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
		final String authority = BggContract.CONTENT_AUTHORITY;

		matcher.addURI(authority, "buddies", BUDDIES);
		matcher.addURI(authority, "buddies/#", BUDDIES_ID);
		matcher.addURI(authority, SearchManager.SUGGEST_URI_PATH_QUERY, SEARCH_SUGGEST);
		matcher.addURI(authority, SearchManager.SUGGEST_URI_PATH_QUERY + "/*", SEARCH_SUGGEST);
		matcher.addURI(authority, SearchManager.SUGGEST_URI_PATH_SHORTCUT, SHORTCUT_REFRESH);
		matcher.addURI(authority, SearchManager.SUGGEST_URI_PATH_SHORTCUT + "/#", SHORTCUT_REFRESH);

		return matcher;
	}

	private static int code = 4000;

	private static void addProvider(HashMap<Integer, BaseProvider> map, UriMatcher matcher, BaseProvider provider) {
		code++;
		matcher.addURI(BggContract.CONTENT_AUTHORITY, provider.getPath(), code);
		map.put(code, provider);
	}

	@SuppressLint("UseSparseArrays")
	private static HashMap<Integer, BaseProvider> buildProviderMap() {
		HashMap<Integer, BaseProvider> map = new HashMap<Integer, BaseProvider>();

		addProvider(map, sUriMatcher, new GamesProvider());
		addProvider(map, sUriMatcher, new GamesIdProvider());
		addProvider(map, sUriMatcher, new GamesIdRankProvider());
		addProvider(map, sUriMatcher, new GamesIdRankIdProvider());
		addProvider(map, sUriMatcher, new GamesIdExpansionsProvider());
		addProvider(map, sUriMatcher, new GamesIdExpansionsIdProvider());
		addProvider(map, sUriMatcher, new GamesIdDesignersProvider());
		addProvider(map, sUriMatcher, new GamesIdDesignersIdProvider());
		addProvider(map, sUriMatcher, new GamesIdArtistsProvider());
		addProvider(map, sUriMatcher, new GamesIdArtistsIdProvider());
		addProvider(map, sUriMatcher, new GamesIdPublishersProvider());
		addProvider(map, sUriMatcher, new GamesIdPublishersIdProvider());

		addProvider(map, sUriMatcher, new GamesIdCategoriesProvider());
		addProvider(map, sUriMatcher, new GamesIdCategoriesIdProvider());
		addProvider(map, sUriMatcher, new GamesIdMechanicsProvider());
		addProvider(map, sUriMatcher, new GamesIdMechanicsIdProvider());

		addProvider(map, sUriMatcher, new GamesRanksProvider());
		addProvider(map, sUriMatcher, new GamesRanksIdProvider());

		addProvider(map, sUriMatcher, new GamesDesignersIdProvider());
		addProvider(map, sUriMatcher, new GamesArtistsIdProvider());
		addProvider(map, sUriMatcher, new GamesPublishersIdProvider());
		addProvider(map, sUriMatcher, new GamesMechanicsIdProvider());
		addProvider(map, sUriMatcher, new GamesCategoriesIdProvider());

		addProvider(map, sUriMatcher, new GamesIdPollsProvider());
		addProvider(map, sUriMatcher, new GamesIdPollsNameProvider());
		addProvider(map, sUriMatcher, new GamesIdPollsNameResultsProvider());
		addProvider(map, sUriMatcher, new GamesIdPollsNameResultsKeyProvider());
		addProvider(map, sUriMatcher, new GamesIdPollsNameResultsKeyResultProvider());
		addProvider(map, sUriMatcher, new GamesIdPollsNameResultsKeyResultKeyProvider());

		addProvider(map, sUriMatcher, new GamesIdColorsProvider());
		addProvider(map, sUriMatcher, new GamesIdColorsNameProvider());

		addProvider(map, sUriMatcher, new DesignersProvider());
		addProvider(map, sUriMatcher, new DesignersIdProvider());
		addProvider(map, sUriMatcher, new ArtistsProvider());
		addProvider(map, sUriMatcher, new ArtistsIdProvider());
		addProvider(map, sUriMatcher, new PublishersProvider());
		addProvider(map, sUriMatcher, new PublishersIdProvider());
		addProvider(map, sUriMatcher, new MechanicsProvider());
		addProvider(map, sUriMatcher, new MechanicsIdProvider());
		addProvider(map, sUriMatcher, new CategoriesProvider());
		addProvider(map, sUriMatcher, new CategoriesIdProvider());

		addProvider(map, sUriMatcher, new CollectionProvider());
		addProvider(map, sUriMatcher, new CollectionIdProvider());

		addProvider(map, sUriMatcher, new PlaysProvider());
		addProvider(map, sUriMatcher, new PlaysIdProvider());
		addProvider(map, sUriMatcher, new PlaysIdItemsProvider());
		addProvider(map, sUriMatcher, new PlaysIdItemsIdProvider());
		addProvider(map, sUriMatcher, new PlaysIdPlayersProvider());
		addProvider(map, sUriMatcher, new PlaysIdPlayersIdProvider());
		addProvider(map, sUriMatcher, new PlaysGamesId());
		addProvider(map, sUriMatcher, new PlaysLocationsProvider());

		addProvider(map, sUriMatcher, new CollectionFiltersProvider());
		addProvider(map, sUriMatcher, new CollectionFiltersIdProvider());
		addProvider(map, sUriMatcher, new CollectionFiltersIdDetailsProvider());
		addProvider(map, sUriMatcher, new CollectionFiltersIdDetailsIdProvider());

		return map;
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

		if (providers.containsKey(match)) {
			return providers.get(match).getType(uri);
		}

		switch (match) {
			case BUDDIES:
				return Buddies.CONTENT_TYPE;
			case BUDDIES_ID:
				return Buddies.CONTENT_ITEM_TYPE;
			case SEARCH_SUGGEST:
				return SearchManager.SUGGEST_MIME_TYPE;
			case SHORTCUT_REFRESH:
				return SearchManager.SHORTCUT_MIME_TYPE;
			default:
				throw new UnsupportedOperationException("Unknown uri getting type: " + uri);
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
					SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
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

		if (providers.containsKey(match)) {
			newUri = providers.get(match).insert(db, uri, values);
			if (newUri != null) {
				getContext().getContentResolver().notifyChange(newUri, null);
			}
			return newUri;
		}

		switch (match) {
			case BUDDIES: {
				db.insertOrThrow(Tables.BUDDIES, null, values);
				newUri = Buddies.buildBuddyUri(values.getAsInteger(Buddies.BUDDY_ID));
				break;
			}
			default: {
				throw new UnsupportedOperationException("Unknown uri inserting: " + uri);
			}
		}

		if (newUri != null) {
			getContext().getContentResolver().notifyChange(newUri, null);
		}
		return newUri;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		if (LOGV) {
			Log.v(TAG, "update(uri=" + uri + ", values=" + values.toString() + ")");
		}

		final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		final int match = sUriMatcher.match(uri);

		int rowCount = 0;
		if (providers.containsKey(match)) {
			rowCount = providers.get(match).buildSimpleSelection(uri).where(selection, selectionArgs)
					.update(db, values);
		} else {
			rowCount = buildSimpleSelection(uri, match).where(selection, selectionArgs).update(db, values);
		}

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

		int match = sUriMatcher.match(uri);
		SelectionBuilder builder = buildSimpleSelection(uri, match).where(selection, selectionArgs);

		SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		if (providers.containsKey(match)) {
			providers.get(match).deleteChildren(db, builder);
		}
		int rowCount = builder.delete(db);

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
		if (providers.containsKey(match)) {
			return providers.get(match).buildSimpleSelection(uri);
		}

		final SelectionBuilder builder = new SelectionBuilder();
		switch (match) {
			case BUDDIES:
				return builder.table(Tables.BUDDIES);
			case BUDDIES_ID:
				final int buddyId = Buddies.getBuddyId(uri);
				return builder.table(Tables.BUDDIES).where(Buddies.BUDDY_ID + "=?", String.valueOf(buddyId));
			default:
				throw new UnsupportedOperationException("Unknown uri: " + uri);
		}
	}

	private SelectionBuilder buildExpandedSelection(Uri uri, int match) {
		if (providers.containsKey(match)) {
			return providers.get(match).buildExpandedSelection(uri);
		}
		return buildSimpleSelection(uri, match);
	}
}
// TODO: improve the magical WHERE clauses with table and column constants - this should improve performance