package com.boardgamegeek.provider;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.HashMap;

import android.annotation.SuppressLint;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import com.boardgamegeek.provider.BggContract.Thumbnails;
import com.boardgamegeek.util.ImageCache;
import com.boardgamegeek.util.SelectionBuilder;

public class BggProvider extends ContentProvider {
	private static final String TAG = "BggProvider";
	private static final boolean LOGV = Log.isLoggable(TAG, Log.VERBOSE);

	private BggDatabase mOpenHelper;

	private static UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
	private static HashMap<Integer, BaseProvider> providers = buildProviderMap();
	private static int sCode = 1;

	private static void addProvider(HashMap<Integer, BaseProvider> map, BaseProvider provider) {
		sCode++;
		sUriMatcher.addURI(BggContract.CONTENT_AUTHORITY, provider.getPath(), sCode);
		map.put(sCode, provider);
	}

	@SuppressLint("UseSparseArrays")
	private static HashMap<Integer, BaseProvider> buildProviderMap() {
		HashMap<Integer, BaseProvider> map = new HashMap<Integer, BaseProvider>();

		addProvider(map, new GamesProvider());
		addProvider(map, new GamesIdProvider());
		addProvider(map, new GamesIdRankProvider());
		addProvider(map, new GamesIdRankIdProvider());
		addProvider(map, new GamesIdExpansionsProvider());
		addProvider(map, new GamesIdExpansionsIdProvider());
		addProvider(map, new GamesIdDesignersProvider());
		addProvider(map, new GamesIdDesignersIdProvider());
		addProvider(map, new GamesIdArtistsProvider());
		addProvider(map, new GamesIdArtistsIdProvider());
		addProvider(map, new GamesIdPublishersProvider());
		addProvider(map, new GamesIdPublishersIdProvider());

		addProvider(map, new GamesIdCategoriesProvider());
		addProvider(map, new GamesIdCategoriesIdProvider());
		addProvider(map, new GamesIdMechanicsProvider());
		addProvider(map, new GamesIdMechanicsIdProvider());

		addProvider(map, new GamesRanksProvider());
		addProvider(map, new GamesRanksIdProvider());

		addProvider(map, new GamesDesignersIdProvider());
		addProvider(map, new GamesArtistsIdProvider());
		addProvider(map, new GamesPublishersIdProvider());
		addProvider(map, new GamesMechanicsIdProvider());
		addProvider(map, new GamesCategoriesIdProvider());

		addProvider(map, new GamesIdPollsProvider());
		addProvider(map, new GamesIdPollsNameProvider());
		addProvider(map, new GamesIdPollsNameResultsProvider());
		addProvider(map, new GamesIdPollsNameResultsKeyProvider());
		addProvider(map, new GamesIdPollsNameResultsKeyResultProvider());
		addProvider(map, new GamesIdPollsNameResultsKeyResultKeyProvider());

		addProvider(map, new GamesIdColorsProvider());
		addProvider(map, new GamesIdColorsNameProvider());

		addProvider(map, new DesignersProvider());
		addProvider(map, new DesignersIdProvider());
		addProvider(map, new ArtistsProvider());
		addProvider(map, new ArtistsIdProvider());
		addProvider(map, new PublishersProvider());
		addProvider(map, new PublishersIdProvider());
		addProvider(map, new MechanicsProvider());
		addProvider(map, new MechanicsIdProvider());
		addProvider(map, new CategoriesProvider());
		addProvider(map, new CategoriesIdProvider());

		addProvider(map, new CollectionProvider());
		addProvider(map, new CollectionIdProvider());

		addProvider(map, new PlaysProvider());
		addProvider(map, new PlaysIdProvider());
		addProvider(map, new PlaysIdItemsProvider());
		addProvider(map, new PlaysIdItemsIdProvider());
		addProvider(map, new PlaysIdPlayersProvider());
		addProvider(map, new PlaysIdPlayersIdProvider());
		addProvider(map, new PlaysGamesId());
		addProvider(map, new PlaysLocationsProvider());

		addProvider(map, new CollectionFiltersProvider());
		addProvider(map, new CollectionFiltersIdProvider());
		addProvider(map, new CollectionFiltersIdDetailsProvider());
		addProvider(map, new CollectionFiltersIdDetailsIdProvider());

		addProvider(map, new BuddiesProvider());
		addProvider(map, new BuddiesIdProvider());

		addProvider(map, new SearchSuggestProvider());
		addProvider(map, new SearchSuggestTextProvider());
		addProvider(map, new SearchRefreshProvider());

		return map;
	}

	@Override
	public boolean onCreate() {
		mOpenHelper = new BggDatabase(getContext());
		return true;
	}

	@Override
	public String getType(Uri uri) {
		return getProvider(uri).getType(uri);
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		if (LOGV) {
			Log.v(TAG, "query(uri=" + uri + ", proj=" + Arrays.toString(projection) + ")");
		}
		SQLiteDatabase db = mOpenHelper.getReadableDatabase();
		return getProvider(uri).query(getContext().getContentResolver(), db, uri, projection, selection, selectionArgs,
				sortOrder);
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		if (LOGV) {
			Log.v(TAG, "insert(uri=" + uri + ", values=" + values.toString() + ")");
		}

		SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		Uri newUri = getProvider(uri).insert(db, uri, values);
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

		SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		int rowCount = getProvider(uri).buildSimpleSelection(uri).where(selection, selectionArgs).update(db, values);

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

		BaseProvider provider = getProvider(uri);
		SelectionBuilder builder = provider.buildSimpleSelection(uri).where(selection, selectionArgs);

		SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		provider.deleteChildren(db, builder);
		int rowCount = builder.delete(db);

		if (LOGV) {
			Log.v(TAG, "deleted " + rowCount + " rows");
		}

		getContext().getContentResolver().notifyChange(uri, null);

		return rowCount;
	}

	@Override
	public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
		Log.d(TAG, "Open file: " + uri);

		if (!uri.toString().startsWith(Thumbnails.CONTENT_URI.toString())) {
			return null;
		}

		// TODO: fix this to not include the entire thumbnail URL in the URI
		String fileName = uri.getLastPathSegment();
		final File file = ImageCache.getExistingImageFile(fileName);
		if (file != null) {
			return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
		}
		return null;
	}

	private BaseProvider getProvider(Uri uri) {
		int match = sUriMatcher.match(uri);
		if (providers.containsKey(match)) {
			return providers.get(match);
		}
		throw new UnsupportedOperationException("Unknown uri: " + uri);
	}
}
