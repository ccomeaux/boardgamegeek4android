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

public interface ForumService {
	public static String TYPE_REGION = "region";
	public static String TYPE_THING = "thing";

	public static final int REGION_BOARDGAME = 1;
	public static final int REGION_RPG = 2;
	public static final int REGION_VIDEOGAME = 3;

	static final SimpleDateFormat FORMAT = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US);
	static final long UNPARSED_DATE = -2;
	static final long UNKNOWN_DATE = -1;

	@GET("/xmlapi2/forumlist")
	ForumListResponse listForums(@Query("type") String type, @Query("id") int id);

	static class ForumListResponse {
		@Attribute
		public String type;

		@Attribute
		public int id;

		@Attribute(name = "termsofuse")
		private String termsOfUse;

		@ElementList(inline = true)
		public List<Forum> forums;
	}

	@Root(name = "forum")
	static class Forum {
		private long mLastPostDateTime = UNPARSED_DATE;

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
			if (mLastPostDateTime == UNPARSED_DATE) {
				try {
					mLastPostDateTime = FORMAT.parse(lastpostdate).getTime();
				} catch (ParseException e) {
					mLastPostDateTime = UNKNOWN_DATE;
				}
			}
			return mLastPostDateTime;
		}

		@Override
		public String toString() {
			return "" + id + ": " + title + " - " + description;
		}
	}

	@GET("/xmlapi2/forum")
	ForumResponse listThreads(@Query("id") int id, @Query("page") int page);

	static class ForumResponse {
		public static final int PAGE_SIZE = 50;

		@Attribute
		public int id;

		@Attribute
		public String title;

		@Attribute(name = "numthreads")
		public int numberOfThreads;

		@Attribute
		private int numposts;

		@Attribute
		private String lastpostdate;

		@Attribute
		private int noposting;

		@Attribute(name = "termsofuse")
		private String termsOfUse;

		@ElementList
		public List<Thread> threads;

		@Override
		public String toString() {
			return "" + id + ": " + title;
		}
	}

	@Root(name = "thread")
	static class Thread {
		private long mPostDateTime;
		private long mLastPostDateTime;

		@Attribute
		public int id;

		@Attribute
		public String subject;

		@Attribute
		public String author;

		@Attribute(name = "numarticles")
		public int numberOfArticles;

		@Attribute
		public String postdate;

		@Attribute
		public String lastpostdate;

		public long postDate() {
			if (mPostDateTime == UNPARSED_DATE) {
				try {
					mPostDateTime = FORMAT.parse(lastpostdate).getTime();
				} catch (ParseException e) {
					mPostDateTime = UNKNOWN_DATE;
				}
			}
			return mPostDateTime;
		}

		public long lastPostDate() {
			if (mLastPostDateTime == UNPARSED_DATE) {
				try {
					mLastPostDateTime = FORMAT.parse(lastpostdate).getTime();
				} catch (ParseException e) {
					mLastPostDateTime = UNKNOWN_DATE;
				}
			}
			return mLastPostDateTime;
		}
	}
}
