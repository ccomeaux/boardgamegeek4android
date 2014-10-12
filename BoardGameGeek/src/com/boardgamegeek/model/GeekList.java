package com.boardgamegeek.model;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

import com.boardgamegeek.util.DateTimeUtils;
import com.boardgamegeek.util.StringUtils;

@Root(name = "geeklist")
public class GeekList {
	private static final SimpleDateFormat FORMAT = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US);
	private long mPostDateTime = DateTimeUtils.UNPARSED_DATE;
	private long mEditDateTime = DateTimeUtils.UNPARSED_DATE;

	@Attribute private int id;

	@Attribute private String termsofuse;

	@Element private String postdate;

	@Element(name = "postdate_timestamp") private String postdateTimestamp;

	public long getPostDate() {
		mPostDateTime = DateTimeUtils.tryParseDate(mPostDateTime, postdate, FORMAT);
		return mPostDateTime;
	}

	@Element private String editdate;

	@Element(name = "editdate_timestamp") private String editdateTimestamp;

	public long getEditDate() {
		mEditDateTime = DateTimeUtils.tryParseDate(mEditDateTime, editdate, FORMAT);
		return mEditDateTime;
	}

	@Element private String thumbs;

	public int getThumbs() {
		return StringUtils.parseInt(thumbs);
	}

	@Element private String numitems;

	public int getNumberOfItems() {
		return StringUtils.parseInt(numitems);
	}

	@Element private String username;

	public String getUsername() {
		return username;
	}

	@Element private String title;

	@Element private String description;

	@ElementList(name = "comment", inline = true, required = false) private List<GeekListComment> comments;

	@ElementList(name = "item", inline = true, required = false) private List<GeekListItem> items;

	public List<GeekListItem> getItems() {
		return items;
	}
}
