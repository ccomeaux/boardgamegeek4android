package com.boardgamegeek.io.model;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.ElementList;

import java.util.List;

public class ForumResponse {
	public static final int PAGE_SIZE = 50;
	@Attribute private int id;
	@Attribute private String title;
	@Attribute public String numthreads;
	@Attribute private String numposts;
	@Attribute private String lastpostdate;
	@Attribute private int noposting;
	@ElementList public List<ForumThread> threads;
}
