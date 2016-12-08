package com.boardgamegeek.model;

import android.os.Parcel;
import android.os.Parcelable;

import com.boardgamegeek.util.DateTimeUtils;
import com.boardgamegeek.util.StringUtils;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Root;
import org.simpleframework.xml.Text;

import java.text.SimpleDateFormat;
import java.util.Locale;

@Root(name = "comment")
public class GeekListComment implements Parcelable {
	private static final SimpleDateFormat FORMAT = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US);
	private long dateTime = DateTimeUtils.UNPARSED_DATE;
	private long postDateTime = DateTimeUtils.UNPARSED_DATE;
	private long editDateTime = DateTimeUtils.UNPARSED_DATE;

	@SuppressWarnings("unused")
	public GeekListComment() {
	}

	@Attribute private String date;
	@Attribute private String editdate;
	@Attribute private String postdate;
	@Attribute private String thumbs;
	@Attribute private String username;
	@Text private String content;

	public long getDate() {
		dateTime = DateTimeUtils.tryParseDate(dateTime, date, FORMAT);
		return dateTime;
	}

	public long getPostDate() {
		postDateTime = DateTimeUtils.tryParseDate(postDateTime, postdate, FORMAT);
		return postDateTime;
	}

	public long getEditDate() {
		editDateTime = DateTimeUtils.tryParseDate(editDateTime, editdate, FORMAT);
		return editDateTime;
	}

	public int getNumberOfThumbs() {
		return StringUtils.parseInt(thumbs);
	}

	public String getUsername() {
		return username;
	}

	public String getContent() {
		return content.trim();
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel out, int flags) {
		out.writeString(date);
		out.writeString(editdate);
		out.writeString(postdate);
		out.writeString(thumbs);
		out.writeString(username);
		out.writeString(content);
	}

	public static final Parcelable.Creator<GeekListComment> CREATOR = new Parcelable.Creator<GeekListComment>() {
		public GeekListComment createFromParcel(Parcel in) {
			return new GeekListComment(in);
		}

		public GeekListComment[] newArray(int size) {
			return new GeekListComment[size];
		}
	};

	private GeekListComment(Parcel in) {
		date = in.readString();
		editdate = in.readString();
		postdate = in.readString();
		thumbs = in.readString();
		username = in.readString();
		content = in.readString();
	}
}
