package com.boardgamegeek.events;

import android.support.annotation.StringRes;

public class ImportFinishedEvent {
	@StringRes public int messageId;

	public ImportFinishedEvent(@StringRes int messageId) {
		this.messageId = messageId;
	}
}
