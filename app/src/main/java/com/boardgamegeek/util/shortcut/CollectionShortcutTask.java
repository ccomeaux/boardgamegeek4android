package com.boardgamegeek.util.shortcut;

import android.content.Context;
import android.content.Intent;

import com.boardgamegeek.util.ActivityUtils;
import com.boardgamegeek.util.ShortcutUtils;

public class CollectionShortcutTask extends ShortcutTask {
	private final long viewId;
	private final String viewName;

	public CollectionShortcutTask(Context context, long viewId, String viewName, String thumbnailUrl) {
		super(context, thumbnailUrl);
		this.viewId = viewId;
		this.viewName = viewName;
	}

	@Override
	protected Intent createIntent() {
		Intent intent = ActivityUtils.createCollectionIntent(context, viewId);
		return ShortcutUtils.createShortcutIntent(context, viewName, intent);
	}
}
