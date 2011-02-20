package com.boardgamegeek.provider;

import android.net.Uri;
import android.net.Uri.Builder;
import android.provider.BaseColumns;

import com.boardgamegeek.Utility;

public class BggContract {

	public interface SyncColumns {
		String UPDATED = "updated";
	}

	public interface SyncListColumns {
		String UPDATED_LIST = "updated_list";
	}

	interface GamesColumns {
		String GAME_ID = "game_id";
		String GAME_NAME = "game_name";
		String GAME_SORT_NAME = "game_sort_name";
		String YEAR_PUBLISHED = "year_published";
		String IMAGE_URL = "image_url";
		String THUMBNAIL_URL = "thumbnail_url";
		String MIN_PLAYERS = "min_players";
		String MAX_PLAYERS = "max_players";
		String PLAYING_TIME = "playing_time";
		String NUM_OWNED = "num_owned";
		String NUM_PLAYS = "num_of_plays";
		String MINIMUM_AGE = "age";
		String DESCRIPTION = "description";
		String STATS_USERS_RATED = "usersrated";
		String STATS_AVERAGE = "average";
		String STATS_BAYES_AVERAGE = "bayes_average";
		String STATS_STANDARD_DEVIATION = "standard_deviation";
		String STATS_MEDIAN = "median";
		String STATS_NUMBER_OWNED = "number_owned";
		String STATS_NUMBER_TRADING = "number_trading";
		String STATS_NUMBER_WANTING = "number_wanting";
		String STATS_NUMBER_WISHING = "number_wishing";
		String STATS_NUMBER_COMMENTS = "number_commenting";
		String STATS_NUMBER_WEIGHTS = "number_weighting";
		String STATS_AVERAGE_WEIGHT = "average_weight";
	}

	interface GameRanksColumns {
		String GAME_RANK_ID = "gamerank_id";
		String GAME_RANK_TYPE = "gamerank_type";
		String GAME_RANK_NAME = "gamerank_name";
		String GAME_RANK_FRIENDLY_NAME = "gamerank_friendly_name";
		String GAME_RANK_VALUE = "gamerank_value";
		String GAME_RANK_BAYES_AVERAGE = "gamerank_bayes_average";
	}

	interface DesignersColumns {
		String DESIGNER_ID = "designer_id";
		String DESIGNER_NAME = "designer_name";
		String DESIGNER_DESCRIPTION = "designer_description";
	}

	interface ArtistsColumns {
		String ARTIST_ID = "artist_id";
		String ARTIST_NAME = "artist_name";
		String ARTIST_DESCRIPTION = "artist_description";
	}

	interface CollectionColumns {
		String COLLECTION_ID = "collection_id";
		String COLLECTION_NAME = "collection_name";
		String COLLECTION_SORT_NAME = "collection_sort_name";
		String STATUS_OWN = "own";
		String STATUS_PREVIOUSLY_OWNED = "previously_owned";
		String STATUS_FOR_TRADE = "for_trade";
		String STATUS_WANT = "want";
		String STATUS_WANT_TO_PLAY = "want_to_play";
		String STATUS_WANT_TO_BUY = "want_to_buy";
		String STATUS_WISHLIST = "wishlist";
		String STATUS_PREORDERED = "preordered";
		String COMMENT = "comment";
		String PRIVATE_INFO_PRICE_PAID_CURRENCY = "price_paid_currency";
		String PRIVATE_INFO_PRICE_PAID = "price_paid";
		String PRIVATE_INFO_CURRENT_VALUE_CURRENCY = "current_value_currency";
		String PRIVATE_INFO_CURRENT_VALUE = "current_value";
		String PRIVATE_INFO_QUANTITY = "quantity";
		String PRIVATE_INFO_ACQUISITION_DATE = "acquisition_date";
		String PRIVATE_INFO_ACQUIRED_FROM = "acquired_from";
		String PRIVATE_INFO_COMMENT = "private_comment";
	}

	interface BuddiesColumns {
		String BUDDY_ID = "buddy_id";
		String BUDDY_NAME = "buddy_name";
		String BUDDY_FIRSTNAME = "buddy_firtname";
		String BUDDY_LASTNAME = "buddy_lastname";
		String AVATAR_URL = "avatar_url";
	}

	public static final String CONTENT_AUTHORITY = "com.boardgamegeek";

