package com.boardgamegeek.tasks;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import androidx.annotation.Nullable;

import com.boardgamegeek.events.CollectionItemDeletedEvent;
import com.boardgamegeek.extensions.AsyncTaskKt;
import com.boardgamegeek.provider.BggContract.Collection;

import org.greenrobot.eventbus.EventBus;

import kotlin.Unit;

public class DeleteCollectionItemTask {
	@SuppressLint("StaticFieldLeak") @Nullable private final Context context;
	private final long internalId;

	public DeleteCollectionItemTask(@Nullable Context context, long internalId) {
		this.context = context == null ? null : context.getApplicationContext();
		this.internalId = internalId;
	}

	protected Boolean doInBackground() {
		if (context == null) return false;
		final ContentResolver resolver = context.getContentResolver();
		ContentValues values = new ContentValues();
		values.put(Collection.COLLECTION_DELETE_TIMESTAMP, System.currentTimeMillis());
		return resolver.update(Collection.buildUri(internalId), values, null, null) > 0;
	}

	public void execute() {
		AsyncTaskKt.launchTaskWithResult(
			() -> doInBackground(),
			result -> {
				if (result) {
					EventBus.getDefault().post(new CollectionItemDeletedEvent(internalId));
				}
				return Unit.INSTANCE;
			}
		);
	}
}
