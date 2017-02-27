package com.boardgamegeek.service;

import android.content.Context;
import android.support.annotation.NonNull;

import com.boardgamegeek.R;
import com.boardgamegeek.io.BggService;
import com.boardgamegeek.provider.BggContract.Buddies;
import com.boardgamegeek.util.ResolverUtils;
import com.boardgamegeek.util.SelectionBuilder;

import java.util.List;

/**
 * Syncs all buddies that haven't been updated completely.
 */
public class SyncBuddiesDetailUnupdated extends SyncBuddiesDetail {
	public SyncBuddiesDetailUnupdated(Context context, BggService service) {
		super(context, service);
	}

	@Override
	public int getSyncType() {
		return SyncService.FLAG_SYNC_BUDDIES;
	}

	@NonNull
	@Override
	protected String getLogMessage() {
		return "Syncing unupdated buddies...";
	}

	@Override
	protected List<String> getBuddyNames() {
		return ResolverUtils.queryStrings(context.getContentResolver(), Buddies.CONTENT_URI,
			Buddies.BUDDY_NAME,
			SelectionBuilder.whereZeroOrNull(Buddies.UPDATED),
			null,
			Buddies.BUDDY_NAME);
	}

	@Override
	public int getNotificationSummaryMessageId() {
		return R.string.sync_notification_buddies_unupdated;
	}
}
