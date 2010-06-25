package com.boardgamegeek;

import android.net.Uri;
import android.provider.BaseColumns;

public class BoardGameGeekData {
	public static final String AUTHORITY = "com.boardgamegeek.provider";

	// This class cannot be instantiated
	private BoardGameGeekData() {}

	public static final class Designers implements BaseColumns {
		// This class cannot be instantiated
		private Designers() {}

		public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/designers");
		public static final String CONTENT_TYPE = "vnd.boardgamegeek.cursor.dir/vnd.boardgamegeek.designer";
		public static final String CONTENT_ITEM_TYPE = "vnd.boardgamegeek.cursor.item/vnd.boardgamegeek.designer";

		public static final String NAME = "name";
		public static final String DESCRIPTION = "description";
		public static final String UPDATED_DATE = "updated";

		public static final String DEFAULT_SORT_ORDER = NAME + " ASC";
	}

	public static final class Thumbnails implements BaseColumns {
		private Thumbnails() {}

		public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/thumbnails");
		public static final String CONTENT_TYPE = "vnd.boardgamegeek.cursor.dir/vnd.boardgamegeek.thumbnail";
		public static final String CONTENT_ITEM_TYPE = "vnd.boardgamegeek.cursor.item/vnd.boardgamegeek.thumbnail";

		public static final String PATH = "path";
		public static final String DATA = "data";

		public static final String DEFAULT_SORT_ORDER = _ID + " ASC";
	}

	public static final class Artists implements BaseColumns {
		// This class cannot be instantiated
		private Artists() {}

		public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/artists");
		public static final String CONTENT_TYPE = "vnd.boardgamegeek.cursor.dir/vnd.boardgamegeek.artist";
		public static final String CONTENT_ITEM_TYPE = "vnd.boardgamegeek.cursor.item/vnd.boardgamegeek.artist";

		public static final String NAME = "name";
		public static final String DESCRIPTION = "description";
		public static final String UPDATED_DATE = "updated";

		public static final String DEFAULT_SORT_ORDER = NAME + " ASC";
	}

	public static final class Publishers implements BaseColumns {
		// This class cannot be instantiated
		private Publishers() {}

		public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/publishers");
		public static final String CONTENT_TYPE = "vnd.boardgamegeek.cursor.dir/vnd.boardgamegeek.publisher";
		public static final String CONTENT_ITEM_TYPE = "vnd.boardgamegeek.cursor.item/vnd.boardgamegeek.publisher";

		public static final String NAME = "name";
		public static final String DESCRIPTION = "description";
		public static final String UPDATED_DATE = "updated";

		public static final String DEFAULT_SORT_ORDER = NAME + " ASC";
	}

	public static final class Categories implements BaseColumns {
		// This class cannot be instantiated
		private Categories() {}

		public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/categories");
		public static final String CONTENT_TYPE = "vnd.boardgamegeek.cursor.dir/vnd.boardgamegeek.category";
		public static final String CONTENT_ITEM_TYPE = "vnd.boardgamegeek.cursor.item/vnd.boardgamegeek.category";

		public static final String NAME = "name";

		public static final String DEFAULT_SORT_ORDER = NAME + " ASC";
	}

	public static final class Mechanics implements BaseColumns {
		// This class cannot be instantiated
		private Mechanics() {}

		public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/mechanics");
		public static final String CONTENT_TYPE = "vnd.boardgamegeek.cursor.dir/vnd.boardgamegeek.mechanic";
		public static final String CONTENT_ITEM_TYPE = "vnd.boardgamegeek.cursor.item/vnd.boardgamegeek.mechanic";

		public static final String NAME = "name";

		public static final String DEFAULT_SORT_ORDER = NAME + " ASC";
	}

	public static final class BoardGames implements BaseColumns {
		// This class cannot be instantiated
		private BoardGames() {}

		public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/boardgames");
		public static final String CONTENT_TYPE = "vnd.boardgamegeek.cursor.dir/vnd.boardgamegeek.boardgame";
		public static final String CONTENT_ITEM_TYPE = "vnd.boardgamegeek.cursor.item/vnd.boardgamegeek.boardgame";

