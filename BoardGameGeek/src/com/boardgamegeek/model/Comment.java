package com.boardgamegeek.model;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Root;
import org.simpleframework.xml.Text;

@Root(name = "comment")
public class Comment {
	@Attribute
	public String date;

	@Attribute
	public String editdate;

	@Attribute
	public String postdate;

	@Attribute
	public String thumbs;

	@Attribute
	public String username;
	
	@Text
    public String content;
}
