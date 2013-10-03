package com.boardgamegeek.io;

import java.io.IOException;
import java.util.List;

import org.xmlpull.v1.XmlPullParserException;

import com.boardgamegeek.model.Play;
import com.boardgamegeek.model.persister.PlayPersister;

public class RemotePlaysHandler extends RemoteBggHandler {
	private RemotePlaysParser parser;

	public RemotePlaysHandler() {
		super();
		parser = new RemotePlaysParser();
	}

	public List<Play> getPlays() {
		return parser.getPlays();
	}

	public long getNewestDate() {
		return parser.getNewestDate();
	}

	public long getOldestDate() {
		return parser.getOldestDate();
	}

	public void setDatesMaybe(String date) {
		parser.setDatesMaybe(date);
	}
	@Override
	public int getCount() {
		return parser.getCount();
	}

	@Override
	protected void clearResults() {
		parser.clearResults();
	}

	@Override
	protected String getRootNodeName() {
		return parser.getRootNodeName();
	}
	
	@Override
	protected String getTotalCountAttributeName() {
		return parser.getTotalCountAttributeName();
	}

	@Override
	protected void parseItems() throws XmlPullParserException, IOException {
		parser.setParser(mParser);
		parser.parseItems();
		PlayPersister.save(mResolver, parser.getPlays());
	}
}