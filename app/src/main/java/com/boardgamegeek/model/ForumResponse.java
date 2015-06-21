package com.boardgamegeek.model;

import com.boardgamegeek.util.StringUtils;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.ElementList;

import java.util.ArrayList;
import java.util.List;

public class ForumResponse {
	public static final int PAGE_SIZE = 50;

	@Attribute private int id;

	@Attribute private String title;

	@Attribute private String numthreads;

	@Attribute private String numposts;

	@Attribute private String lastpostdate;

	@Attribute private int noposting;

	@ElementList private List<Thread> threads;

	@Override
	public String toString() {
		return id + ": " + title;
	}

	public int numberOfThreads() {
		return StringUtils.parseInt(numthreads);
	}

	public List<Thread> getThreads() {
		if (threads == null) {
			return new ArrayList<>();
		}
		return threads;
	}
}
