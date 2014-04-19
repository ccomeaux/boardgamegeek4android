package com.boardgamegeek.io;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
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
    static final SimpleDateFormat FORMAT2 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssz", Locale.US);
	static final long UNPARSED_DATE = -2;
	static final long UNKNOWN_DATE = -1;

	@GET("/xmlapi2/forumlist")
	ForumListResponse forumList(@Query("type") String type, @Query("id") int id);

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
	ForumResponse forum(@Query("id") int id, @Query("page") int page);

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
		private long mPostDateTime = UNPARSED_DATE;
		private long mLastPostDateTime = UNPARSED_DATE;

		@Attribute
		public int id;

		@Attribute
		public String subject;

		@Attribute
		public String author;

		@Attribute(name = "numarticles")
		public int numberOfArticles;

		@Attribute
		private String postdate;

		@Attribute
		private String lastpostdate;

		public long postDate() {
			if (mPostDateTime == UNPARSED_DATE) {
				try {
					mPostDateTime = FORMAT.parse(postdate).getTime();
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

	// minarticleid=NNN Filters the results so that only articles with an equal or higher id than NNN will be returned.
	// minarticledate=YYYY-MM-DD Filters the results so that only articles on the specified date or later will be
	// returned.
	// minarticledate=YYYY-MM-DD%20HH%3AMM%3ASS Filteres the results so that only articles after the specified date an
	// time (HH:MM:SS) or later will be returned.
	// count=NNN Limits the number of articles returned to no more than NNN.
	// username=NAME

	@GET("/xmlapi2/thread")
	ThreadResponse thread(@Query("id") int id);

	static class ThreadResponse {
		@Attribute
		private int id;

		@Attribute(name = "numarticles")
		private int numberOfArticles;

		@Attribute
		private String link;

		@Attribute(name = "termsofuse")
		private String termsOfUse;

		@Element
		private String subject;

		@ElementList
		public List<Article> articles;
	}

	@Root(name = "article")
	static class Article {
		private long mEditDateTime = UNPARSED_DATE;

		@Attribute
		private int id;

		@Attribute
		public String username;

		@Attribute
		public String link;

		@Attribute
		private String postdate;

		@Attribute
		private String editdate;

		@Attribute
		private int numedits;

		@Element
		private String subject;

		@Element
		public String body;

		public long editDate() {
			if (mEditDateTime == UNPARSED_DATE) {
				try {
					mEditDateTime = FORMAT2.parse(editdate).getTime();
				} catch (ParseException e) {
					mEditDateTime = UNKNOWN_DATE;
				}
			}
			return mEditDateTime;
		}
	}
}
