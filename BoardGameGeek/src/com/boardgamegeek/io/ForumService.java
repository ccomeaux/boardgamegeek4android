package com.boardgamegeek.io;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

import retrofit.http.GET;
import retrofit.http.Query;
import android.os.Parcel;
import android.os.Parcelable;

public interface ForumService {
	public static String TYPE_REGION = "region";
	public static String TYPE_THING = "thing";

	public static final int REGION_BOARDGAME = 1;
	public static final int REGION_RPG = 2;
	public static final int REGION_VIDEOGAME = 3;

	static final SimpleDateFormat FORMAT = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US);

	@GET("/xmlapi2/forumlist")
	ForumListResponse listForums(@Query("type") String type, @Query("id") int id);

	static class ForumListResponse {
		@Attribute(name = "type")
		private String type;

		@Attribute(name = "id")
		int id;

		@Attribute(name = "termsofuse")
		private String termsOfUse;

		@ElementList(inline = true)
		public List<Forum> forums;

		@Override
		public String toString() {
			return "" + id + ": " + type;
		}
	}

	@Root(name = "forum")
	static class Forum implements Parcelable {
		public Forum() {
		}

		@Attribute
		public int id;

		@Attribute
		private int groupid;

		@Attribute
		public String title;

		@Attribute
		private int noposting;

		@Attribute
		private String description;

		@Attribute(name = "numthreads")
		public int numberOfThreads;

		@Attribute
		private int numposts;

		@Attribute
		private String lastpostdate;

		public boolean isHeader() {
			return noposting == 1;
		}

		public long lastPostDate() {
			try {
				return FORMAT.parse(lastpostdate).getTime();
			} catch (ParseException e) {
				return 0;
			}
		}

		@Override
		public String toString() {
			return "" + id + ": " + title + " - " + description;
		}

		@Override
		public int describeContents() {
			return 0;
		}

		@Override
		public void writeToParcel(Parcel dest, int flags) {
			dest.writeInt(id);
			dest.writeInt(groupid);
			dest.writeString(title);
			dest.writeInt(noposting);
			dest.writeString(description);
			dest.writeInt(numberOfThreads);
			dest.writeInt(numposts);
			dest.writeString(lastpostdate);
		}

		public Forum(Parcel in) {
			id = in.readInt();
			groupid = in.readInt();
			title = in.readString();
			noposting = in.readInt();
			description = in.readString();
			numberOfThreads = in.readInt();
			numposts = in.readInt();
			lastpostdate = in.readString();
		}

		public static final Parcelable.Creator<Forum> CREATOR = new Parcelable.Creator<Forum>() {
			public Forum createFromParcel(Parcel in) {
				return new Forum(in);
			}

			public Forum[] newArray(int size) {
				return new Forum[size];
			}
		};
	}
}
