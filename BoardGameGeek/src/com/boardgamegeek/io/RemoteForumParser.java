package com.boardgamegeek.io;

import static org.xmlpull.v1.XmlPullParser.END_DOCUMENT;
import static org.xmlpull.v1.XmlPullParser.END_TAG;
import static org.xmlpull.v1.XmlPullParser.START_TAG;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.xmlpull.v1.XmlPullParserException;

import com.boardgamegeek.model.ForumThread;
import com.boardgamegeek.util.HttpUtils;

public class RemoteForumParser extends RemoteBggParser {
	private static final SimpleDateFormat FORMAT = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US);
	private List<ForumThread> mThreads = new ArrayList<ForumThread>();
	private String mUrl;

	public RemoteForumParser(int forumId, int page) {
		mUrl = HttpUtils.constructForumUrl(forumId, page);
	}

	@Override
	public String getUrl() {
		return mUrl;
	}

	public List<ForumThread> getResults() {
		return mThreads;
	}

	@Override
	protected void clearResults() {
		mThreads.clear();
	}

	@Override
	protected String getRootNodeName() {
		return Tags.FORUM;
	}

	@Override
	protected String getTotalCountAttributeName() {
		return Tags.NUM_THREADS;
	}

	@Override
	protected void parseItems() throws XmlPullParserException, IOException {
		final int depth = mParser.getDepth();
		int type;
		while (((type = mParser.next()) != END_TAG || mParser.getDepth() > depth) && type != END_DOCUMENT) {
			if (type == START_TAG && Tags.THREADS.equals(mParser.getName())) {
				parseThreads();
			}
		}
	}

	private void parseThreads() throws XmlPullParserException, IOException {
		final int depth = mParser.getDepth();
		int type;
		while (((type = mParser.next()) != END_TAG || mParser.getDepth() > depth) && type != END_DOCUMENT) {
			if (type == START_TAG && Tags.THREAD.equals(mParser.getName())) {

				ForumThread thread = new ForumThread();
				thread.id = parseStringAttribute(Tags.ID);
				thread.subject = parseStringAttribute(Tags.SUBJECT);
				thread.author = parseStringAttribute(Tags.AUTHOR);
				thread.numberOfArticles = parseIntegerAttribute(Tags.NUM_ARTICLES);
				thread.postDate = parseDateAttribute(Tags.POST_DATE, FORMAT, false);
				thread.lastPostDate = parseDateAttribute(Tags.LAST_POST_DATE, FORMAT, false);
				mThreads.add(thread);
			}
		}
	}

	private interface Tags {
		String FORUM = "forum";
		String NUM_THREADS = "numthreads";
		String NUM_ARTICLES = "numarticles";
		String THREADS = "threads";
		String THREAD = "thread";
		String ID = "id";
		String SUBJECT = "subject";
		String AUTHOR = "author";
		String LAST_POST_DATE = "lastpostdate";
		String POST_DATE = "postdate";
	}
}
