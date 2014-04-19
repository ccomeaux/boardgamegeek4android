package com.boardgamegeek.ui.widget;

import java.util.List;

import retrofit.RetrofitError;
import android.text.TextUtils;

public abstract class Data<T> {
	private String mErrorMessage;

	public Data() {
	}

	public Data(Exception e) {
		if (e instanceof RetrofitError) {
			RetrofitError re = (RetrofitError) e;
			if (re.isNetworkError() && re.getResponse() == null) {
				mErrorMessage = "You need to be online to read forums.";
			} else {
				mErrorMessage = re.getMessage();
			}
		} else {
			mErrorMessage = e.getMessage();
		}
	}

	protected abstract List<T> list();

	public boolean hasError() {
		return !TextUtils.isEmpty(mErrorMessage);
	}

	public String getErrorMessage() {
		return mErrorMessage;
	}
}
