package com.boardgamegeek.io;

import java.util.List;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;

import retrofit.http.GET;
import retrofit.http.Query;

import com.boardgamegeek.model.Article;
import com.boardgamegeek.model.Forum;
import com.boardgamegeek.model.Thread;

public interface ForumService {
	public static String TYPE_REGION = "region";
	public static String TYPE_THING = "thing";

	public static final int REGION_BOARDGAME = 1;
	public static final int REGION_RPG = 2;
	public static final int REGION_VIDEOGAME = 3;

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

	// minarticleid=NNN Filters the results so that only articles with an equal or higher id than NNN will be returned.
	// minarticledate=YYYY-MM-DD Filters the results so that only articles on the specified date or later will be
	// returned.
	// minarticledate=YYYY-MM-DD%20HH%3AMM%3ASS Filters the results so that only articles after the specified date an
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

}
