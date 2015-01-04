package com.boardgamegeek.service;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;

import com.boardgamegeek.io.Adapter;
import com.boardgamegeek.io.BggService;
import com.boardgamegeek.model.Person;
import com.boardgamegeek.provider.BggContract.Designers;

import timber.log.Timber;

public class SyncDesigner extends UpdateTask {
	private int mDesignerId;

	public SyncDesigner(int designerId) {
		mDesignerId = designerId;
	}

	@Override
	public String getDescription() {
		return "Sync designer ID=" + mDesignerId;
	}

	@Override
	public void execute(Context context) {
		BggService service = Adapter.create();
		Person person = service.person(BggService.PERSON_TYPE_DESIGNER, mDesignerId);
		Uri uri = Designers.buildDesignerUri(mDesignerId);
		context.getContentResolver().update(uri, toValues(person), null, null);
		Timber.i("Synced Designer " + mDesignerId);
	}

	private static ContentValues toValues(Person person) {
		ContentValues values = new ContentValues();
		values.put(Designers.DESIGNER_NAME, person.name);
		values.put(Designers.DESIGNER_DESCRIPTION, person.description);
		values.put(Designers.UPDATED, System.currentTimeMillis());
		return values;
	}
}
