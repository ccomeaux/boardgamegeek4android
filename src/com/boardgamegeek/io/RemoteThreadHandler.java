package com.boardgamegeek.io;

import static org.xmlpull.v1.XmlPullParser.END_DOCUMENT;
import static org.xmlpull.v1.XmlPullParser.END_TAG;
import static org.xmlpull.v1.XmlPullParser.START_TAG;
import static org.xmlpull.v1.XmlPullParser.TEXT;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.xmlpull.v1.XmlPullParserException;

import com.boardgamegeek.model.ThreadArticle;
import com.boardgamegeek.util.DateTimeUtils;
import com.boardgamegeek.util.StringUtils;

public class RemoteThreadHandler extends RemoteBggHandler {

	private List<ThreadArticle> mArticles = new ArrayList<ThreadArticle>();

	public List<ThreadArticle> getResults() {
		return mArticles;
	}

	@Override
	protected void clearResults() {
		mArticles.clear();
	}

	@Override
	public int getCount() {
		return mArticles.size();
	}

	@Override
	protected String getRootNodeName() {
		return "thread";
	}

	@Override
	protected void parseItems() throws XmlPullParserException, IOException {

		final int depth = mParser.getDepth();
		int type;
		while (((type = mParser.next()) != END_TAG || mParser.getDepth() > depth) && type != END_DOCUMENT) {
			if (type == START_TAG && Tags.ARTICLE.equals(mParser.getName())) {
				final String userName = mParser.getAttributeValue(null, Tags.USERNAME);
				final long postDate = DateTimeUtils.parseDate(mParser.getAttributeValue(null, Tags.POST_DATE));
				final long editDate = DateTimeUtils.parseDate(mParser.getAttributeValue(null, Tags.EDIT_DATE));
				final int numEdits = StringUtils.parseInt(mParser.getAttributeValue(null, Tags.NUM_EDITS));

				final ThreadArticle article = parseItem();
				article.username = userName;
				article.postdate = postDate;
				article.editdate = editDate;
				article.numedits = numEdits;
				mArticles.add(article);
			}
		}
	}

	private ThreadArticle parseItem() throws XmlPullParserException, IOException {
		String tag = null;
		final ThreadArticle article = new ThreadArticle();

		final int depth = mParser.getDepth();
		int type;
		while (((type = mParser.next()) != END_TAG || mParser.getDepth() > depth) && type != END_DOCUMENT) {

			if (type == START_TAG) {
				tag = mParser.getName();
			}
			if (type == TEXT) {
				final String text = mParser.getText();
				if (Tags.SUBJECT.equals(tag)) {
					article.subject = text;
				} else if (Tags.BODY.equals(tag)) {
					// http://code.google.com/p/android/issues/detail?id=4401
					article.body = text.replace("%", "&#37;");
				}
			} else if (type == END_TAG) {
				tag = null;
			}
		}

		return article;
	}

	private interface Tags {
		String ARTICLE = "article";
		String USERNAME = "username";
		String POST_DATE = "postdate";
		String EDIT_DATE = "editdate";
		String NUM_EDITS = "numedits";
		String SUBJECT = "subject";
		String BODY = "body";
	}
}
