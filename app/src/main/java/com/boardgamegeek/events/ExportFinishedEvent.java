package com.boardgamegeek.events;

import android.support.annotation.StringRes;

public class ExportFinishedEvent {
	@StringRes public int messageId;

	public ExportFinishedEvent(@StringRes int messageId) {
		this.messageId = messageId;
	}
}
