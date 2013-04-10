package com.boardgamegeek.io;

import android.net.Uri;

import com.boardgamegeek.provider.BggContract.Designers;

public class RemoteDesignerHandler extends RemoteProducerHandler {
	public RemoteDesignerHandler(int producerId) {
		super(producerId);
	}

	@Override
	protected String type() {
		return "designer";
	}

	@Override
	protected Uri createUri() {
		return Designers.buildDesignerUri(mProducerId);
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
		return Designers.DESIGNER_ID;
	}

	@Override
	protected String nameColumn() {
		return Designers.DESIGNER_NAME;
	}

	@Override
	protected String descriptionColumn() {
		return Designers.DESIGNER_DESCRIPTION;
	}

	@Override
	protected String updatedColumn() {
		return Designers.UPDATED;
	}

	@Override
	protected String getRootNodeName() {
		return "people";
	}
}
