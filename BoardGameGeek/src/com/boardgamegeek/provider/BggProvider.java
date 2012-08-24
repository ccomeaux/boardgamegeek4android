package com.boardgamegeek.provider;

import static com.boardgamegeek.util.LogUtils.LOGD;
import static com.boardgamegeek.util.LogUtils.LOGE;
import static com.boardgamegeek.util.LogUtils.LOGV;
import static com.boardgamegeek.util.LogUtils.makeLogTag;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
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

import com.boardgamegeek.database.ResolverUtils;
import com.boardgamegeek.provider.BggContract.Buddies;
import com.boardgamegeek.provider.BggContract.Collection;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.provider.BggContract.Thumbnails;
import com.boardgamegeek.util.ImageCache;
import com.boardgamegeek.util.SelectionBuilder;

public class BggProvider extends ContentProvider {
	private static final String TAG = makeLogTag(BggProvider.class);

	private BggDatabase mOpenHelper;

	private static UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
	private static HashMap<Integer, BaseProvider> providers = buildProviderMap();
	private static UriMatcher sFileUriMatcher = createFileUriMatcher();
	private static int sCode = 1;

	private static final int CODE_GAMES_ID_THUMBNAIL = 1001;
	private static final int CODE_COLLECTION_ID_THUMBNAIL = 1002;
	private static final int CODE_BUDDIES_ID_AVATAR = 1003;

	private static UriMatcher createFileUriMatcher() {
		UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
		matcher.addURI(BggContract.CONTENT_AUTHORITY, "games/#/thumbnails", CODE_GAMES_ID_THUMBNAIL);
		matcher.addURI(BggContract.CONTENT_AUTHORITY, "collection/#/thumbnails", CODE_COLLECTION_ID_THUMBNAIL);
		matcher.addURI(BggContract.CONTENT_AUTHORITY, "buddies/#/avatars", CODE_BUDDIES_ID_AVATAR);
		return matcher;
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
		addProvider(map, new CollectionExpansionsProvider());
		addProvider(map, new CollectionNoExpansionsProvider());

		addProvider(map, new PlaysProvider());
		addProvider(map, new PlaysIdProvider());
		addProvider(map, new PlaysIdItemsProvider());
		addProvider(map, new PlaysIdItemsIdProvider());
		addProvider(map, new PlaysIdPlayersProvider());
		addProvider(map, new PlaysIdPlayersIdProvider());
		addProvider(map, new PlaysGamesId());
		addProvider(map, new PlaysLocationsProvider());

		addProvider(map, new CollectionViewProvider());
		addProvider(map, new CollectionViewIdProvider());
		addProvider(map, new CollectionViewIdFiltersProvider());
		addProvider(map, new CollectionViewIdFiltersIdProvider());

		addProvider(map, new BuddiesProvider());
		addProvider(map, new BuddiesIdProvider());

		addProvider(map, new SearchSuggestProvider());
		addProvider(map, new SearchSuggestTextProvider());
		addProvider(map, new SearchRefreshProvider());

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
		LOGV(TAG, "query(uri=" + uri + ", proj=" + Arrays.toString(projection) + ")");
		SQLiteDatabase db = mOpenHelper.getReadableDatabase();
		return getProvider(uri).query(getContext().getContentResolver(), db, uri, projection, selection, selectionArgs,
			sortOrder);
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		LOGV(TAG, "insert(uri=" + uri + ", values=" + values.toString() + ")");

		SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		Uri newUri = getProvider(uri).insert(db, uri, values);
		if (newUri != null) {
			getContext().getContentResolver().notifyChange(newUri, null);
		}
		return newUri;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		LOGV(TAG, "update(uri=" + uri + ", values=" + values.toString() + ")");

		SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		int rowCount = getProvider(uri).buildSimpleSelection(uri).where(selection, selectionArgs).update(db, values);

		LOGV(TAG, "updated " + rowCount + " rows");

		getContext().getContentResolver().notifyChange(uri, null);

		return rowCount;
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		LOGV(TAG, "delete(uri=" + uri + ")");

		BaseProvider provider = getProvider(uri);
		SelectionBuilder builder = provider.buildSimpleSelection(uri).where(selection, selectionArgs);

		int rowCount = builder.delete(mOpenHelper.getWritableDatabase());
		LOGV(TAG, "deleted " + rowCount + " rows");

		getContext().getContentResolver().notifyChange(uri, null);

		return rowCount;
	}

