package com.boardgamegeek.provider;

import android.net.Uri;

import com.boardgamegeek.provider.BggContract.Designers;
import com.boardgamegeek.provider.BggDatabase.Tables;
import com.boardgamegeek.util.SelectionBuilder;

public class DesignersIdCollectionProvider extends BaseProvider {
	@Override
	protected SelectionBuilder buildSimpleSelection(Uri uri) {
		int designerId = Designers.getDesignerId(uri);
		return new SelectionBuilder().table(Tables.DESIGNER_JOIN_GAMES_JOIN_COLLECTION).whereEquals(Designers.DESIGNER_ID, designerId);
	}

	@Override
	protected String getPath() {
		return "designers/#/collection";
	}
}
