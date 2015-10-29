package com.boardgamegeek.service;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;

import com.boardgamegeek.io.Adapter;
import com.boardgamegeek.io.BggService;
import com.boardgamegeek.model.Person;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.provider.BggContract.Designers;

import timber.log.Timber;

public class SyncDesigner extends UpdateTask {
	private final int designerId;

	public SyncDesigner(int designerId) {
		this.designerId = designerId;
	}

	@NonNull
	@Override
	public String getDescription() {
		// TODO use resources for description
		if (designerId == BggContract.INVALID_ID) {
			return "update an unknown designer";
		}
		return "update designer " + designerId;
	}

	@Override
	public void execute(@NonNull Context context) {
		BggService service = Adapter.create();
		Person person = service.person(BggService.PERSON_TYPE_DESIGNER, designerId);
		Uri uri = Designers.buildDesignerUri(designerId);
		context.getContentResolver().update(uri, toValues(person), null, null);
		Timber.i("Synced Designer " + designerId);
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
