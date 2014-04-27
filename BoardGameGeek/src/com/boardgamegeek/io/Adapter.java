package com.boardgamegeek.io;

import com.boardgamegeek.io.xml.SimpleXMLConverter;

import retrofit.RestAdapter;

public class Adapter {
	public static RestAdapter get() {
		return new RestAdapter.Builder().setEndpoint("http://www.boardgamegeek.com/")
			.setConverter(new SimpleXMLConverter()).build();
	}

	public static BggService create() {
		return new RestAdapter.Builder().setEndpoint("http://www.boardgamegeek.com/")
			.setConverter(new SimpleXMLConverter()).build().create(BggService.class);
	}
}
