package com.boardgamegeek;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import android.app.Dialog;
import android.app.ExpandableListActivity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.util.Linkify;
import android.util.Log;
import android.view.View;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ScrollView;
import android.widget.SimpleExpandableListAdapter;
import android.widget.TextView;
import android.widget.Toast;

public class BoardGameExtraTab extends ExpandableListActivity {

	private List<Map<String, String>> groupData;
	private List<List<Map<String, String>>> childData;
	private static final String NAME = "NAME";
	private static final String COUNT = "COUNT";
	private final int ID_DIALOG_PROGRESS = 1;
	private final int ID_DIALOG_RESULTS = 2;
	private ExpandableListAdapter adapter;
	private Handler handler = new Handler();
	private String name;
	private String description;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		BoardGame boardGame = ViewBoardGame.boardGame;
		if (boardGame == null) {
			return;
		}

		groupData = new ArrayList<Map<String, String>>();
		childData = new ArrayList<List<Map<String, String>>>();

		createGroup(R.string.designers, boardGame.getDesignerNames());
		createGroup(R.string.artists, boardGame.getArtistNames());
		createGroup(R.string.publishers, boardGame.getPublisherNames());
		createGroup(R.string.categories, boardGame.getCategoryNames());
		createGroup(R.string.mechanics, boardGame.getMechanicNames());
		createGroup(R.string.expansions, boardGame.getExpansionNames());

		adapter = new SimpleExpandableListAdapter(this, groupData,
				R.layout.grouprow, new String[] { NAME, COUNT }, new int[] {
						R.id.name, R.id.count }, childData, R.layout.childrow,
				new String[] { NAME, COUNT },
				new int[] { R.id.name, R.id.count });
		setListAdapter(adapter);
	}

	private void createGroup(int nameId, Collection<String> children) {
		Map<String, String> groupMap = new HashMap<String, String>();
		groupData.add(groupMap);
		groupMap.put(NAME, getResources().getString(nameId));
		groupMap.put(COUNT, "" + children.size());

		List<Map<String, String>> childrenMap = new ArrayList<Map<String, String>>();
		for (String designer : children) {
			Map<String, String> childMap = new HashMap<String, String>();
			childrenMap.add(childMap);
			childMap.put(NAME, designer);
		}
		childData.add(childrenMap);
	}

	@Override
	public boolean onChildClick(ExpandableListView parent, View v,
			int groupPosition, int childPosition, long id) {

		BoardGame boardGame = ViewBoardGame.boardGame;
		if (boardGame != null) {
			try {
				switch (groupPosition) {
				case 0: // designer
					searchDetail("boardgamedesigner", boardGame
							.getDesignerId(childPosition));
					break;
				case 1: // artist
					searchDetail("boardgameartist", boardGame
							.getArtistId(childPosition));
					break;
				case 2: // publisher
					searchDetail("boardgamepublisher", boardGame
							.getPublisherId(childPosition));
					break;
				case 3: // category
				case 4: // mechanic
					Toast.makeText(this, "No extra information",
							Toast.LENGTH_SHORT).show();
					break;
				case 5:
					Intent intent = new Intent(this, ViewBoardGame.class);
					intent.putExtra("GAME_ID", boardGame
							.getExpansionId(childPosition));
					startActivity(intent);
					break;
				default:
					Log.w("BGG", "Unexpected: Group: " + groupPosition
							+ ", Child: " + childPosition);
					break;
				}
			} catch (MalformedURLException e) {
				Log.d("BGG", "Couldn't find detail", e);
				removeDialog(ID_DIALOG_PROGRESS);
			}
		}
		return super.onChildClick(parent, v, groupPosition, childPosition, id);
	}

	private void searchDetail(String path, String id)
			throws MalformedURLException {
		removeDialog(ID_DIALOG_RESULTS);
		showDialog(ID_DIALOG_PROGRESS);
		name = null;
		description = null;
		final URL url = new URL("http://www.boardgamegeek.com/xmlapi/" + path
				+ "/" + id);
		new Thread() {
			public void run() {
				try {
					SAXParser saxParser = SAXParserFactory.newInstance()
							.newSAXParser();
					XMLReader xmlReader = saxParser.getXMLReader();
					NameDescriptionHandler handler = new NameDescriptionHandler();
					xmlReader.setContentHandler(handler);
					xmlReader.parse(new InputSource(url.openStream()));
				} catch (Exception e) {
					Log.d("BGG", "PULLING XML - Failed", e);
				}
				handler.post(updateResults);
			}
		}.start();
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		if (id == ID_DIALOG_PROGRESS) {
			ProgressDialog dialog = new ProgressDialog(this);
			dialog.setTitle(R.string.dialog_search_message);
			dialog.setMessage(getResources().getString(
					R.string.dialog_search_message));
			dialog.setIndeterminate(true);
			dialog.setCancelable(true);
			return dialog;
		} else if (id == ID_DIALOG_RESULTS) {
			Dialog dialog = new Dialog(this);
			dialog.setTitle(name);
			TextView tv = new TextView(this);
			tv.setAutoLinkMask(Linkify.ALL);
			tv.setText(Utility.unescapeText(description));
			ScrollView sv = new ScrollView(this);
			sv.setPadding(5, 0, 5, 5);
			sv.addView(tv);
			dialog.setContentView(sv);
			dialog.setCancelable(true);
			return dialog;
		}
		return super.onCreateDialog(id);
	}

	final Runnable updateResults = new Runnable() {
		public void run() {
			removeDialog(ID_DIALOG_PROGRESS);
			showDialog(ID_DIALOG_RESULTS);
		}
	};

	private class NameDescriptionHandler extends DefaultHandler {
		private StringBuilder currentElement;

		@Override
		public void startElement(String uri, String localName, String qName,
				Attributes attributes) throws SAXException {
			super.startElement(uri, localName, qName, attributes);
			currentElement = new StringBuilder();
		}

		@Override
		public void endElement(String namespaceURI, String localName,
				String qName) throws SAXException {
			if (localName == "name") {
				name = currentElement.toString();
			} else if (localName == "description") {
				description = currentElement.toString();
			}
		}

		@Override
		public void characters(char ch[], int start, int length) {
			currentElement.append(ch, start, length);
		}
	}
}
