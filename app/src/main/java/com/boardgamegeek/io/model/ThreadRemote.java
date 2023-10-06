package com.boardgamegeek.io.model;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Root;

@SuppressWarnings({ "unused", "SpellCheckingInspection" })
@Root(name = "thread")
public class ThreadRemote {
	@Attribute public int id;
	@Attribute public String subject;
	@Attribute public String author;
	@Attribute(required = false) public int numarticles;
	@Attribute public String postdate;
	@Attribute public String lastpostdate;
}
