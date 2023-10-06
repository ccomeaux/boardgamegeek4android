package com.boardgamegeek.io.model;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Root;

import androidx.annotation.NonNull;

@SuppressWarnings({ "unused", "SpellCheckingInspection" })
@Root(name = "forum")
public class ForumRemote {
	@Attribute public int id;
	@Attribute public int groupid;
	@Attribute public String title;
	@Attribute public int noposting;
	@Attribute public String description;
	@Attribute public int numthreads;
	@Attribute public int numposts;
	@Attribute public String lastpostdate;

	@NonNull
	@Override
	public String toString() {
		return "" + id + ": " + title + " - " + description;
	}
}
