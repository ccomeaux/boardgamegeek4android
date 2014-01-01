package com.boardgamegeek.io;

import static org.xmlpull.v1.XmlPullParser.END_DOCUMENT;
import static org.xmlpull.v1.XmlPullParser.END_TAG;
import static org.xmlpull.v1.XmlPullParser.START_TAG;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.xmlpull.v1.XmlPullParserException;

import com.boardgamegeek.model.Forum;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.util.HttpUtils;

public class RemoteForumsParser extends RemoteBggParser {
	private static final String GENERAL_FORUMS_URL = HttpUtils.BASE_URL_2 + "forumlist?id=1&type=region";
	private static final String DATE_FORMAT = "EEE, dd MMM yyyy HH:mm:ss Z";

	private List<Forum> mForums = new ArrayList<Forum>();
	private String mUrl;

	public RemoteForumsParser(int gameId) {
		mUrl = (gameId != BggContract.INVALID_ID) ? HttpUtils.constructForumlistUrl(gameId) : GENERAL_FORUMS_URL;
	}

	public String getUrl() {
		return mUrl;
	}

	public List<Forum> getResults() {
		return mForums;
	}

	@Override
	protected void clearResults() {
		mForums.clear();
	}

	@Override
	public int getCount() {
		return mForums.size();
	}

	@Override
	protected String getRootNodeName() {
		return Tags.FORUMS;
	}

	@Override
	protected void parseItems() throws XmlPullParserException, IOException {
		final int depth = mParser.getDepth();
		int type;
		while (((type = mParser.next()) != END_TAG || mParser.getDepth() > depth) && type != END_DOCUMENT) {
			if (type == START_TAG && Tags.FORUM.equals(mParser.getName())) {

				if (mParser.getAttributeValue(null, Tags.NO_POSTING).equals("1")) {
					// ignore forums that are not writable
					continue;
				}

				final Forum forum = new Forum();
				forum.id = parseStringAttribute(Tags.ID);
				forum.title = parseStringAttribute(Tags.TITLE);
				forum.numberOfThreads = parseIntegerAttribute(Tags.NUM_THREADS);
				forum.lastPostDate = parseDateAttribute(Tags.LAST_POST_DATE, DATE_FORMAT, false);
				mForums.add(forum);
			}
		}
	}

	private interface Tags {
		String FORUMS = "forums";
		String FORUM = "forum";
		String ID = "id";
		String TITLE = "title";
		String NO_POSTING = "noposting";
		String NUM_THREADS = "numthreads";
		String LAST_POST_DATE = "lastpostdate";
	}
}