	private static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);

	private static final String PATH_GAMES = "games";
	private static final String PATH_RANKS = "ranks";
	private static final String PATH_DESIGNERS = "designers";
	private static final String PATH_ARTISTS = "artists";
	private static final String PATH_COLLECTION = "collection";
	private static final String PATH_BUDDIES = "buddies";
	private static final String PATH_THUMBNAILS = "thumbnails";

	public static class Thumbnails {
		public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath(PATH_THUMBNAILS).build();
	}

	public static class Games implements GamesColumns, BaseColumns, SyncColumns, SyncListColumns {
		public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath(PATH_GAMES).build();

		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.boardgamegeek.game";
		public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.boardgamegeek.game";

		public static final String DEFAULT_SORT = GamesColumns.GAME_SORT_NAME + " COLLATE NOCASE ASC";

		public static Uri buildGameUri(int gameId) {
			return getUriBuilder(gameId).build();
		}

		public static Uri buildRanksUri(int gameId) {
			return getUriBuilder(gameId).appendPath(PATH_RANKS).build();
		}

		public static Uri buildDesignersUri(int gameId) {
			return getUriBuilder(gameId).appendPath(PATH_DESIGNERS).build();
		}
		
		public static Uri buildArtistsUri(int gameId) {
			return getUriBuilder(gameId).appendPath(PATH_ARTISTS).build();
		}

		private static Builder getUriBuilder(int gameId){
			return CONTENT_URI.buildUpon().appendPath("" + gameId);
		}

		public static int getGameId(Uri uri) {
			return Utility.parseInt(uri.getPathSegments().get(1));
		}
	}

	public static class GameRanks implements GameRanksColumns, GamesColumns, BaseColumns {
		public static final Uri CONTENT_URI = Games.CONTENT_URI.buildUpon().appendPath(PATH_RANKS).build();

		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.boardgamegeek.rank";
		public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.boardgamegeek.rank";

		public static final String DEFAULT_SORT = GameRanksColumns.GAME_RANK_TYPE + " DESC, "
				+ GameRanksColumns.GAME_RANK_FRIENDLY_NAME + " ASC";

		public static Uri buildGameRankUri(int gameRankId) {
			return CONTENT_URI.buildUpon().appendPath("" + gameRankId).build();
		}

		public static int getRankId(Uri uri) {
			return Utility.parseInt(uri.getPathSegments().get(1));
		}
	}

	public static class Designers implements DesignersColumns, BaseColumns, SyncColumns {
		public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath(PATH_DESIGNERS).build();

		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.boardgamegeek.designer";
		public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.boardgamegeek.designer";

		public static final String DEFAULT_SORT = DesignersColumns.DESIGNER_NAME + " COLLATE NOCASE ASC";

		public static Uri buildDesignerUri(int designerId) {
			return CONTENT_URI.buildUpon().appendPath("" + designerId).build();
		}
		
		public static int getDesignerId(Uri uri) {
			return Utility.parseInt(uri.getPathSegments().get(1));
		}
	}

	public static class Artists implements ArtistsColumns, BaseColumns, SyncColumns {
		public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath(PATH_ARTISTS).build();

		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.boardgamegeek.artist";
		public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.boardgamegeek.artist";

		public static final String DEFAULT_SORT = ArtistsColumns.ARTIST_NAME + " COLLATE NOCASE ASC";

		public static Uri buildArtistUri(int artistId) {
			return CONTENT_URI.buildUpon().appendPath("" + artistId).build();
		}
		
		public static int getArtistId(Uri uri) {
			return Utility.parseInt(uri.getPathSegments().get(1));
		}
	}

	public static class Collection implements CollectionColumns, GamesColumns, BaseColumns, SyncColumns, SyncListColumns {
		public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath(PATH_COLLECTION).build();

		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.boardgamegeek.collection";
		public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.boardgamegeek.collection";

		public static final String DEFAULT_SORT = CollectionColumns.COLLECTION_SORT_NAME + " COLLATE NOCASE ASC";

		public static Uri buildItemUri(int itemId) {
			return CONTENT_URI.buildUpon().appendPath("" + itemId).build();
		}

		public static int getItemId(Uri uri) {
			return Utility.parseInt(uri.getPathSegments().get(1));
		}
	}

	public static class Buddies implements BuddiesColumns, BaseColumns, SyncColumns, SyncListColumns {
		public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath(PATH_BUDDIES).build();

		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.boardgamegeek.buddy";
		public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.boardgamegeek.buddy";

		public static final String DEFAULT_SORT = BuddiesColumns.BUDDY_LASTNAME + " COLLATE NOCASE ASC, "
				+ BuddiesColumns.BUDDY_FIRSTNAME + " COLLATE NOCASE ASC";

		public static Uri buildBuddyUri(int buddyId) {
			return CONTENT_URI.buildUpon().appendPath("" + buddyId).build();
		}

		public static int getBuddyId(Uri uri) {
			return Utility.parseInt(uri.getPathSegments().get(1));
		}
	}

	private BggContract() {
	}
}
