package com.boardgamegeek.pref;

import android.content.Context;
import android.util.AttributeSet;

import com.boardgamegeek.tasks.ResetBuddiesTask;
import com.boardgamegeek.tasks.ToastingAsyncTask;

public class ResetBuddiesDialogPreference extends AsyncDialogPreference {
	public ResetBuddiesDialogPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	protected ToastingAsyncTask getTask() {
		return new ResetBuddiesTask(getContext());
	}
}
