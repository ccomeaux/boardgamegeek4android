package com.boardgamegeek.io;

import static org.xmlpull.v1.XmlPullParser.END_DOCUMENT;
import static org.xmlpull.v1.XmlPullParser.END_TAG;
import static org.xmlpull.v1.XmlPullParser.START_TAG;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.content.ContentResolver;
import android.util.Log;

import com.boardgamegeek.model.ForumThread;
import com.boardgamegeek.util.StringUtils;

public class RemoteForumHandler extends RemoteBggHandler {
	private static final String TAG = "RemoteForumHandler";
	private XmlPullParser mParser;
	private List<ForumThread> mThreads = new ArrayList<ForumThread>();
	private SimpleDateFormat mFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss");
	private int mThreadsCount;

	public List<ForumThread> getResults() {
		return mThreads;
	}

	@Override
	protected void clearResults() {
		mThreads.clear();
	}

	@Override
	public int getCount() {
		return mThreadsCount;
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
			if (type == START_TAG) {
				if (Tags.THREADS.equals(mParser.getName())) {
					parseItems();
				}
				else if (Tags.FORUM.equals(mParser.getName())) {
					mThreadsCount = StringUtils.parseInt(mParser.getAttributeValue(null, Tags.NUM_THREADS));
					Log.i(TAG, "Expecting " + mThreadsCount + " threads");
				}
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
				thread.numarticles = StringUtils.parseInt(mParser.getAttributeValue(null, Tags.NUM_ARTICLES));
				try {
					thread.postdate = mFormat.parse(mParser.getAttributeValue(null, Tags.POST_DATE)).getTime();
					thread.lastpostdate = mFormat.parse(mParser.getAttributeValue(null, Tags.LAST_POST_DATE)).getTime();
				} catch (ParseException e) {
					Log.w(TAG, e.toString());
				}
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
