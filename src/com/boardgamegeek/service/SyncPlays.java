package com.boardgamegeek.service;

import android.content.Context;

import com.boardgamegeek.BggApplication;
import com.boardgamegeek.R;
import com.boardgamegeek.io.RemoteExecutor;
import com.boardgamegeek.io.RemotePlaysHandler;
import com.boardgamegeek.io.XmlHandler;
import com.boardgamegeek.io.XmlHandler.HandlerException;
import com.boardgamegeek.util.HttpUtils;

public class SyncPlays extends SyncTask {

	@Override
	public void execute(RemoteExecutor executor, Context context) throws HandlerException {

		XmlHandler handler = new RemotePlaysHandler();
		String username = BggApplication.getInstance().getUserName();

		String url = HttpUtils.constructPlaysUrlNew(username);
		int page = 1;
		while (executor.executeGet(url + "&page=" + page, handler)) {
			page++;
		}

		url = HttpUtils.constructPlaysUrlOld(username);
		page = 1;
		while (executor.executeGet(url + "&page=" + page, handler)) {
			page++;
		}

		BggApplication.getInstance().putMaxPlayDate("0000-00-00");
	}

	@Override
	public int getNotification() {
		return R.string.notification_text_plays;
	}
}
