package com.boardgamegeek.model;

import android.os.Parcel;
import android.os.Parcelable;

import com.boardgamegeek.util.DateTimeUtils;
import com.boardgamegeek.util.StringUtils;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

@Root(name = "geeklist")
public class GeekList implements Parcelable {
	private static final SimpleDateFormat FORMAT = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US);
	private long mPostDateTime = DateTimeUtils.UNPARSED_DATE;
	private long mEditDateTime = DateTimeUtils.UNPARSED_DATE;

	public GeekList() {
	}

	@Attribute private int id;

	public int getId() {
		return id;
	}

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

	public String getTitle() {
		return title;
	}

	@Element private String description;

	public String getDescription() {
		return description;
	}

	@ElementList(name = "comment", inline = true, required = false) private List<GeekListComment> comments;

	@ElementList(name = "item", inline = true, required = false) private List<GeekListItem> items;

	public List<GeekListItem> getItems() {
		return items;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel out, int flags) {
		out.writeInt(id);
		out.writeString(postdate);
		out.writeString(editdate);
		out.writeString(thumbs);
		out.writeString(numitems);
		out.writeString(username);
		out.writeString(title);
		out.writeString(description);
	}

	public static final Parcelable.Creator<GeekList> CREATOR = new Parcelable.Creator<GeekList>() {
		public GeekList createFromParcel(Parcel in) {
			return new GeekList(in);
		}

		public GeekList[] newArray(int size) {
			return new GeekList[size];
		}
	};

	private GeekList(Parcel in) {
		id = in.readInt();
		postdate = in.readString();
		editdate = in.readString();
		thumbs = in.readString();
		numitems = in.readString();
		username = in.readString();
		title = in.readString();
		description = in.readString();
	}
}
