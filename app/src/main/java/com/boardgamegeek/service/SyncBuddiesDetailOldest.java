package com.boardgamegeek.service;

import android.content.Context;
import android.support.annotation.NonNull;

import com.boardgamegeek.R;
import com.boardgamegeek.io.BggService;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.util.ResolverUtils;

import java.util.List;

/**
 * Syncs a number of buddies that haven't been updated in a while.
 */
public class SyncBuddiesDetailOldest extends SyncBuddiesDetail {
	private static final int SYNC_LIMIT = 1;

	public SyncBuddiesDetailOldest(Context context, BggService service) {
		super(context, service);
	}

	@Override
	public int getSyncType() {
		return SyncService.FLAG_SYNC_BUDDIES;
	}

	@NonNull
	@Override
	protected String getLogMessage() {
		return "Syncing oldest buddies...";
	}

	@Override
	protected List<String> getBuddyNames() {
		return ResolverUtils.queryStrings(context.getContentResolver(), BggContract.Buddies.CONTENT_URI,
			BggContract.Buddies.BUDDY_NAME, null, null,
			BggContract.Buddies.UPDATED + " LIMIT " + SYNC_LIMIT);
	}

	@Override
	public int getNotification() {
		return R.string.sync_notification_buddies_oldest;
	}
}
