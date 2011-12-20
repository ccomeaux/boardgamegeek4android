package com.boardgamegeek.service;

import android.content.ContentResolver;
import android.content.Context;
import android.util.Log;

import com.boardgamegeek.BggApplication;
import com.boardgamegeek.R;
import com.boardgamegeek.io.RemoteExecutor;
import com.boardgamegeek.io.RemotePlaysHandler;
import com.boardgamegeek.io.XmlHandler;
import com.boardgamegeek.io.XmlHandler.HandlerException;
import com.boardgamegeek.provider.BggContract.Plays;
import com.boardgamegeek.util.HttpUtils;

public class SyncPlays extends SyncTask {
	private final static String TAG = "SyncPlays";

	@Override
	public void execute(RemoteExecutor executor, Context context) throws HandlerException {

		ContentResolver resolver = context.getContentResolver();
		XmlHandler handler = new RemotePlaysHandler();
		String username = BggApplication.getInstance().getUserName();
		String minDate = BggApplication.getInstance().getMinPlayDate();
		String maxDate = BggApplication.getInstance().getMaxPlayDate();
		long startTime = System.currentTimeMillis();

		String url = HttpUtils.constructPlaysUrlNew(username);
		int page = 1;
		while (executor.executeGet(url + "&page=" + page, handler)) {
			page++;
		}
		String[] selectionArgs = new String[] { String.valueOf(startTime), minDate };
		int count = resolver.delete(Plays.CONTENT_URI, Plays.UPDATED_LIST + "<? AND " + Plays.DATE + ">=?",
				selectionArgs);
		Log.i(TAG, "Deleted " + count + " plays");

		url = HttpUtils.constructPlaysUrlOld(username);
		page = 1;
		while (executor.executeGet(url + "&page=" + page, handler)) {
			page++;
		}
		selectionArgs = new String[] { String.valueOf(startTime), maxDate };
		count = resolver.delete(Plays.CONTENT_URI, Plays.UPDATED_LIST + "<? AND " + Plays.DATE + "<=?", selectionArgs);
		Log.i(TAG, "Deleted " + count + " plays");

		BggApplication.getInstance().putMaxPlayDate("0000-00-00");
	}

	@Override
	public int getNotification() {
		return R.string.notification_text_plays;
	}
}
