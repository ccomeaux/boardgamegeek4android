package com.boardgamegeek.service;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.boardgamegeek.R;
import com.boardgamegeek.io.RemoteBuddiesHandler;
import com.boardgamegeek.io.RemoteExecutor;
import com.boardgamegeek.io.XmlHandler.HandlerException;
import com.boardgamegeek.provider.BggContract.Buddies;

public class SyncBuddiesList extends SyncTask {

	@Override
	public void execute(RemoteExecutor executor, Context context)
		throws HandlerException {

		ContentResolver resolver = context.getContentResolver();

		// TODO:
		final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
		String mUsername = preferences.getString("username", "");

		final long startTime = System.currentTimeMillis();
		executor.executePagedGet(SyncService.BASE_URL_2 + "user?name=" + mUsername + "&buddies=1",
			new RemoteBuddiesHandler());
		resolver.delete(Buddies.CONTENT_URI, Buddies.UPDATED_LIST + "<?", new String[] { "" + startTime });
	}

	@Override
	public int getNotification() {
		return R.string.notification_text_buddies_list;
	}
}
