package com.boardgamegeek.model;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Root(name = "geeklist")
public class GeekList implements Parcelable {
	public static final SimpleDateFormat FORMAT = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US);

	@SuppressWarnings("unused")
	public GeekList() {
	}

	@Attribute private int id;
	@Element public String postdate;
	@Element public String editdate;
	@Element(name = "postdate_timestamp") private String postdateTimestamp;
	@Element(name = "editdate_timestamp") private String editdateTimestamp;
	@Element public String thumbs;
	@Element public String numitems;
	@Element public String username;
	@Element private String title;
	@Element public String description;
	@ElementList(name = "comment", inline = true, required = false) private ArrayList<GeekListComment> comments;
	@ElementList(name = "item", inline = true, required = false) private List<GeekListItem> items;

	public int getId() {
		return id;
	}

	public String getTitle() {
		if (TextUtils.isEmpty(title)) return "";
		return title.trim();
	}

	public ArrayList<GeekListComment> getComments() {
		return comments;
	}

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
