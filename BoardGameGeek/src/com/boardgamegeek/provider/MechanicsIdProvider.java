package com.boardgamegeek.provider;

import android.net.Uri;

import com.boardgamegeek.provider.BggContract.Mechanics;
import com.boardgamegeek.provider.BggDatabase.Tables;
import com.boardgamegeek.util.SelectionBuilder;

public class MechanicsIdProvider extends BaseProvider {

	@Override
	protected SelectionBuilder buildSimpleSelection(Uri uri) {
		int mechanicId = Mechanics.getMechanicId(uri);
		return new SelectionBuilder().table(Tables.MECHANICS).whereEquals(Mechanics.MECHANIC_ID, mechanicId);
	}

	@Override
	protected String getPath() {
		return "mechanics/#";
	}

	@Override
	protected String getType(Uri uri) {
		return Mechanics.CONTENT_ITEM_TYPE;
	}
}
