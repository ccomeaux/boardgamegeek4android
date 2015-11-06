package com.boardgamegeek.provider;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.support.annotation.NonNull;
import android.support.v4.util.SimpleArrayMap;

import java.io.FileNotFoundException;

import hugo.weaving.DebugLog;

public class BggProvider extends ContentProvider {
	private static final UriMatcher URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
	private static final SimpleArrayMap<Integer, BaseProvider> PROVIDERS = buildProviderMap();
	private static int URI_MATCH_CODE = 1;
	private BggDatabase openHelper;

	@DebugLog
	private static SimpleArrayMap<Integer, BaseProvider> buildProviderMap() {
		SimpleArrayMap<Integer, BaseProvider> map = new SimpleArrayMap<>();

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

	@DebugLog
	private static void addProvider(SimpleArrayMap<Integer, BaseProvider> map, BaseProvider provider) {
		URI_MATCH_CODE++;
		URI_MATCHER.addURI(BggContract.CONTENT_AUTHORITY, provider.getPath(), URI_MATCH_CODE);
		map.put(URI_MATCH_CODE, provider);
	}

	@DebugLog
	@Override
	public boolean onCreate() {
		openHelper = new BggDatabase(getContext());
		return true;
	}

	@DebugLog
	@Override
	public String getType(@NonNull Uri uri) {
		return getProvider(uri).getType(uri);
	}

	@DebugLog
	@Override
	public Cursor query(@NonNull Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		SQLiteDatabase db = openHelper.getReadableDatabase();
		if (getContext() != null) {
			Cursor cursor = getProvider(uri).query(getContext().getContentResolver(), db, uri, projection, selection, selectionArgs, sortOrder);
			cursor.setNotificationUri(getContext().getContentResolver(), uri);
			return cursor;
		} else {
			return null;
		}
	}

	@DebugLog
	@Override
	public Uri insert(@NonNull Uri uri, ContentValues values) {
		SQLiteDatabase db = openHelper.getWritableDatabase();
		Uri newUri = getProvider(uri).insert(getContext(), db, uri, values);
		if (newUri != null & getContext() != null) {
			getContext().getContentResolver().notifyChange(newUri, null);
		}
		return newUri;
	}

	@DebugLog
	@Override
	public int update(@NonNull Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		return getProvider(uri).update(getContext(), openHelper.getWritableDatabase(), uri, values, selection, selectionArgs);
	}

	@DebugLog
	@Override
	public int delete(@NonNull Uri uri, String selection, String[] selectionArgs) {
		BaseProvider provider = getProvider(uri);
		return provider.delete(getContext(), openHelper.getWritableDatabase(), uri, selection, selectionArgs);
	}

	@DebugLog
	@Override
	public ParcelFileDescriptor openFile(@NonNull Uri uri, @NonNull String mode) throws FileNotFoundException {
		BaseProvider provider = getProvider(uri);
		return provider.openFile(getContext(), uri, mode);
	}

	@DebugLog
	private BaseProvider getProvider(Uri uri) {
		int match = URI_MATCHER.match(uri);
		if (PROVIDERS.containsKey(match)) {
			return PROVIDERS.get(match);
		}
		throw new UnsupportedOperationException("Unknown uri: " + uri);
	}
}
