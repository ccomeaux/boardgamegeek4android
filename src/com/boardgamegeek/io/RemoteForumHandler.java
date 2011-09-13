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

import com.boardgamegeek.model.ForumThread;
import com.boardgamegeek.util.UIUtils;

public class RemoteForumHandler extends RemoteBggHandler {

	private XmlPullParser mParser;
	private List<ForumThread> mThreads = new ArrayList<ForumThread>();
	
	public List<ForumThread> getResults() {
		return mThreads;
	}
	
	@Override
	protected void clearResults() {
		mThreads.clear();
	}

	@Override
	public int getCount() {
		return mThreads.size();
	}

	@Override
	protected String getRootNodeName() {
		return "forum";
	}
	
	@Override
	public boolean parse(XmlPullParser parser, ContentResolver resolver, String authority)
			throws XmlPullParserException, IOException {

		mParser = parser;

		int type;
		while ((type = mParser.next()) != END_DOCUMENT) {
			if (type == START_TAG && Tags.THREADS.equals(mParser.getName())) {
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
			if (type == START_TAG && Tags.THREAD.equals(mParser.getName())) {

				ForumThread thread = new ForumThread();
				thread.id = mParser.getAttributeValue(null, Tags.ID);
				thread.subject = mParser.getAttributeValue(null, Tags.SUBJECT);
				thread.author = mParser.getAttributeValue(null, Tags.AUTHOR);
				thread.numarticles = mParser.getAttributeValue(null, Tags.NUM_ARTICLES);
				thread.lastpostdate = UIUtils.parseDate(mParser.getAttributeValue(null, Tags.LAST_POST_DATE));
				thread.postdate = UIUtils.parseDate(mParser.getAttributeValue(null, Tags.POST_DATE));
				mThreads.add(thread);
			}
		}
	}

	private interface Tags {
//		String FORUM = "forum";
//		String TITLE = "title";
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
