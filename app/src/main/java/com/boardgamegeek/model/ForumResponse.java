package com.boardgamegeek.model;

import com.boardgamegeek.util.StringUtils;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.ElementList;

import java.util.List;

public class ForumResponse {
	public static final int PAGE_SIZE = 50;

	@Attribute public int id;

	@Attribute public String title;

	@Attribute private String numthreads;

	public int numberOfThreads() {
		return StringUtils.parseInt(numthreads);
	}

	@Attribute private String numposts;

	@Attribute private String lastpostdate;

	@Attribute private int noposting;

	@ElementList public List<Thread> threads;

	@Override
	public String toString() {
		return id + ": " + title;
	}
}
