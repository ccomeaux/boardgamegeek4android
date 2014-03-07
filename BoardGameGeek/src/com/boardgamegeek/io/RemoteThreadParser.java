package com.boardgamegeek.io;

import static org.xmlpull.v1.XmlPullParser.END_DOCUMENT;
import static org.xmlpull.v1.XmlPullParser.END_TAG;
import static org.xmlpull.v1.XmlPullParser.START_TAG;
import static org.xmlpull.v1.XmlPullParser.TEXT;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.xmlpull.v1.XmlPullParserException;

import com.boardgamegeek.model.ThreadArticle;
import com.boardgamegeek.util.HttpUtils;

public class RemoteThreadParser extends RemoteBggParser {
	private static final SimpleDateFormat FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssz", Locale.US);
	private List<ThreadArticle> mArticles = new ArrayList<ThreadArticle>();
	private String mUrl;

	public RemoteThreadParser(String threadId) {
		mUrl = HttpUtils.constructThreadUrl(threadId);
	}

	@Override
	public String getUrl() {
		return mUrl;
	}

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
		return Tags.THREAD;
	}

	@Override
	protected void parseItems() throws XmlPullParserException, IOException {

		final int depth = mParser.getDepth();
		int type;
		while (((type = mParser.next()) != END_TAG || mParser.getDepth() > depth) && type != END_DOCUMENT) {
			if (type == START_TAG && Tags.ARTICLE.equals(mParser.getName())) {
				int id = parseIntegerAttribute(Tags.ID);
				String userName = parseStringAttribute(Tags.USERNAME);
				String link = parseStringAttribute(Tags.LINK);
				long postDate = parseDateAttribute(Tags.POST_DATE, FORMAT, true);
				long editDate = parseDateAttribute(Tags.EDIT_DATE, FORMAT, true);
				int numEdits = parseIntegerAttribute(Tags.NUM_EDITS);

				ThreadArticle article = parseItem();
				article.id = id;
				article.username = userName;
				article.link = link;
				article.postDate = postDate;
				article.editDate = editDate;
				article.numberOfEdits = numEdits;
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
					article.subject = text.trim();
				} else if (Tags.BODY.equals(tag)) {
					article.body = text;
				}
			} else if (type == END_TAG) {
				tag = null;
			}
		}

		return article;
	}

	private interface Tags {
		String THREAD = "thread";
		String ARTICLE = "article";
		String ID = "id";
		String USERNAME = "username";
		String LINK = "link";
		String POST_DATE = "postdate";
		String EDIT_DATE = "editdate";
		String NUM_EDITS = "numedits";
		String SUBJECT = "subject";
		String BODY = "body";
	}
}
