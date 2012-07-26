package com.boardgamegeek.io;

import static com.boardgamegeek.util.LogUtils.LOGE;
import static com.boardgamegeek.util.LogUtils.makeLogTag;
import static org.xmlpull.v1.XmlPullParser.END_DOCUMENT;
import static org.xmlpull.v1.XmlPullParser.END_TAG;
import static org.xmlpull.v1.XmlPullParser.START_TAG;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import org.xmlpull.v1.XmlPullParserException;

import com.boardgamegeek.model.ForumThread;
import com.boardgamegeek.util.StringUtils;

public class RemoteForumHandler extends RemoteBggHandler {
	private static final String TAG = makeLogTag(RemoteForumHandler.class);

	private List<ForumThread> mThreads = new ArrayList<ForumThread>();
	private SimpleDateFormat mFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss");

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
				thread.id = mParser.getAttributeValue(null, Tags.ID);
				thread.subject = mParser.getAttributeValue(null, Tags.SUBJECT);
				thread.author = mParser.getAttributeValue(null, Tags.AUTHOR);
				thread.numarticles = StringUtils.parseInt(mParser.getAttributeValue(null, Tags.NUM_ARTICLES));
				try {
					thread.postdate = mFormat.parse(mParser.getAttributeValue(null, Tags.POST_DATE)).getTime();
					thread.lastpostdate = mFormat.parse(mParser.getAttributeValue(null, Tags.LAST_POST_DATE)).getTime();
				} catch (ParseException e) {
					LOGE(TAG, "Parsing forum thread", e);
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
