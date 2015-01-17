package com.boardgamegeek.service;

import com.boardgamegeek.io.BggService;
import com.boardgamegeek.io.RetryableException;
import com.boardgamegeek.model.CollectionItem;
import com.boardgamegeek.model.CollectionResponse;

import java.util.ArrayList;
import java.util.Map;

import timber.log.Timber;

public class ServiceTask {
	public static final int NO_NOTIFICATION = 0;
	private static final int MAX_RETRIES = 5;
	private static final int RETRY_BACKOFF_IN_MS = 500;

	public ServiceTask() {
		super();
	}

	protected int getNotification() {
		return NO_NOTIFICATION;
	}

	protected CollectionResponse getCollectionResponse(BggService service, String username, Map<String, String> options) {
		int retries = 0;
		while (true) {
			try {
				return service.collection(username, options);
			} catch (Exception e) {
				if (e instanceof RetryableException || e.getCause() instanceof RetryableException) {
					retries++;
					if (retries > MAX_RETRIES) {
						break;
					}
					try {
						Timber.i("...retrying #" + retries);
						Thread.sleep(retries * retries * RETRY_BACKOFF_IN_MS);
					} catch (InterruptedException e1) {
						Timber.i("Interrupted while sleeping before retry " + retries);
						break;
					}
				} else {
					throw e;
				}
			}
		}
		CollectionResponse response = new CollectionResponse();
		response.items = new ArrayList<CollectionItem>();
		return response;
	}
}