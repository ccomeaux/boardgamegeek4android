package com.boardgamegeek.auth;

import static com.boardgamegeek.util.LogUtils.LOGV;
import static com.boardgamegeek.util.LogUtils.makeLogTag;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class AuthenticationService extends Service {
	private static final String TAG = makeLogTag(AuthenticationService.class);

	private Authenticator mAuthenticator;

	@Override
	public void onCreate() {
		LOGV(TAG, "BoardGameGeek Authentication Service started.");
		mAuthenticator = new Authenticator(this);
	}

	@Override
	public void onDestroy() {
		LOGV(TAG, "BoardGameGeek Authentication Service stopped.");
	}

	@Override
	public IBinder onBind(Intent intent) {
		LOGV(TAG, "getBinder: returning the AccountAuthenticator binder for intent " + intent);
		return mAuthenticator.getIBinder();
	}
}
