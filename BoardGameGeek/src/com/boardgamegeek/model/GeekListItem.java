package com.boardgamegeek.model;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

import com.boardgamegeek.util.DateTimeUtils;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

@Root(name = "item")
public class GeekListItem {
	private static final SimpleDateFormat FORMAT = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US);

	private long mPostDateTime = DateTimeUtils.UNPARSED_DATE;
	private long mEditDateTime = DateTimeUtils.UNPARSED_DATE;

	@Attribute public String id;

	@Attribute public String imageid;

	@Attribute public String objectid;

	@Attribute public String objectname;

	@Attribute public String objecttype;

	@Attribute private String postdate;

	public long postDate() {
		mPostDateTime = DateTimeUtils.tryParseDate(mPostDateTime, postdate, FORMAT);
		return mPostDateTime;
	}

	@Attribute private String editdate;

	public long editDate() {
		mEditDateTime = DateTimeUtils.tryParseDate(mEditDateTime, editdate, FORMAT);
		return mEditDateTime;
	}

	@Attribute public String subtype;

	@Attribute public String thumbs;

	@Attribute public String username;

	@Element(required = false) public String body;

	@ElementList(name = "comment", inline = true, required = false) public List<GeekListComment> comments;
}
