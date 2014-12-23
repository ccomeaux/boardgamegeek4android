package com.boardgamegeek.service;

import android.content.Context;

import com.boardgamegeek.R;
import com.boardgamegeek.io.BggService;
import com.boardgamegeek.provider.BggContract.Buddies;
import com.boardgamegeek.provider.BggContract.SyncColumns;
import com.boardgamegeek.util.ResolverUtils;

import java.util.List;

/**
 * Syncs all buddies that haven't been updated completely.
 */
public class SyncBuddiesDetailUnupdated extends SyncBuddiesDetail {
	public SyncBuddiesDetailUnupdated(Context context, BggService service) {
		super(context, service);
	}

	@Override
	protected String getLogMessage() {
		return "Syncing unupdated buddies...";
	}

	@Override
	protected List<String> getBuddyNames() {
		return ResolverUtils.queryStrings(mContext.getContentResolver(), Buddies.CONTENT_URI,
			Buddies.BUDDY_NAME, SyncColumns.UPDATED + "=0 OR " + SyncColumns.UPDATED + " IS NULL", null);
	}

	@Override
	public int getNotification() {
		return R.string.sync_notification_buddies_unupdated;
	}
}
