package com.boardgamegeek.model;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.util.DateTimeUtils;
import com.boardgamegeek.util.StringUtils;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

@Root(name = "item")
public class GeekListItem {
	private static final SimpleDateFormat FORMAT = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US);

	private long mPostDateTime = DateTimeUtils.UNPARSED_DATE;
	private long mEditDateTime = DateTimeUtils.UNPARSED_DATE;

	@Attribute private String id;

	@Attribute private String imageid;

	public int imageId() {
		return Integer.valueOf(imageid);
	}

	@Attribute private String objectid;

	public int getGameId() {
		if ("thing".equals(objecttype) && "boardgame".equals(subtype)) {
			return Integer.valueOf(objectid);
		}
		return BggContract.INVALID_ID;
	}

	@Attribute public String objectname;

	@Attribute private String objecttype;

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

	@Attribute private String subtype;

	@Attribute private String thumbs;

	public int getThumbCount() {
		return StringUtils.parseInt(thumbs, 0);
	}

	@Attribute public String username;

	@Element(required = false) public String body;

	@ElementList(name = "comment", inline = true, required = false) private List<GeekListComment> comments;
}
