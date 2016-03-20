package com.boardgamegeek.events;

import android.support.annotation.StringRes;

public class ExportFinishedEvent {
	@StringRes private final int messageId;

	public ExportFinishedEvent(@StringRes int messageId) {
		this.messageId = messageId;
	}

	@StringRes
	public int getMessageId() {
		return messageId;
	}
}
