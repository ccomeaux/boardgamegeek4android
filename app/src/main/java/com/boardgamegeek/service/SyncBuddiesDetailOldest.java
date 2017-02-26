package com.boardgamegeek.service;

import android.content.Context;
import android.support.annotation.NonNull;

import com.boardgamegeek.R;
import com.boardgamegeek.io.BggService;
import com.boardgamegeek.provider.BggContract.Buddies;
import com.boardgamegeek.util.MathUtils;
import com.boardgamegeek.util.ResolverUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Syncs roughly 7% of buddies that haven't been updated in the longer while.
 */
public class SyncBuddiesDetailOldest extends SyncBuddiesDetail {
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
		int count = ResolverUtils.queryInt(context.getContentResolver(),
			Buddies.CONTENT_URI,
			"count(*) AS count");

		if (count == 0) return new ArrayList<>(0);

		int syncLimit = count / 14; // will sync all buddies every 2 weeks
		syncLimit = MathUtils.constrain(syncLimit, 1, 16);

		return ResolverUtils.queryStrings(context.getContentResolver(),
			Buddies.CONTENT_URI,
			Buddies.BUDDY_NAME,
			null,
			null,
			Buddies.UPDATED + " LIMIT " + syncLimit);
	}

	@Override
	public int getNotificationSummaryMessageId() {
		return R.string.sync_notification_buddies_oldest;
	}
}