		public static final String NAME = "name";
		public static final String SORT_INDEX = "sortIndex";
		public static final String SORT_NAME = "sortName";
		public static final String YEAR = "year";
		public static final String MIN_PLAYERS = "minPlayers";
		public static final String MAX_PLAYERS = "maxPlayers";
		public static final String PLAYING_TIME = "playingTime";
		public static final String AGE = "age";
		public static final String DESCRIPTION = "description";
		public static final String THUMBNAIL_URL = "thumbnailUrl";
		public static final String RATING_COUNT = "ratingCount";
		public static final String AVERAGE = "average";
		public static final String BAYES_AVERAGE = "bayesAverage";
		public static final String RANK = "rank";
		public static final String STANDARD_DEVIATION = "standardDeviation";
		public static final String MEDIAN = "median";
		public static final String OWNED_COUNT = "ownedCount";
		public static final String TRADING_COUNT = "tradingCount";
		public static final String WANTING_COUNT = "wantingCount";
		public static final String WISHING_COUNT = "wishingCount";
		public static final String COMMENT_COUNT = "commentCount";
		public static final String WEIGHT_COUNT = "weightCount";
		public static final String AVERAGE_WEIGHT = "averageWeight";
		public static final String UPDATED_DATE = "updated";

		public static final String DEFAULT_SORT_ORDER = SORT_NAME + " ASC";
	}

	public static final class BoardGameDesigners implements BaseColumns {
		// This class cannot be instantiated
		private BoardGameDesigners() {}

		public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/boardgamedesigners");
		public static final String CONTENT_TYPE = "vnd.boardgamegeek.cursor.dir/vnd.boardgamegeek.boardgamedesigner";
		public static final String CONTENT_ITEM_TYPE = "vnd.boardgamegeek.cursor.item/vnd.boardgamegeek.boardgamedesigner";

		public static final String BOARDGAME_ID = "boardgame_id";
		public static final String DESIGNER_ID = "designer_id";
		public static final String DESIGNER_NAME = "designer_name";

		public static final String DEFAULT_SORT_ORDER = DESIGNER_ID + " ASC";
	}

	public static final class BoardGameArtists implements BaseColumns {
		// This class cannot be instantiated
		private BoardGameArtists() {}

		public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/boardgameartists");
		public static final String CONTENT_TYPE = "vnd.boardgamegeek.cursor.dir/vnd.boardgamegeek.boardgameartist";
		public static final String CONTENT_ITEM_TYPE = "vnd.boardgamegeek.cursor.item/vnd.boardgamegeek.boardgameartist";

		public static final String BOARDGAME_ID = "boardgame_id";
		public static final String ARTIST_ID = "artist_id";
		public static final String ARTIST_NAME = "artist_name";

		public static final String DEFAULT_SORT_ORDER = ARTIST_ID + " ASC";
	}

	public static final class BoardGamePublishers implements BaseColumns {
		// This class cannot be instantiated
		private BoardGamePublishers() {}

		public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/boardgamepublishers");
		public static final String CONTENT_TYPE = "vnd.boardgamegeek.cursor.dir/vnd.boardgamegeek.boardgamepublisher";
		public static final String CONTENT_ITEM_TYPE = "vnd.boardgamegeek.cursor.item/vnd.boardgamegeek.boardgamepublisher";

		public static final String BOARDGAME_ID = "boardgame_id";
		public static final String PUBLISHER_ID = "publisher_id";
		public static final String PUBLISHER_NAME = "publisher_name";

		public static final String DEFAULT_SORT_ORDER = PUBLISHER_ID + " ASC";
	}

	public static final class BoardGameCategories implements BaseColumns {
		// This class cannot be instantiated
		private BoardGameCategories() {}

		public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/boardgamecategories");
		public static final String CONTENT_TYPE = "vnd.boardgamegeek.cursor.dir/vnd.boardgamegeek.boardgamecategory";
		public static final String CONTENT_ITEM_TYPE = "vnd.boardgamegeek.cursor.item/vnd.boardgamegeek.boardgamecategory";

