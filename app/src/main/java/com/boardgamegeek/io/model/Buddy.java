package com.boardgamegeek.io.model;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Root;

@Root(name = "buddy")
public class Buddy {
	@Attribute public String id;
	@Attribute public String name;
}
