package com.boardgamegeek.io;

import android.net.Uri;

import com.boardgamegeek.provider.BggContract.Publishers;

public class RemotePublisherHandler extends RemoteProducerHandler {
	public RemotePublisherHandler(int producerId) {
		super(producerId);
	}

	@Override
	protected String getRootNodeName() {
		return "companies";
	}

	@Override
	protected String type() {
		return "publisher";
	}

	@Override
	protected Uri createUri() {
		return Publishers.buildPublisherUri(mProducerId);
	}

	@Override
	protected String ItemTag() {
		return "company";
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
		return Publishers.PUBLISHER_ID;
	}

	@Override
	protected String nameColumn() {
		return Publishers.PUBLISHER_NAME;
	}

	@Override
	protected String descriptionColumn() {
		return Publishers.PUBLISHER_DESCRIPTION;
	}

	@Override
	protected String updatedColumn() {
		return Publishers.UPDATED;
	}
}