	@Override
	public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
		LOGD(TAG, "Open file: " + uri);

		File file = null;
		int match = sFileUriMatcher.match(uri);
		switch (match) {
			case CODE_GAMES_ID_THUMBNAIL:
				file = ImageCache.getExistingImageFile(fetchFileName(Games.buildGameUri(Games.getGameId(uri)),
					Games.THUMBNAIL_URL));
				break;
			case CODE_COLLECTION_ID_THUMBNAIL:
				file = new File(generateContentPath(BggContract.PATH_THUMBNAILS), fetchFileName(
					Collection.buildItemUri(Collection.getItemId(uri)), Collection.THUMBNAIL_URL));
				break;
			case CODE_BUDDIES_ID_AVATAR:
				file = new File(generateContentPath(BggContract.PATH_AVATARS), fetchFileName(
					Buddies.buildBuddyUri(Buddies.getBuddyId(uri)), Buddies.AVATAR_URL));
				break;
			default:
				if (uri.toString().startsWith(Thumbnails.CONTENT_URI.toString())) {
					file = ImageCache.getExistingImageFile(uri.getLastPathSegment());
				}
				break;
		}
		if (file == null) {
			return null;
		}

		if (!file.exists()) {
			try {
				if (!file.createNewFile()) {
					throw new FileNotFoundException();
				}
			} catch (IOException e) {
				LOGE(TAG, "Error creating a new file.", e);
				throw new FileNotFoundException();
			}
		}

		int parcelMode = calculatParcelMode(uri, mode);
		return ParcelFileDescriptor.open(file, parcelMode);
	}

	private String fetchFileName(Uri uri, String columnName) {
		String path = ResolverUtils.queryString(getContext().getContentResolver(), uri, columnName);
		int i = path.lastIndexOf(File.separator);
		if (i > 0) {
			return path.substring(i + 1);
		}
		return null;
	}

	private String generateContentPath(String type) {
		String path = getContext().getExternalFilesDir(null).getPath() + File.separator + "content" + File.separator
			+ type;
		File folder = new File(path);
		if (!folder.exists()) {
			folder.mkdirs();
		}
		return path;
	}

	// from Android ContentResolver.modeToMode
	private static int calculatParcelMode(Uri uri, String mode) throws FileNotFoundException {
		int modeBits;
		if ("r".equals(mode)) {
			modeBits = ParcelFileDescriptor.MODE_READ_ONLY;
		} else if ("w".equals(mode) || "wt".equals(mode)) {
			modeBits = ParcelFileDescriptor.MODE_WRITE_ONLY | ParcelFileDescriptor.MODE_CREATE
				| ParcelFileDescriptor.MODE_TRUNCATE;
		} else if ("wa".equals(mode)) {
			modeBits = ParcelFileDescriptor.MODE_WRITE_ONLY | ParcelFileDescriptor.MODE_CREATE
				| ParcelFileDescriptor.MODE_APPEND;
		} else if ("rw".equals(mode)) {
			modeBits = ParcelFileDescriptor.MODE_READ_WRITE | ParcelFileDescriptor.MODE_CREATE;
		} else if ("rwt".equals(mode)) {
			modeBits = ParcelFileDescriptor.MODE_READ_WRITE | ParcelFileDescriptor.MODE_CREATE
				| ParcelFileDescriptor.MODE_TRUNCATE;
		} else {
			throw new FileNotFoundException("Bad mode for " + uri + ": " + mode);
		}
		return modeBits;
	}

	private BaseProvider getProvider(Uri uri) {
		int match = sUriMatcher.match(uri);
		if (providers.containsKey(match)) {
			return providers.get(match);
		}
		throw new UnsupportedOperationException("Unknown uri: " + uri);
	}
}
