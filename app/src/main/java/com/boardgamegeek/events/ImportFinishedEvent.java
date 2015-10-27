package com.boardgamegeek.events;

import android.support.annotation.StringRes;

public class ImportFinishedEvent {
	@StringRes private final int messageId;

	public ImportFinishedEvent(@StringRes int messageId) {
		this.messageId = messageId;
	}

	@StringRes
	public int getMessageId() {
		return messageId;
	}
}
