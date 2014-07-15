package com.boardgamegeek.model;

import java.util.List;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.ElementList;

public class ForumResponse {
	public static final int PAGE_SIZE = 50;

	@Attribute
	public int id;

	@Attribute
	public String title;

	@Attribute(name = "numthreads")
	public int numberOfThreads;

	@Attribute
	private int numposts;

	@Attribute
	private String lastpostdate;

	@Attribute
	private int noposting;

	@Attribute(name = "termsofuse")
	private String termsOfUse;

	@ElementList
	public List<Thread> threads;

	@Override
	public String toString() {
		return "" + id + ": " + title;
	}

}
