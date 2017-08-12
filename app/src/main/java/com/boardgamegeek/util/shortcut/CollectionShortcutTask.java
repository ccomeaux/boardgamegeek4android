package com.boardgamegeek.util.shortcut;

import android.content.Context;
import android.content.Intent;

import com.boardgamegeek.R;
import com.boardgamegeek.ui.CollectionActivity;

public class CollectionShortcutTask extends ShortcutTask {
	private final long viewId;
	private final String viewName;

	public CollectionShortcutTask(Context context, long viewId, String viewName) {
		super(context);
		this.viewId = viewId;
		this.viewName = viewName;
	}

	@Override
	protected String getShortcutName() {
		return viewName;
	}

	@Override
	protected Intent createIntent() {
		return CollectionActivity.createIntentAsShortcut(context, viewId);
	}

	@Override
	protected int getShortcutIconResId() {
		return R.drawable.ic_shortcut_ic_collection;
	}

	@Override
	protected String getId() {
		return "collection_view-" + viewId;
	}
}
