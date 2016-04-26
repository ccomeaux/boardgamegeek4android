package com.boardgamegeek.io;

import com.boardgamegeek.model.User;

import java.io.IOException;

import retrofit2.Call;
import retrofit2.Response;
import timber.log.Timber;

public class UserRequest {
	private final BggService bggService;
	private final String name;
	private final boolean includeBuddies;

	public UserRequest(BggService service, String name) {
		this.bggService = service;
		this.name = name;
		this.includeBuddies = false;
	}

	public UserRequest(BggService service, String name, boolean includeBuddies) {
		this.bggService = service;
		this.name = name;
		this.includeBuddies = includeBuddies;
	}

	public User execute() {
		Call<User> call;
		try {
			if (includeBuddies) {
				call = bggService.user(name, 1, 1);
			} else {
				call = bggService.user(name);
			}
			Response<User> response = call.execute();
			if (response.isSuccessful()) {
				return response.body();
			} else {
				Timber.w("Unsuccessful user fetch with code: %s", response.code());
			}
		} catch (IOException e) {
			// This is probably caused by a timeout, but for now treat it like an empty response
			Timber.w(e, "Unsuccessful user fetch");
		}
		return new User();
	}
}
