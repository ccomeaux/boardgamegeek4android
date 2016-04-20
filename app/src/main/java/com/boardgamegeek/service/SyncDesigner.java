package com.boardgamegeek.service;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;

import com.boardgamegeek.R;
import com.boardgamegeek.io.Adapter;
import com.boardgamegeek.io.BggService;
import com.boardgamegeek.model.Person;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.provider.BggContract.Designers;

import java.io.IOException;

import timber.log.Timber;

public class SyncDesigner extends UpdateTask {
	private final int designerId;

	public SyncDesigner(int designerId) {
		this.designerId = designerId;
	}

	@NonNull
	@Override
	public String getDescription(Context context) {
		if (isValid()) {
			return context.getString(R.string.sync_msg_designer_valid, designerId);
		}
		return context.getString(R.string.sync_msg_designer_invalid);
	}

	@Override
	public boolean isValid() {
		return designerId != BggContract.INVALID_ID;
	}

	@Override
	public void execute(@NonNull Context context) {
		BggService service = Adapter.createForXml();
		try {
			Person person = service.person(BggService.PERSON_TYPE_DESIGNER, designerId).execute().body();
			Uri uri = Designers.buildDesignerUri(designerId);
			context.getContentResolver().update(uri, toValues(person), null, null);
			Timber.i("Synced Designer " + designerId);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@NonNull
	private static ContentValues toValues(@NonNull Person person) {
		ContentValues values = new ContentValues();
		values.put(Designers.DESIGNER_NAME, person.name);
		values.put(Designers.DESIGNER_DESCRIPTION, person.description);
		values.put(Designers.UPDATED, System.currentTimeMillis());
		return values;
	}
}
