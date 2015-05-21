package com.boardgamegeek.provider;

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

import timber.log.Timber;

public class BggProvider extends ContentProvider {
	private BggDatabase mOpenHelper;
	private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
	private static final HashMap<Integer, BaseProvider> providers = buildProviderMap();
	private static int sCode = 1;

	@SuppressLint("UseSparseArrays")
	private static HashMap<Integer, BaseProvider> buildProviderMap() {
		HashMap<Integer, BaseProvider> map = new HashMap<>();

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
		addProvider(map, new GamesIdPollsNameResultsResultProvider());
		addProvider(map, new GamesIdPollsNameResultsKeyProvider());
		addProvider(map, new GamesIdPollsNameResultsKeyResultProvider());
		addProvider(map, new GamesIdPollsNameResultsKeyResultKeyProvider());

		addProvider(map, new GamesIdColorsProvider());
		addProvider(map, new GamesIdColorsNameProvider());

		addProvider(map, new GamesIdPlaysProvider());

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
		addProvider(map, new PlaysLocationsProvider());
		addProvider(map, new PlaysPlayersProvider());

		addProvider(map, new CollectionViewProvider());
		addProvider(map, new CollectionViewIdProvider());
		addProvider(map, new CollectionViewIdFiltersProvider());
		addProvider(map, new CollectionViewIdFiltersIdProvider());

		addProvider(map, new BuddiesProvider());
		addProvider(map, new BuddiesIdProvider());

		addProvider(map, new ThumbnailsProvider());
		addProvider(map, new ThumbnailsIdProvider());
		addProvider(map, new GamesIdThumbnailProvider());
		addProvider(map, new CollectionIdThumbnailProvider());
		addProvider(map, new AvatarsProvider());
		addProvider(map, new AvatarsIdProvider());
		addProvider(map, new BuddiesIdAvatarProvider());

		addProvider(map, new SearchSuggestProvider());
		addProvider(map, new SearchSuggestTextProvider());
		addProvider(map, new SearchRefreshProvider());

		addProvider(map, new PlayerColorsProvider());
		addProvider(map, new UsersNameColorsProvider());
		addProvider(map, new UsersNameColorsOrderProvider());

		return map;
	}

	private static void addProvider(HashMap<Integer, BaseProvider> map, BaseProvider provider) {
		sCode++;
		sUriMatcher.addURI(BggContract.CONTENT_AUTHORITY, provider.getPath(), sCode);
		map.put(sCode, provider);
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
		Timber.v("query(uri=" + uri + ", projection=" + Arrays.toString(projection) + ")");
		SQLiteDatabase db = mOpenHelper.getReadableDatabase();
		Cursor cursor = getProvider(uri).query(getContext().getContentResolver(), db, uri, projection, selection,
			selectionArgs, sortOrder);
		cursor.setNotificationUri(getContext().getContentResolver(), uri);
		return cursor;
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		Timber.v("insert(uri=" + uri + ", values=" + values.toString() + ")");

		SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		Uri newUri = getProvider(uri).insert(getContext(), db, uri, values);
		if (newUri != null) {
			getContext().getContentResolver().notifyChange(newUri, null);
		}
		return newUri;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		Timber.v("update(uri=" + uri + ", values=" + values.toString() + ")");
		int rowCount = getProvider(uri).update(getContext(), mOpenHelper.getWritableDatabase(), uri, values, selection,
			selectionArgs);
		Timber.v("updated " + rowCount + " rows");
		return rowCount;
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		Timber.v("delete(uri=" + uri + ")");
		BaseProvider provider = getProvider(uri);
		int rowCount = provider.delete(getContext(), mOpenHelper.getWritableDatabase(), uri, selection, selectionArgs);
		Timber.v("deleted " + rowCount + " rows");
		return rowCount;
	}

	@Override
	public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
		Timber.v("Open file: " + uri);

		BaseProvider provider = getProvider(uri);
		return provider.openFile(getContext(), uri, mode);
	}

	private BaseProvider getProvider(Uri uri) {
		int match = sUriMatcher.match(uri);
		if (providers.containsKey(match)) {
			return providers.get(match);
		}
		throw new UnsupportedOperationException("Unknown uri: " + uri);
	}
}
