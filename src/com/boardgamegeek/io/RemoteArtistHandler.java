package com.boardgamegeek.io;

import android.net.Uri;

import com.boardgamegeek.provider.BggContract.Artists;

public class RemoteArtistHandler extends RemoteProducerHandler {
	public RemoteArtistHandler(int producerId) {
		super(producerId);
	}

	@Override
	protected String type() {
		return "artist";
	}

	@Override
	protected Uri createUri() {
		return Artists.buildArtistUri(mProducerId);
	}

	@Override
	protected String ItemTag() {
		return "person";
	}

	@Override
	protected String nameTag() {
		return "name";
	}

	@Override
	protected String descriptionTag() {
		return "description";
	}

	@Override
	protected String idColumn() {
		return Artists.ARTIST_ID;
	}

	@Override
	protected String nameColumn() {
		return Artists.ARTIST_NAME;
	}

	@Override
	protected String descriptionColumn() {
		return Artists.ARTIST_DESCRIPTION;
	}

	@Override
	protected String updatedColumn() {
		return Artists.UPDATED;
	}

	@Override
	protected String getRootNodeName() {
		return "people";
	}
}
