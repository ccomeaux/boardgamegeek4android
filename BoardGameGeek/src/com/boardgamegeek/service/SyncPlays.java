package com.boardgamegeek.service;

import android.content.Context;

import com.boardgamegeek.BggApplication;
import com.boardgamegeek.R;
import com.boardgamegeek.io.RemoteExecutor;
import com.boardgamegeek.io.RemotePlaysHandler;
import com.boardgamegeek.io.XmlHandler.HandlerException;
import com.boardgamegeek.util.HttpUtils;

public class SyncPlays extends SyncTask {

	@Override
	public void execute(RemoteExecutor executor, Context context) throws HandlerException {
		// TODO Auto-generated method stub
		String username = BggApplication.getInstance().getUserName();
		String url = HttpUtils.constructPlaysUrl(username);
		executor.executeGet(url, new RemotePlaysHandler());
	}

	@Override
	public int getNotification() {
		return R.string.notification_text_plays;
	}
}