		public static final String BOARDGAME_ID = "boardgame_id";
		public static final String CATEGORY_ID = "category_id";
		public static final String CATEGORY_NAME = "category_name";

		public static final String DEFAULT_SORT_ORDER = CATEGORY_ID + " ASC";
	}

	public static final class BoardGameMechanics implements BaseColumns {
		// This class cannot be instantiated
		private BoardGameMechanics() {}

		public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/boardgamemechanics");
		public static final String CONTENT_TYPE = "vnd.boardgamegeek.cursor.dir/vnd.boardgamegeek.boardgamemechanic";
		public static final String CONTENT_ITEM_TYPE = "vnd.boardgamegeek.cursor.item/vnd.boardgamegeek.boardgamemechanic";

		public static final String BOARDGAME_ID = "boardgame_id";
		public static final String MECHANIC_ID = "mechanic_id";
		public static final String MECHANIC_NAME = "mechanic_name";

		public static final String DEFAULT_SORT_ORDER = MECHANIC_ID + " ASC";
	}

	public static final class BoardGameExpansions implements BaseColumns {
		// This class cannot be instantiated
		private BoardGameExpansions() {}

		public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/boardgameexpansions");
		public static final String CONTENT_TYPE = "vnd.boardgamegeek.cursor.dir/vnd.boardgamegeek.boardgameexpansion";
		public static final String CONTENT_ITEM_TYPE = "vnd.boardgamegeek.cursor.item/vnd.boardgamegeek.boardgameexpansion";

		public static final String BOARDGAME_ID = "boardgame_id";
		public static final String EXPANSION_ID = "expansion_id";
		public static final String EXPANSION_NAME = "expansion_name";

		public static final String DEFAULT_SORT_ORDER = EXPANSION_ID + " ASC";
	}

	public static final class BoardGamePolls implements BaseColumns {
		// This class cannot be instantiated
		private BoardGamePolls() {}

		public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/boardgamepolls");
		public static final String CONTENT_TYPE = "vnd.boardgamegeek.cursor.dir/vnd.boardgamegeek.boardgamepoll";
		public static final String CONTENT_ITEM_TYPE = "vnd.boardgamegeek.cursor.item/vnd.boardgamegeek.boardgamepoll";

		public static final String BOARDGAME_ID = "boardgame_id";
		public static final String NAME = "poll_name";
		public static final String TITLE = "poll_title";
		public static final String VOTES = "poll_votes";

		public static final String DEFAULT_SORT_ORDER = NAME + " ASC";
	}

	public static final class BoardGamePollResults implements BaseColumns {
		// This class cannot be instantiated
		private BoardGamePollResults() {}

		public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/boardgamepollresults");
		public static final String CONTENT_TYPE = "vnd.boardgamegeek.cursor.dir/vnd.boardgamegeek.boardgamepollresults";
		public static final String CONTENT_ITEM_TYPE = "vnd.boardgamegeek.cursor.item/vnd.boardgamegeek.boardgamepollresults";

		public static final String POLL_ID = "poll_id";
		public static final String PLAYERS = "players";

		public static final String DEFAULT_SORT_ORDER = POLL_ID + " ASC, " + PLAYERS + " ASC";
	}

	public static final class BoardGamePollResult implements BaseColumns {
		// This class cannot be instantiated
		private BoardGamePollResult() {}

		public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/boardgamepollresult");
		public static final String CONTENT_TYPE = "vnd.boardgamegeek.cursor.dir/vnd.boardgamegeek.boardgamepollresult";
		public static final String CONTENT_ITEM_TYPE = "vnd.boardgamegeek.cursor.item/vnd.boardgamegeek.boardgamepollresult";

		public static final String POLLRESULTS_ID = "pollresults_id";
		public static final String LEVEL = "result_level";
		public static final String VALUE = "result_value";
		public static final String VOTES = "result_votes";

		public static final String DEFAULT_SORT_ORDER = POLLRESULTS_ID + " ASC";
	}
}
