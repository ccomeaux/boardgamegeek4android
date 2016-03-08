package com.boardgamegeek.events;

import android.support.annotation.StringRes;

public class ColorAssignmentCompleteEvent {
	private final boolean isSuccessful;
	@StringRes private final int messageId;

	public ColorAssignmentCompleteEvent(boolean isSuccessful, @StringRes int messageId) {
		this.isSuccessful = isSuccessful;
		this.messageId = messageId;
	}

	public boolean isSuccessful() {
		return isSuccessful;
	}

	@StringRes
	public int getMessageId() {
		return messageId;
	}
}
