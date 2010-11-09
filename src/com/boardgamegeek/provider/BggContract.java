package com.boardgamegeek.provider;

import com.boardgamegeek.Utility;

import android.net.Uri;
import android.provider.BaseColumns;

public class BggContract {

	public interface SyncColumns {
		String UPDATED_LIST = "updated_list";
		String UPDATED_DETAIL = "updated_detail";
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

	private static final String PATH_BUDDIES = "buddies";

	public static class Buddies implements BuddiesColumns, BaseColumns, SyncColumns {
		public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath(PATH_BUDDIES).build();

		public static final String DEFAULT_SORT =
			BuddiesColumns.BUDDY_LASTNAME + " COLLATE NOCASE ASC, " +
			BuddiesColumns.BUDDY_FIRSTNAME + " COLLATE NOCASE ASC";

		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.boardgamegeek.buddy";
		public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.boardgamegeek.buddy";

		public static Uri buildBuddyUri(int buddyId) {
			return CONTENT_URI.buildUpon().appendPath("" + buddyId).build();
		}

		public static int getBuddyId(Uri uri) {
			return Utility.parseInt(uri.getPathSegments().get(1));
		}
	}

	private BggContract() {}
}
