package com.boardgamegeek.ui.loader;

import android.text.TextUtils;

import java.util.List;

public abstract class Data<T> {
	private String errorMessage;

	public Data() {
	}

	public Data(Exception e) {
		errorMessage = e.getMessage();
	}

	protected abstract List<T> list();

	public boolean hasError() {
		return !TextUtils.isEmpty(errorMessage);
	}

	public String getErrorMessage() {
		return errorMessage;
	}
}
