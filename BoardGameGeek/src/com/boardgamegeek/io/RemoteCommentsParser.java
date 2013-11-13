package com.boardgamegeek.io;

import static com.boardgamegeek.util.LogUtils.LOGI;
import static com.boardgamegeek.util.LogUtils.makeLogTag;
import static org.xmlpull.v1.XmlPullParser.END_DOCUMENT;
import static org.xmlpull.v1.XmlPullParser.END_TAG;
import static org.xmlpull.v1.XmlPullParser.START_TAG;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.xmlpull.v1.XmlPullParserException;

import com.boardgamegeek.model.Comment;

public class RemoteCommentsParser extends RemoteBggParser {
	private static final String TAG = makeLogTag(RemoteCommentsParser.class);

	private List<Comment> mComments = new ArrayList<Comment>();
	private int mCommentsCount;

	public List<Comment> getResults() {
		return mComments;
	}

	@Override
	public int getCount() {
		return mCommentsCount;
	}

	@Override
	protected void clearResults() {
		mComments.clear();
	}

	@Override
	protected String getRootNodeName() {
		return Tags.ITEMS;
	}

	@Override
	protected void parseItems() throws XmlPullParserException, IOException {
		final int depth = mParser.getDepth();
		int type;
		while (((type = mParser.next()) != END_TAG || mParser.getDepth() > depth) && type != END_DOCUMENT) {
			if (type == START_TAG && Tags.COMMENTS.equals(mParser.getName())) {

				mCommentsCount = parseIntegerAttribute(Tags.TOTAL_ITEMS);
				LOGI(TAG, "Expecting " + mCommentsCount + " comments");

				parseComments();
			}
		}
	}

	private void parseComments() throws XmlPullParserException, IOException {

		String tag = null;

		final int depth = mParser.getDepth();
		int type;
		while (((type = mParser.next()) != END_TAG || mParser.getDepth() > depth) && type != END_DOCUMENT) {
			if (type == START_TAG) {
				tag = mParser.getName();
				if (Tags.COMMENT.equals(tag)) {
					Comment comment = new Comment();
					comment.Username = parseStringAttribute(Tags.USERNAME);
					comment.Rating = parseStringAttribute(Tags.RATING);
					comment.Value = parseStringAttribute(Tags.VALUE);
					mComments.add(comment);
				}
			}
		}
	}

	private interface Tags {
		// String ITEM = "item";
		// String ID = "id";
		String ITEMS = "items";
		String COMMENTS = "comments";
		// String PAGE = "page";
		String TOTAL_ITEMS = "totalitems";
		String COMMENT = "comment";
		String USERNAME = "username";
		String RATING = "rating";
		String VALUE = "value";
	}
}
