package com.boardgamegeek.service;

import android.content.Context;

public abstract class UpdateTask extends ServiceTask {
	public abstract void execute(Context context);

	public abstract String getDescription();
}
