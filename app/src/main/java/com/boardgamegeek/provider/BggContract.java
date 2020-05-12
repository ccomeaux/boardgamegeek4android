package com.boardgamegeek.provider;

import android.net.Uri;
import android.net.Uri.Builder;
import android.provider.BaseColumns;
import android.text.TextUtils;

import com.boardgamegeek.util.StringUtils;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class BggContract {

	public static final int INVALID_ID = -1;
	public static final String INVALID_URL = "N/A";

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
		String MIN_PLAYING_TIME = "min_playing_time";
		String MAX_PLAYING_TIME = "max_playing_time";
		String NUM_PLAYS = "num_of_plays";
		String MINIMUM_AGE = "age";
		String DESCRIPTION = "description";
		String SUBTYPE = "subtype";
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
		String LAST_VIEWED = "last_viewed";
		String STARRED = "starred";
		String UPDATED_PLAYS = "updated_plays";
		String CUSTOM_PLAYER_SORT = "custom_player_sort";
		String GAME_RANK = "game_rank";
		String SUGGESTED_PLAYER_COUNT_POLL_VOTE_TOTAL = "suggested_player_count_poll_vote_total";
		String PLAYER_COUNT_RECOMMENDATION_PREFIX = "player_count_recommendation_";
		String HERO_IMAGE_URL = "hero_image_url";
		String ICON_COLOR = "ICON_COLOR";
		String DARK_COLOR = "DARK_COLOR";
		String WINS_COLOR = "WINS_COLOR";
		String WINNABLE_PLAYS_COLOR = "WINNABLE_PLAYS_COLOR";
		String ALL_PLAYS_COLOR = "ALL_PLAYS_COLOR";
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
		String DESIGNER_IMAGE_URL = "designer_image_url";
		String DESIGNER_THUMBNAIL_URL = "designer_thumbnail_url";
		String DESIGNER_HERO_IMAGE_URL = "designer_hero_image_url";
		String DESIGNER_IMAGES_UPDATED_TIMESTAMP = "designer_images_updated_timestamp";
		String WHITMORE_SCORE = "whitmore_score";
		String DESIGNER_STATS_UPDATED_TIMESTAMP = "designer_stats_updated_timestamp";
		String ITEM_COUNT = "item_count";
	}

	interface ArtistsColumns {
		String ARTIST_ID = "artist_id";
		String ARTIST_NAME = "artist_name";
		String ARTIST_DESCRIPTION = "artist_description";
		String ARTIST_IMAGE_URL = "artist_image_url";
		String ARTIST_THUMBNAIL_URL = "artist_thumbnail_url";
		String ARTIST_HERO_IMAGE_URL = "artist_hero_image_url";
		String ARTIST_IMAGES_UPDATED_TIMESTAMP = "artist_images_updated_timestamp";
		String WHITMORE_SCORE = "whitmore_score";
		String ARTIST_STATS_UPDATED_TIMESTAMP = "artist_stats_updated_timestamp";
		String ITEM_COUNT = "item_count";
	}

	interface PublishersColumns {
		String PUBLISHER_ID = "publisher_id";
		String PUBLISHER_NAME = "publisher_name";
		String PUBLISHER_DESCRIPTION = "publisher_description";
		String PUBLISHER_IMAGE_URL = "publisher_image_url";
		String PUBLISHER_THUMBNAIL_URL = "publisher_thumbnail_url";
		String PUBLISHER_HERO_IMAGE_URL = "publisher_hero_image_url";
		String PUBLISHER_SORT_NAME = "publisher_sort_name";
		String WHITMORE_SCORE = "whitmore_score";
		String PUBLISHER_STATS_UPDATED_TIMESTAMP = "publisher_stats_updated_timestamp";
		String ITEM_COUNT = "item_count";
	}

	interface MechanicsColumns {
		String MECHANIC_ID = "mechanic_id";
		String MECHANIC_NAME = "mechanic_name";
		String ITEM_COUNT = "item_count";
	}

	interface CategoriesColumns {
		String CATEGORY_ID = "category_id";
		String CATEGORY_NAME = "category_name";
		String ITEM_COUNT = "item_count";
	}

	interface GamesExpansionsColumns {
		String EXPANSION_ID = "expansion_id";
		String EXPANSION_NAME = "expansion_name";
		String INBOUND = "inbound";
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
		String STATUS_WISHLIST_PRIORITY = "wishlist_priority";
		String STATUS_PREORDERED = "preordered";
		String COLLECTION_YEAR_PUBLISHED = "collection_year_published";
		String RATING = "rating";
		String COMMENT = "comment";
		String CONDITION = "conditiontext";
		String WANTPARTS_LIST = "wantpartslist";
		String HASPARTS_LIST = "haspartslist";
		String WISHLIST_COMMENT = "wishlistcomment";
		String COLLECTION_THUMBNAIL_URL = "collection_thumbnail_url";
		String COLLECTION_IMAGE_URL = "collection_image_url";
		String LAST_MODIFIED = "last_modified";
		String PRIVATE_INFO_PRICE_PAID_CURRENCY = "price_paid_currency";
		String PRIVATE_INFO_PRICE_PAID = "price_paid";
		String PRIVATE_INFO_CURRENT_VALUE_CURRENCY = "current_value_currency";
		String PRIVATE_INFO_CURRENT_VALUE = "current_value";
		String PRIVATE_INFO_QUANTITY = "quantity";
		String PRIVATE_INFO_ACQUISITION_DATE = "acquisition_date";
		String PRIVATE_INFO_ACQUIRED_FROM = "acquired_from";
		String PRIVATE_INFO_COMMENT = "private_comment";
		String STATUS_DIRTY_TIMESTAMP = "status_dirty_timestamp";
		String RATING_DIRTY_TIMESTAMP = "rating_dirty_timestamp";
		String COMMENT_DIRTY_TIMESTAMP = "comment_dirty_timestamp";
		String PRIVATE_INFO_DIRTY_TIMESTAMP = "private_info_dirty_timestamp";
		String COLLECTION_DIRTY_TIMESTAMP = "collection_dirty_timestamp";
		String COLLECTION_DELETE_TIMESTAMP = "collection_delete_timestamp";
		String WISHLIST_COMMENT_DIRTY_TIMESTAMP = "wishlist_comment_dirty_timestamp";
		String TRADE_CONDITION_DIRTY_TIMESTAMP = "trade_condition_dirty_timestamp";
		String WANT_PARTS_DIRTY_TIMESTAMP = "want_parts_dirty_timestamp";
		String HAS_PARTS_DIRTY_TIMESTAMP = "has_parts_dirty_timestamp";
		String COLLECTION_HERO_IMAGE_URL = "collection_hero_image_url";
		String PRIVATE_INFO_INVENTORY_LOCATION = "inventory_location";
	}

	interface BuddiesColumns {
		String BUDDY_ID = "buddy_id";
		String BUDDY_NAME = "buddy_name";
		String BUDDY_FIRSTNAME = "buddy_firtname";
		String BUDDY_LASTNAME = "buddy_lastname";
		String AVATAR_URL = "avatar_url";
		String PLAY_NICKNAME = "play_nickname";
		String BUDDY_FLAG = "buddy_flag";
		String SYNC_HASH_CODE = "sync_hash_code";
	}

	interface PlayerColorsColumns {
		String PLAYER_NAME = "player_name";
		String PLAYER_TYPE = "player_type";
		String PLAYER_COLOR = "player_color";
		String PLAYER_COLOR_SORT_ORDER = "player_color_sort";
	}

	interface GamePollsColumns {
		String POLL_NAME = "poll_name";
		String POLL_TITLE = "poll_title";
		String POLL_TOTAL_VOTES = "poll_total_votes";
	}

	interface GamePollResultsColumns {
		String POLL_ID = "poll_id";
		String POLL_RESULTS_KEY = "pollresults_key";
		String POLL_RESULTS_PLAYERS = "pollresults_players";
		String POLL_RESULTS_SORT_INDEX = "pollresults_sortindex";
	}

	interface GamePollResultsResultColumns {
		String POLL_RESULTS_ID = "pollresults_id";
		String POLL_RESULTS_RESULT_KEY = "pollresultsresult_key";
		String POLL_RESULTS_RESULT_LEVEL = "pollresultsresult_level";
		String POLL_RESULTS_RESULT_VALUE = "pollresultsresult_value";
		String POLL_RESULTS_RESULT_VOTES = "pollresultsresult_votes";
		String POLL_RESULTS_RESULT_SORT_INDEX = "pollresultsresult_sortindex";
	}

	interface GameSuggestedPlayerCountPollResultsColumns {
		String PLAYER_COUNT = "player_count";
		String SORT_INDEX = "sort_index";
		String BEST_VOTE_COUNT = "best_vote_count";
		String RECOMMENDED_VOTE_COUNT = "recommended_vote_count";
		String NOT_RECOMMENDED_VOTE_COUNT = "not_recommended_vote_count";
		String RECOMMENDATION = "recommendation";
	}

	interface GameColorsColumns {
		String COLOR = "color";
	}

	interface PlaysColumns {
		String PLAY_ID = "play_id";
		String DATE = "date";
		String QUANTITY = "quantity";
		String LENGTH = "length";
		String INCOMPLETE = "incomplete";
		String NO_WIN_STATS = "no_win_stats";
		String LOCATION = "location";
		String COMMENTS = "comments";
		String START_TIME = "start_time";
		String PLAYER_COUNT = "player_count";
		String SYNC_HASH_CODE = "sync_hash_code";
		String ITEM_NAME = "item_name";
		String OBJECT_ID = "object_id";
		String DELETE_TIMESTAMP = "delete_timestamp";
		String UPDATE_TIMESTAMP = "update_timestamp";
		String DIRTY_TIMESTAMP = "dirty_timestamp";
		String SYNC_TIMESTAMP = "updated_list";
		String SUM_QUANTITY = "sum_quantity";
		String SUM_WINS = "sum_wins";
		String MAX_DATE = "max_date";
	}

	interface PlayPlayersColumns {
		String _PLAY_ID = "_play_id";
		String USER_NAME = "user_name";
		String USER_ID = "user_id";
		String NAME = "name";
		String START_POSITION = "start_position";
		String COLOR = "color";
		String SCORE = "score";
		String NEW = "new";
		String RATING = "rating";
		String WIN = "win";
		String COUNT = "count";
		String DESCRIPTION = "description";
		String UNIQUE_NAME = "unique_name";
	}

	interface CollectionViewsColumns {
		String NAME = "name";
		String STARRED = "starred";
		String SORT_TYPE = "sort_type";
		String SELECTED_COUNT = "selected_count";
		String SELECTED_TIMESTAMP = "selected_timestamp";
	}

	interface CollectionViewFiltersColumns {
		String VIEW_ID = "filter_id";
		String TYPE = "type";
		String DATA = "data";
	}

	public static final String COLLATE_NOCASE = " COLLATE NOCASE";
	public static final String CONTENT_AUTHORITY = "com.boardgamegeek";
	private static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);

	public static final String PATH_GAMES = "games";
	public static final String PATH_RANKS = "ranks";
	public static final String PATH_DESIGNERS = "designers";
	public static final String PATH_ARTISTS = "artists";
	public static final String PATH_PUBLISHERS = "publishers";
	public static final String PATH_MECHANICS = "mechanics";
	public static final String PATH_CATEGORIES = "categories";
	public static final String PATH_EXPANSIONS = "expansions";
	public static final String PATH_COLLECTION = "collection";
	public static final String PATH_BUDDIES = "buddies";
	public static final String PATH_USERS = "users";
	public static final String PATH_POLLS = "polls";
	public static final String PATH_POLL_RESULTS = "results";
	public static final String PATH_POLL_RESULTS_RESULT = "result";
	public static final String PATH_SUGGESTED_PLAYER_COUNT_POLL_RESULTS = "suggestedplayercountpollresults";
	public static final String PATH_THUMBNAILS = "thumbnails";
	public static final String PATH_AVATARS = "avatars";
	public static final String PATH_COLORS = "colors";
	public static final String PATH_PLAYER_COLORS = "playercolors";
	public static final String PATH_PLAYS = "plays";
	public static final String PATH_PLAYERS = "players";
	public static final String PATH_LOCATIONS = "locations";
	public static final String PATH_ACQUIRED_FROM = "acquiredfrom";
	public static final String PATH_INVENTORY_LOCATION = "inventorylocation";
	public static final String PATH_COLLECTION_VIEWS = "collectionviews";
	public static final String PATH_FILTERS = "filters";
	public static final String QUERY_KEY_GROUP_BY = "groupby";
	public static final String QUERY_VALUE_NAME_NOT_USER = "namenotuser";
	public static final String QUERY_VALUE_UNIQUE_NAME = "uniquename";
	public static final String QUERY_VALUE_UNIQUE_PLAYER = "uniqueplayer";
	public static final String QUERY_VALUE_UNIQUE_USER = "uniqueuser";
	public static final String QUERY_VALUE_COLOR = "color";
	public static final String QUERY_VALUE_PLAY = "play";
	public static final String QUERY_KEY_HAVING = "having";
	public static final String FRAGMENT_SIMPLE = "simple";
	public static final String FRAGMENT_PLAYS = "plays";
	public static final String QUERY_KEY_LIMIT = "limit";
	public static final String POLL_TYPE_LANGUAGE_DEPENDENCE = "language_dependence";
	public static final String POLL_TYPE_SUGGESTED_PLAYER_AGE = "suggested_playerage";

	public static class Thumbnails {
		public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath(PATH_THUMBNAILS).build();

		public static Uri buildUri(String fileName) {
			return CONTENT_URI.buildUpon().appendPath(fileName).build();
		}
	}

	public static class Avatars {
		public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath(PATH_AVATARS).build();

		public static Uri buildUri(String avatarFileName) {
			return CONTENT_URI.buildUpon().appendPath(avatarFileName).build();
		}
	}

	public static class Games implements GamesColumns, BaseColumns, SyncColumns, SyncListColumns {
		public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath(PATH_GAMES).build();
		public static final Uri CONTENT_PLAYS_URI = CONTENT_URI.buildUpon().fragment(FRAGMENT_PLAYS).build();

		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.boardgamegeek.game";
		public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.boardgamegeek.game";

		public static final String DEFAULT_SORT = GamesColumns.GAME_SORT_NAME + COLLATE_NOCASE + " ASC";

		public static final String POLLS_COUNT = "polls_count";

		public static boolean isGameUri(@Nullable Uri uri) {
			if (uri == null) return false;
			List<String> segments = uri.getPathSegments();
			return segments != null && segments.size() > 0 && PATH_GAMES.equals(segments.get(0));
		}

		@NonNull
		public static Uri buildGameUri(int gameId) {
			return getUriBuilder(gameId).build();
		}

		@NonNull
		public static Uri buildRanksUri(int gameId) {
			return getUriBuilder(gameId, PATH_RANKS).build();
		}

		public static Uri buildRanksUri(int gameId, int rankId) {
			return getUriBuilder(gameId, PATH_RANKS, rankId).build();
		}

		public static Uri buildDesignersUri(int gameId) {
			return buildLimitedDesignersUri(gameId, 0);
		}

		public static Uri buildLimitedDesignersUri(int gameId, int limitCount) {
			return getLimitedUriBuilder(gameId, PATH_DESIGNERS, limitCount).build();
		}

		public static Uri buildDesignersUri(long rowId) {
			return getUriBuilder().appendPath(PATH_DESIGNERS).appendPath(String.valueOf(rowId)).build();
		}

		public static Uri buildArtistsUri(int gameId) {
			return buildLimitedArtistsUri(gameId, 0);
		}

		public static Uri buildLimitedArtistsUri(int gameId, int limitCount) {
			return getLimitedUriBuilder(gameId, PATH_ARTISTS, limitCount).build();
		}

		public static Uri buildArtistUri(long rowId) {
			return getUriBuilder().appendPath(PATH_ARTISTS).appendPath(String.valueOf(rowId)).build();
		}

		public static Uri buildPublishersUri(int gameId) {
			return buildLimitedPublishersUri(gameId, 0);
		}

		public static Uri buildLimitedPublishersUri(int gameId, int limitCount) {
			return getLimitedUriBuilder(gameId, PATH_PUBLISHERS, limitCount).build();
		}

		public static Uri buildPublisherUri(long rowId) {
			return getUriBuilder().appendPath(PATH_PUBLISHERS).appendPath(String.valueOf(rowId)).build();
		}

		public static Uri buildMechanicsUri(int gameId) {
			return buildLimitedMechanicsUri(gameId, 0);
		}

		public static Uri buildLimitedMechanicsUri(int gameId, int limitCount) {
			return getLimitedUriBuilder(gameId, PATH_MECHANICS, limitCount).build();
		}

		public static Uri buildMechanicUri(long rowId) {
			return getUriBuilder().appendPath(PATH_MECHANICS).appendPath(String.valueOf(rowId)).build();
		}

		public static Uri buildCategoriesUri(int gameId) {
			return buildLimitedCategoriesUri(gameId, 0);
		}

		public static Uri buildLimitedCategoriesUri(int gameId, int limitCount) {
			return getLimitedUriBuilder(gameId, PATH_CATEGORIES, limitCount).build();
		}

		public static Uri buildCategoryUri(long rowId) {
			return getUriBuilder().appendPath(PATH_CATEGORIES).appendPath(String.valueOf(rowId)).build();
		}

		public static Uri buildExpansionsUri(int gameId) {
			return buildLimitedExpansionsUri(gameId, 0);
		}

		public static Uri buildLimitedExpansionsUri(int gameId, int limitCount) {
			return getLimitedUriBuilder(gameId, PATH_EXPANSIONS, limitCount).build();
		}

		public static Uri buildExpansionUri(long rowId) {
			return getUriBuilder().appendPath(PATH_EXPANSIONS).appendPath(String.valueOf(rowId)).build();
		}

		public static Uri buildPollsUri(int gameId) {
			return getUriBuilder(gameId, PATH_POLLS).build();
		}

		public static Uri buildPollsUri(int gameId, String pollName) {
			return getUriBuilder(gameId, PATH_POLLS).appendPath(pollName).build();
		}

		public static Uri buildPollResultsUri(int gameId, String pollName) {
			return getUriBuilder(gameId, PATH_POLLS).appendPath(pollName).appendPath(PATH_POLL_RESULTS).build();
		}

		public static Uri buildPollResultsUri(int gameId, String pollName, String key) {
			return getUriBuilder(gameId, PATH_POLLS).appendPath(pollName).appendPath(PATH_POLL_RESULTS).appendPath(key)
				.build();
		}

		public static Uri buildPollResultsResultUri(int gameId, String pollName) {
			return getUriBuilder(gameId, PATH_POLLS).appendPath(pollName).appendPath(PATH_POLL_RESULTS)
				.appendPath(PATH_POLL_RESULTS_RESULT).build();
		}

		public static Uri buildPollResultsResultUri(int gameId, String pollName, String key) {
			return getUriBuilder(gameId, PATH_POLLS).appendPath(pollName).appendPath(PATH_POLL_RESULTS).appendPath(key)
				.appendPath(PATH_POLL_RESULTS_RESULT).build();
		}

		public static Uri buildPollResultsResultUri(int gameId, String pollName, String key, String key2) {
			return getUriBuilder(gameId, PATH_POLLS).appendPath(pollName)
				.appendPath(PATH_POLL_RESULTS).appendPath(key)
				.appendPath(PATH_POLL_RESULTS_RESULT).appendPath(key2).build();
		}

		public static Uri buildSuggestedPlayerCountPollResultsUri(int gameId) {
			return getUriBuilder(gameId, PATH_SUGGESTED_PLAYER_COUNT_POLL_RESULTS).build();
		}

		public static Uri buildSuggestedPlayerCountPollResultsUri(int gameId, String playerCount) {
			return getUriBuilder(gameId, PATH_SUGGESTED_PLAYER_COUNT_POLL_RESULTS).appendPath(playerCount).build();
		}

		public static Uri buildColorsUri(int gameId) {
			return getUriBuilder(gameId, PATH_COLORS).build();
		}

		public static Uri buildColorsUri(int gameId, String color) {
			return getUriBuilder(gameId, PATH_COLORS).appendPath(color).build();
		}

		private static Builder getUriBuilder() {
			return CONTENT_URI.buildUpon();
		}

		private static Builder getUriBuilder(int gameId) {
			return CONTENT_URI.buildUpon().appendPath(String.valueOf(gameId));
		}

		private static Builder getUriBuilder(int gameId, String path) {
			return getLimitedUriBuilder(gameId, path, 0);
		}

		public static Uri buildPathUri(int gameId, String path) {
			return CONTENT_URI.buildUpon().appendPath(String.valueOf(gameId)).appendPath(path).build();
		}

		public static Uri buildPathUri(int gameId, String path, int id) {
			return CONTENT_URI.buildUpon().appendPath(String.valueOf(gameId)).appendPath(path)
				.appendPath(String.valueOf(id)).build();
		}

		private static Builder getLimitedUriBuilder(int gameId, String path, int limit) {
			Builder builder = CONTENT_URI.buildUpon().appendPath(String.valueOf(gameId)).appendPath(path);
			if (limit > 0) {
				builder.appendQueryParameter(QUERY_KEY_LIMIT, String.valueOf(limit));
			}
			return builder;
		}

		private static Builder getUriBuilder(int gameId, String path, int id) {
			return getUriBuilder(gameId, path).appendPath(String.valueOf(id));
		}

		public static int getGameId(Uri uri) {
			if (uri != null) {
				List<String> segments = uri.getPathSegments();
				if (segments != null && segments.size() > 1 && PATH_GAMES.equals(segments.get(0))) {
					return StringUtils.parseInt(segments.get(1));
				}
			}
			return INVALID_ID;
		}

		public static String getPollName(Uri uri) {
			return getPathValue(uri, PATH_POLLS);
		}

		public static String getPollResultsKey(Uri uri) {
			return getPathValue(uri, PATH_POLL_RESULTS);
		}

		public static String getPollResultsResultKey(Uri uri) {
			return getPathValue(uri, PATH_POLL_RESULTS_RESULT);
		}

		public static String getPathValue(Uri uri, String path) {
			if (TextUtils.isEmpty(path)) {
				return "";
			}
			boolean isNextValue = false;
			for (String segment : uri.getPathSegments()) {
				if (isNextValue) {
					return segment;
				}
				if (path.equals(segment)) {
					isNextValue = true;
				}
			}
			return "";
		}

		public static String createRecommendedPlayerCountColumn(String playerCount) {
			return Games.PLAYER_COUNT_RECOMMENDATION_PREFIX + playerCount;
		}

		public static String getRecommendedPlayerCountFromColumn(String column) {
			if (column.startsWith(Games.PLAYER_COUNT_RECOMMENDATION_PREFIX)) {
				String delimiter = Games.PLAYER_COUNT_RECOMMENDATION_PREFIX.substring(Games.PLAYER_COUNT_RECOMMENDATION_PREFIX.length() - 1);
				String[] parts = column.split(delimiter);
				return parts[parts.length - 1];
			}
			return null;
		}
	}

	public static class GameRanks implements GameRanksColumns, GamesColumns, BaseColumns {
		public static final Uri CONTENT_URI = Games.CONTENT_URI.buildUpon().appendPath(PATH_RANKS).build();

		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.boardgamegeek.rank";
		public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.boardgamegeek.rank";

		public static final String DEFAULT_SORT = GameRanksColumns.GAME_RANK_TYPE + " DESC,"
			+ GameRanksColumns.GAME_RANK_VALUE + "," + GameRanksColumns.GAME_RANK_FRIENDLY_NAME;

		public static Uri buildGameRankUri(int gameRankId) {
			return CONTENT_URI.buildUpon().appendPath(String.valueOf(gameRankId)).build();
		}

		public static int getRankId(Uri uri) {
			return StringUtils.parseInt(uri.getLastPathSegment());
		}
	}

	public static class Designers implements DesignersColumns, BaseColumns, SyncColumns {
		public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath(PATH_DESIGNERS).build();

		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.boardgamegeek.designer";
		public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.boardgamegeek.designer";

		public static final String DEFAULT_SORT = DesignersColumns.DESIGNER_NAME + COLLATE_NOCASE + " ASC";

		public static Uri buildDesignerUri(int designerId) {
			return CONTENT_URI.buildUpon().appendPath(String.valueOf(designerId)).build();
		}

		public static int getDesignerId(Uri uri) {
			return StringUtils.parseInt(uri.getPathSegments().get(1));
		}

		public static Uri buildDesignerCollectionUri(int designerId) {
			return CONTENT_URI.buildUpon().appendPath(String.valueOf(designerId)).appendPath(PATH_COLLECTION).build();
		}
	}

	public static Uri buildBasicUri(String path, long id) {
		return BASE_CONTENT_URI.buildUpon().appendPath(path).appendPath(String.valueOf(id)).build();
	}

	public static class Artists implements ArtistsColumns, BaseColumns, SyncColumns {
		public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath(PATH_ARTISTS).build();

		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.boardgamegeek.artist";
		public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.boardgamegeek.artist";

		public static final String DEFAULT_SORT = ArtistsColumns.ARTIST_NAME + COLLATE_NOCASE + " ASC";

		public static Uri buildArtistUri(int artistId) {
			return CONTENT_URI.buildUpon().appendPath(String.valueOf(artistId)).build();
		}

		public static Uri buildArtistCollectionUri(int artistId) {
			return CONTENT_URI.buildUpon().appendPath(String.valueOf(artistId)).appendPath(PATH_COLLECTION).build();
		}

		public static int getArtistId(Uri uri) {
			return StringUtils.parseInt(uri.getPathSegments().get(1));
		}
	}

	public static class Publishers implements PublishersColumns, BaseColumns, SyncColumns {
		public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath(PATH_PUBLISHERS).build();

		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.boardgamegeek.publisher";
		public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.boardgamegeek.publisher";

		public static final String DEFAULT_SORT = PublishersColumns.PUBLISHER_NAME + COLLATE_NOCASE + " ASC";

		public static Uri buildPublisherUri(int publisherId) {
			return CONTENT_URI.buildUpon().appendPath(String.valueOf(publisherId)).build();
		}

		public static int getPublisherId(Uri uri) {
			return StringUtils.parseInt(uri.getPathSegments().get(1));
		}

		public static Uri buildCollectionUri(int publisherId) {
			return CONTENT_URI.buildUpon().appendPath(String.valueOf(publisherId)).appendPath(PATH_COLLECTION).build();
		}
	}

	public static class Mechanics implements MechanicsColumns, BaseColumns {
		public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath(PATH_MECHANICS).build();

		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.boardgamegeek.mechanic";
		public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.boardgamegeek.mechanic";

		public static final String DEFAULT_SORT = MechanicsColumns.MECHANIC_NAME + COLLATE_NOCASE + " ASC";

		public static Uri buildMechanicUri(int mechanicId) {
			return createMechanicUri(mechanicId).build();
		}

		private static Builder createMechanicUri(int mechanicId) {
			return CONTENT_URI.buildUpon().appendPath(String.valueOf(mechanicId));
		}

		public static int getMechanicId(Uri uri) {
			return StringUtils.parseInt(uri.getPathSegments().get(1));
		}

		public static Uri buildCollectionUri(int mechanicId) {
			return createMechanicUri(mechanicId).appendPath(PATH_COLLECTION).build();
		}
	}

	public static class Categories implements CategoriesColumns, BaseColumns {
		public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath(PATH_CATEGORIES).build();

		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.boardgamegeek.category";
		public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.boardgamegeek.category";

		public static final String DEFAULT_SORT = CategoriesColumns.CATEGORY_NAME + COLLATE_NOCASE + " ASC";

		public static Uri buildCategoryUri(int categoryId) {
			return createCategoryUri(categoryId).build();
		}

		public static int getCategoryId(Uri uri) {
			return StringUtils.parseInt(uri.getPathSegments().get(1));
		}

		public static Uri buildCollectionUri(int categoryId) {
			return createCategoryUri(categoryId).appendPath(PATH_COLLECTION).build();
		}

		private static Builder createCategoryUri(int categoryId) {
			return CONTENT_URI.buildUpon().appendPath(String.valueOf(categoryId));
		}
	}

	public static class GamesExpansions implements GamesExpansionsColumns, GamesColumns, BaseColumns {
		public static final Uri CONTENT_URI = Games.CONTENT_URI.buildUpon().appendPath(PATH_EXPANSIONS).build();

		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.boardgamegeek.expansion";
		public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.boardgamegeek.expansion";

		public static final String DEFAULT_SORT = GamesExpansionsColumns.EXPANSION_NAME + COLLATE_NOCASE + " ASC";
	}

	public static class Collection implements CollectionColumns, GamesColumns, BaseColumns, SyncColumns, SyncListColumns {
		public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath(PATH_COLLECTION).build();

		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.boardgamegeek.collection";
		public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.boardgamegeek.collection";

		public static final String DEFAULT_SORT = CollectionColumns.COLLECTION_SORT_NAME + COLLATE_NOCASE + " ASC";
		public static final String SORT_ACQUIRED_FROM = CollectionColumns.PRIVATE_INFO_ACQUIRED_FROM + COLLATE_NOCASE + " ASC";
		public static final String SORT_INVENTORY_LOCATION = CollectionColumns.PRIVATE_INFO_INVENTORY_LOCATION + COLLATE_NOCASE + " ASC";

		public static Uri buildUri(long id) {
			return CONTENT_URI.buildUpon().appendPath(String.valueOf(id)).build();
		}

		public static Uri buildUri(String having) {
			if (TextUtils.isEmpty(having)) return CONTENT_URI;
			return CONTENT_URI.buildUpon().appendQueryParameter(QUERY_KEY_HAVING, having).build();
		}

		public static Uri buildAcquiredFromUri() {
			return CONTENT_URI.buildUpon().appendPath(PATH_ACQUIRED_FROM).build();
		}

		public static Uri buildInventoryLocationUri() {
			return CONTENT_URI.buildUpon().appendPath(PATH_INVENTORY_LOCATION).build();
		}

		public static long getId(Uri uri) {
			return StringUtils.parseInt(uri.getPathSegments().get(1));
		}
	}

	public static class Buddies implements BuddiesColumns, BaseColumns, SyncColumns, SyncListColumns {
		public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath(PATH_BUDDIES).build();

		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.boardgamegeek.buddy";
		public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.boardgamegeek.buddy";

		public static final String DEFAULT_SORT = BuddiesColumns.BUDDY_LASTNAME + COLLATE_NOCASE + " ASC, "
			+ BuddiesColumns.BUDDY_FIRSTNAME + COLLATE_NOCASE + " ASC";

		public static final String NAME_SORT = BuddiesColumns.BUDDY_NAME + COLLATE_NOCASE + " ASC";

		public static Uri buildBuddyUri(String buddyName) {
			return CONTENT_URI.buildUpon().appendPath(buddyName).build();
		}

		public static String getBuddyName(Uri uri) {
			return uri.getPathSegments().get(1);
		}
	}

	public static class PlayerColors implements PlayerColorsColumns, BaseColumns {
		public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath(PATH_PLAYER_COLORS).build();

		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.boardgamegeek.playercolor";
		public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.boardgamegeek.playercolor";
		public static final int TYPE_USER = 1;
		public static final int TYPE_PLAYER = 2;

		public static final String DEFAULT_SORT = PlayerColors.PLAYER_TYPE + " ASC, " +
			PlayerColors.PLAYER_NAME + " ASC, " +
			PlayerColors.PLAYER_COLOR_SORT_ORDER + " ASC";

		public static Uri buildUserUri(String username) {
			return BASE_CONTENT_URI.buildUpon().appendPath(PATH_USERS).appendPath(username).appendPath(PATH_COLORS).build();
		}

		public static Uri buildPlayerUri(String playerName) {
			return BASE_CONTENT_URI.buildUpon().appendPath(PATH_PLAYERS).appendPath(playerName).appendPath(PATH_COLORS).build();
		}

		public static Uri buildUserUri(String username, int sortOrder) {
			return buildUserUri(username).buildUpon().appendPath(String.valueOf(sortOrder)).build();
		}

		public static Uri buildPlayerUri(String playerName, int sortOrder) {
			return buildPlayerUri(playerName).buildUpon().appendPath(String.valueOf(sortOrder)).build();
		}

		public static Uri addSortUri(Uri uri, int sortOrder) {
			return uri.buildUpon().appendPath(String.valueOf(sortOrder)).build();
		}

		@Nullable
		public static String getUsername(Uri uri) {
			if (uri != null) {
				List<String> segments = uri.getPathSegments();
				if (segments != null && segments.size() > 1 && PATH_USERS.equals(segments.get(0))) {
					return segments.get(1);
				}
			}
			return null;
		}

		@Nullable
		public static String getPlayerName(Uri uri) {
			if (uri != null) {
				List<String> segments = uri.getPathSegments();
				if (segments != null && segments.size() > 1 && PATH_PLAYERS.equals(segments.get(0))) {
					return segments.get(1);
				}
			}
			return null;
		}

		public static int getSortOrder(Uri uri) {
			if (uri != null) {
				List<String> segments = uri.getPathSegments();
				if (segments != null && segments.size() > 1 && PATH_COLORS.equals(segments.get(0))) {
					return Integer.parseInt(segments.get(1));
				}
			}
			return 0;
		}
	}

	public static class GameSuggestedPlayerCountPollPollResults implements GameSuggestedPlayerCountPollResultsColumns, GamesColumns, BaseColumns {
		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.boardgamegeek.boardgamepoll.playercount";
		public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.boardgamegeek.boardgamepoll.playercount";

		public static final String DEFAULT_SORT = GameSuggestedPlayerCountPollResultsColumns.SORT_INDEX + " ASC";
	}

	public static class GamePolls implements GamePollsColumns, GamesColumns, BaseColumns {
		public static final Uri CONTENT_URI = Games.CONTENT_URI.buildUpon().appendPath(PATH_POLLS).build();

		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.boardgamegeek.boardgamepoll";
		public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.boardgamegeek.boardgamepoll";

		public static final String DEFAULT_SORT = GamePollsColumns.POLL_TITLE + COLLATE_NOCASE + " ASC";
	}

	public static final class GamePollResults implements GamePollResultsColumns, GamePollsColumns, BaseColumns {
		public static final Uri CONTENT_URI = GamePolls.CONTENT_URI.buildUpon().appendPath(PATH_POLL_RESULTS).build();

		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.boardgamegeek.boardgamepollresult";
		public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.boardgamegeek.boardgamepollresult";

		public static final String DEFAULT_SORT = POLL_RESULTS_SORT_INDEX + " ASC";
	}

	public static final class GamePollResultsResult implements GamePollResultsResultColumns, GamePollResultsColumns, BaseColumns {
		public static final Uri CONTENT_URI = GamePollResults.CONTENT_URI.buildUpon().appendPath(PATH_POLL_RESULTS_RESULT).build();

		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.boardgamegeek.boardgamepollresultsresult";
		public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.boardgamegeek.boardgamepollresultsresult";

		public static final String DEFAULT_SORT = POLL_RESULTS_SORT_INDEX + " ASC, " + POLL_RESULTS_RESULT_SORT_INDEX + " ASC";
	}

	public static final class GameColors implements GameColorsColumns, GamesColumns, BaseColumns {
		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.boardgamegeek.boardgamecolor";
		public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.boardgamegeek.boardgamecolor";

		public static final String DEFAULT_SORT = COLOR + COLLATE_NOCASE + " ASC";
	}

	public static final class Plays implements PlaysColumns, BaseColumns {
		public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath(PATH_PLAYS).build();
		public static final Uri CONTENT_SIMPLE_URI = CONTENT_URI.buildUpon().fragment(FRAGMENT_SIMPLE).build();

		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.boardgamegeek.play";
		public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.boardgamegeek.play";

		// TODO define table name in a better spot
		public static final String DEFAULT_SORT = DATE + " DESC, plays." + PLAY_ID + " DESC";

		/**
		 * content://com.boardgamegeek/plays/#
		 */
		public static Uri buildPlayUri(long internalId) {
			return CONTENT_URI.buildUpon().appendPath(String.valueOf(internalId)).build();
		}

		public static Uri buildPlayerUri() {
			return CONTENT_URI.buildUpon()
				.appendPath(PATH_PLAYERS)
				.build();
		}

		public static Uri buildPlayerUri(long internalId) {
			return CONTENT_URI.buildUpon()
				.appendPath(String.valueOf(internalId))
				.appendPath(PATH_PLAYERS)
				.build();
		}

		public static Uri buildPlayerUri(long internalPlayId, long internalPlayerId) {
			return CONTENT_URI.buildUpon()
				.appendPath(String.valueOf(internalPlayId))
				.appendPath(PATH_PLAYERS)
				.appendPath(String.valueOf(internalPlayerId))
				.build();
		}

		public static Uri buildLocationsUri() {
			return CONTENT_URI.buildUpon().appendPath(PATH_LOCATIONS).build();
		}

		public static Uri buildPlayersUri() {
			return CONTENT_URI.buildUpon().appendPath(PATH_PLAYERS).build();
		}

		public static Uri buildPlayersByPlayUri() {
			return buildPlayersUri().buildUpon().appendQueryParameter(QUERY_KEY_GROUP_BY, QUERY_VALUE_PLAY).build();
		}

		public static Uri buildPlayersByUniquePlayerUri() {
			return buildPlayersUri().buildUpon().appendQueryParameter(QUERY_KEY_GROUP_BY, QUERY_VALUE_UNIQUE_PLAYER).build();
		}

		public static Uri buildPlayersByUniqueUserUri() {
			return buildPlayersUri().buildUpon().appendQueryParameter(QUERY_KEY_GROUP_BY, QUERY_VALUE_UNIQUE_USER).build();
		}

		public static Uri buildPlayersByUniqueNameUri() {
			return buildPlayersUri().buildUpon().appendQueryParameter(QUERY_KEY_GROUP_BY, QUERY_VALUE_UNIQUE_NAME).build();
		}

		public static Uri buildPlayersByColor() {
			return buildPlayersUri().buildUpon().appendQueryParameter(QUERY_KEY_GROUP_BY, QUERY_VALUE_COLOR).build();
		}

		public static long getInternalId(Uri uri) {
			return StringUtils.parseLong(uri.getPathSegments().get(1));
		}
	}

	public static final class PlayPlayers implements PlayPlayersColumns, PlaysColumns, BaseColumns {
		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.boardgamegeek.playplayer";
		public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.boardgamegeek.playplayer";

		public static final String DEFAULT_SORT = START_POSITION + " ASC, play_players." + NAME + COLLATE_NOCASE + " ASC";
		public static final String SORT_BY_COUNT = COUNT + " DESC, " + DEFAULT_SORT;

		public static long getPlayPlayerId(Uri uri) {
			if (uri != null) {
				String lastPathSegment = uri.getLastPathSegment();
				return lastPathSegment == null ? BggContract.INVALID_ID : Long.valueOf(lastPathSegment);
			}
			return BggContract.INVALID_ID;
		}
	}

	public static final class PlayLocations {
		public static final String DEFAULT_SORT = PlaysColumns.LOCATION + COLLATE_NOCASE + " ASC";
		public static final String SORT_BY_SUM_QUANTITY = PlaysColumns.SUM_QUANTITY + " DESC, " + DEFAULT_SORT;
	}

	public static final class CollectionViews implements CollectionViewsColumns, BaseColumns {
		public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath(PATH_COLLECTION_VIEWS).build();

		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.boardgamegeek.collectionview";
		public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.boardgamegeek.collectionview";

		public static final String DEFAULT_SORT = STARRED + " DESC, " + NAME + COLLATE_NOCASE + " ASC";

		public static Uri buildViewUri(long viewId) {
			return CONTENT_URI.buildUpon().appendPath(String.valueOf(viewId)).build();
		}

		public static Uri buildViewFilterUri(long viewId) {
			return buildViewUri(viewId).buildUpon().appendPath(PATH_FILTERS).build();
		}

		public static Uri buildViewFilterUri(long viewId, long filterId) {
			return buildViewUri(viewId).buildUpon().appendPath(PATH_FILTERS).appendPath(String.valueOf(filterId))
				.build();
		}

		public static int getViewId(Uri uri) {
			return StringUtils.parseInt(uri.getPathSegments().get(1));
		}
	}

	public static final class CollectionViewFilters implements CollectionViewFiltersColumns, CollectionViewsColumns,
		BaseColumns {
		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.boardgamegeek.collectionviewfilter";
		public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.boardgamegeek.collectionviewfilter";

		public static final String DEFAULT_SORT = STARRED + " DESC, " + NAME + COLLATE_NOCASE + " ASC, " + TYPE + " ASC";

		public static int getFilterType(Uri uri) {
			if (uri != null) {
				String lastPathSegment = uri.getLastPathSegment();
				return lastPathSegment == null ? BggContract.INVALID_ID : Integer.valueOf(lastPathSegment);
			}
			return BggContract.INVALID_ID;
		}
	}

	private BggContract() {
	}
}