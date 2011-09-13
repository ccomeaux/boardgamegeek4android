package com.boardgamegeek.io;

import static org.xmlpull.v1.XmlPullParser.END_DOCUMENT;
import static org.xmlpull.v1.XmlPullParser.END_TAG;
import static org.xmlpull.v1.XmlPullParser.START_TAG;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.content.ContentResolver;

import com.boardgamegeek.model.Forum;
import com.boardgamegeek.util.UIUtils;

public class RemoteForumlistHandler extends RemoteBggHandler {

	private XmlPullParser mParser;
	private List<Forum> mForums = new ArrayList<Forum>();
	
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
		return "forums";
	}
	
	@Override
	public boolean parse(XmlPullParser parser, ContentResolver resolver, String authority)
			throws XmlPullParserException, IOException {

		mParser = parser;

		int type;
		while ((type = mParser.next()) != END_DOCUMENT) {
			if (type == START_TAG && Tags.FORUMS.equals(mParser.getName())) {
				parseItems();
			}
		}
		return false;
	}

	@Override
	protected void parseItems() throws XmlPullParserException, IOException {
		final int depth = mParser.getDepth();
		int type;
		while (((type = mParser.next()) != END_TAG || mParser.getDepth() > depth) && type != END_DOCUMENT) {
			if (type == START_TAG && Tags.FORUM.equals(mParser.getName())) {

				final Forum forum = new Forum();
				forum.id = mParser.getAttributeValue(null, Tags.ID);
				forum.title = mParser.getAttributeValue(null, Tags.TITLE);
				forum.numthreads = mParser.getAttributeValue(null, Tags.NUM_THREADS);
//				forum.numposts = mParser.getAttributeValue(null, Tags.NUM_POSTS);
				forum.lastpostdate = UIUtils.parseDate(mParser.getAttributeValue(null, Tags.LAST_POST_DATE));
				mForums.add(forum);
			}
		}
	}

	private interface Tags {
		String FORUMS = "forums";
		String FORUM = "forum";
		String ID = "id";
		String TITLE = "title";
		String NUM_THREADS = "numthreads";
//		String NUM_POSTS = "numposts";
		String LAST_POST_DATE = "lastpostdate";
	}
}
