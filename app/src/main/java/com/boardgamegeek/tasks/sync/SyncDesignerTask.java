package com.boardgamegeek.tasks.sync;


import android.content.ContentValues;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;

import com.boardgamegeek.R;
import com.boardgamegeek.io.BggService;
import com.boardgamegeek.model.Person;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.provider.BggContract.Designers;
import com.boardgamegeek.tasks.sync.SyncDesignerTask.CompletedEvent;

import retrofit2.Call;
import timber.log.Timber;

public class SyncDesignerTask extends SyncTask<Person, CompletedEvent> {
	private final int designerId;

	public SyncDesignerTask(Context context, int designerId) {
		super(context);
		this.designerId = designerId;
	}

	@Override
	@StringRes
	protected int getTypeDescriptionResId() {
		return R.string.title_designer;
	}

	@Override
	protected Call<Person> createCall() {
		return bggService.person(BggService.PERSON_TYPE_DESIGNER, designerId);
	}

	@Override
	protected boolean isRequestParamsValid() {
		return super.isRequestParamsValid() && designerId != BggContract.INVALID_ID;
	}

	@Override
	protected void persistResponse(Person designer) {
		if (context == null) return;
		context.getContentResolver().update(Designers.buildDesignerUri(designerId), toValues(designer), null, null);
		Timber.i("Synced designer '%s'", designerId);
	}

	@NonNull
	@Override
	protected CompletedEvent createEvent(String errorMessage) {
		return new CompletedEvent(errorMessage, designerId);
	}

	@NonNull
	private static ContentValues toValues(@NonNull Person designer) {
		ContentValues values = new ContentValues();
		values.put(Designers.DESIGNER_NAME, designer.name);
		values.put(Designers.DESIGNER_DESCRIPTION, designer.description);
		values.put(Designers.UPDATED, System.currentTimeMillis());
		return values;
	}

	public class CompletedEvent extends SyncTask.CompletedEvent {
		private final int designerId;

		public CompletedEvent(String errorMessage, int designerId) {
			super(errorMessage);
			this.designerId = designerId;
		}

		public int getDesignerId() {
			return designerId;
		}
	}
}
