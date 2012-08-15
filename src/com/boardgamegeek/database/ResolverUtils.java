package com.boardgamegeek.database;

import java.util.ArrayList;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.OperationApplicationException;
import android.os.RemoteException;

import com.boardgamegeek.provider.BggContract;

public class ResolverUtils {
	public static void applyBatch(ContentResolver resolver, ArrayList<ContentProviderOperation> batch) {
		if (batch.size() > 0) {
			try {
				resolver.applyBatch(BggContract.CONTENT_AUTHORITY, batch);
			} catch (RemoteException e) {
				throw new RuntimeException(e);
			} catch (OperationApplicationException e) {
				throw new RuntimeException(e);
			}
		}
	}
}
