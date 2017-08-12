package com.boardgamegeek.util.shortcut;

import android.content.Context;
import android.content.Intent;

import com.boardgamegeek.R;
import com.boardgamegeek.ui.CollectionActivity;
import com.boardgamegeek.util.ShortcutUtils;

public class CollectionShortcutTask extends ShortcutTask {
	private final long viewId;
	private final String viewName;

	public CollectionShortcutTask(Context context, long viewId, String viewName) {
		super(context);
		this.viewId = viewId;
		this.viewName = viewName;
	}

	@Override
	protected Intent createIntent() {
		Intent intent = CollectionActivity.createIntentAsShortcut(context, viewId);
		return ShortcutUtils.createShortcutIntent(context, viewName, intent, R.drawable.ic_shortcut_ic_collection);
	}
}
