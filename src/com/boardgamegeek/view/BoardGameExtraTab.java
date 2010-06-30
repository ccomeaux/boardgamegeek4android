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

import com.boardgamegeek.BoardGameGeekData.Artists;
import com.boardgamegeek.BoardGameGeekData.Designers;
import com.boardgamegeek.BoardGameGeekData.Publishers;
import com.boardgamegeek.model.BoardGame;

import android.app.Dialog;
import android.app.ExpandableListActivity;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
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

	private static final String LOG_TAG = "BoardGameGeek";
	private static final String NAME = "NAME";
	private static final String COUNT = "COUNT";
	private final int ID_DIALOG_PROGRESS = 1;
	private final int ID_DIALOG_RESULTS = 2;

	private List<Map<String, String>> groupData;
	private List<List<Map<String, String>>> childData;
	private ExpandableListAdapter adapter;
	private long cacheDuration;
	private Handler handler = new Handler();
	private int selectedId;
	private String name;
	private String description;
	private String title;

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

		adapter = new SimpleExpandableListAdapter(this, groupData, R.layout.grouprow, new String[] { NAME,
			COUNT }, new int[] { R.id.name, R.id.count }, childData, R.layout.childrow,
			new String[] { NAME }, new int[] { R.id.name });
		setListAdapter(adapter);
	}

	@Override
	public void onResume() {
		super.onResume();
		getPreferences();
	}

	private void getPreferences() {
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
		cacheDuration = Utility.parseInt(preferences.getString("cacheDurationExtra", "1209600000"),
			1209600000);
	}

	private void createGroup(int nameId, Collection<String> children) {
		Map<String, String> groupMap = new HashMap<String, String>();
		groupData.add(groupMap);
		groupMap.put(NAME, getResources().getString(nameId));
		groupMap.put(COUNT, "" + children.size());

		List<Map<String, String>> childrenMap = new ArrayList<Map<String, String>>();
		for (String child : children) {
			Map<String, String> childMap = new HashMap<String, String>();
			childrenMap.add(childMap);
			childMap.put(NAME, child);
		}
		childData.add(childrenMap);
	}

	@Override
	public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition,
		long id) {

		switch (groupPosition) {
		case 0: // designer
			ClickDesigner(childPosition);
			break;
		case 1: // artist
			ClickArtist(childPosition);
			break;
		case 2: // publisher
			ClickPublisher(childPosition);
			break;
		case 3: // category
		case 4: // mechanic
			Toast.makeText(this, "No extra information", Toast.LENGTH_SHORT).show();
			break;
		case 5: // expansion
			BoardGame boardGame = ViewBoardGame.boardGame;
			if (boardGame != null) {
				Intent intent = new Intent(this, ViewBoardGame.class);
				intent.putExtra("GAME_ID", boardGame.getExpansionByPosition(childPosition).Id);
				startActivity(intent);
			} else {
				Log.w(LOG_TAG, "BoardGame was unexpectedly null!");
			}
			break;
		default:
			Log.w(LOG_TAG, "Unexpected: Group: " + groupPosition + ", Child: " + childPosition);
			break;
		}
		return super.onChildClick(parent, v, groupPosition, childPosition, id);
	}

	private void ClickDesigner(int position) {

		removeDialog(ID_DIALOG_RESULTS);
		showDialog(ID_DIALOG_PROGRESS);

		title = getResources().getString(R.string.designer);
		name = null;
		description = null;
		BoardGame boardGame = ViewBoardGame.boardGame;

		if (boardGame != null) {
			selectedId = boardGame.getDesignerByPosition(position).Id;

			// check if the designer is in the database
			Uri designerUri = Uri.withAppendedPath(Designers.CONTENT_URI, "" + selectedId);
			Cursor cursor = managedQuery(designerUri, null, null, null, null);

			if (cursor.moveToFirst()) {
				// found in the DB
				Long date = cursor.getLong(cursor.getColumnIndex(Designers.UPDATED_DATE));
				Long now = System.currentTimeMillis();
				if (date + cacheDuration > now) {
					// data is less than 14 days old, so use
					name = cursor.getString(cursor.getColumnIndex(Designers.NAME));
					description = cursor.getString(cursor.getColumnIndex(Designers.DESCRIPTION));
					removeDialog(ID_DIALOG_PROGRESS);
					showDialog(ID_DIALOG_RESULTS);
					return;
				}
			}
			// not in DB or too old, so get search BGG
			try {
				final URL url = new URL("http://www.boardgamegeek.com/xmlapi/boardgamedesigner/" + selectedId);
				new Thread() {
					public void run() {
						try {
							SAXParser saxParser = SAXParserFactory.newInstance().newSAXParser();
							XMLReader xmlReader = saxParser.getXMLReader();
							DesignerHandler handler = new DesignerHandler();
							xmlReader.setContentHandler(handler);
							xmlReader.parse(new InputSource(url.openStream()));
						} catch (Exception e) {
							Log.d("BGG", "PULLING XML - Failed", e);
						}
						handler.post(updateResults);
					}
				}.start();
			} catch (MalformedURLException e) {
				Log.d("BGG", "Couldn't find designer " + selectedId, e);
				removeDialog(ID_DIALOG_PROGRESS);
			}
		}
	}

	private void ClickArtist(int position) {

		removeDialog(ID_DIALOG_RESULTS);
		showDialog(ID_DIALOG_PROGRESS);

		title = getResources().getString(R.string.artist);
		name = null;
		description = null;
		BoardGame boardGame = ViewBoardGame.boardGame;

		if (boardGame != null) {
			selectedId = boardGame.getArtistByPosition(position).Id;

			// check if the artist is in the database
			Uri artistUri = Uri.withAppendedPath(Artists.CONTENT_URI, "" + selectedId);
			Cursor cursor = managedQuery(artistUri, null, null, null, null);

			if (cursor.moveToFirst()) {
				// found in the DB
				Long date = cursor.getLong(cursor.getColumnIndex(Artists.UPDATED_DATE));
				Long now = System.currentTimeMillis();
				if (date + cacheDuration > now) {
					// data is less than 14 days old, so use
					name = cursor.getString(cursor.getColumnIndex(Artists.NAME));
					description = cursor.getString(cursor.getColumnIndex(Artists.DESCRIPTION));
					removeDialog(ID_DIALOG_PROGRESS);
					showDialog(ID_DIALOG_RESULTS);
					return;
				}
			}
			// not in DB or too old, so get search BGG
			try {
				final URL url = new URL("http://www.boardgamegeek.com/xmlapi/boardgameartist/" + selectedId);
				new Thread() {
					public void run() {
						try {
							SAXParser saxParser = SAXParserFactory.newInstance().newSAXParser();
							XMLReader xmlReader = saxParser.getXMLReader();
							ArtistHandler handler = new ArtistHandler();
							xmlReader.setContentHandler(handler);
							xmlReader.parse(new InputSource(url.openStream()));
						} catch (Exception e) {
							Log.d("BGG", "PULLING XML - Failed", e);
						}
						handler.post(updateResults);
					}
				}.start();
			} catch (MalformedURLException e) {
				Log.d("BGG", "Couldn't find artist " + selectedId, e);
				removeDialog(ID_DIALOG_PROGRESS);
			}
		}
	}

	private void ClickPublisher(int position) {

		removeDialog(ID_DIALOG_RESULTS);
		showDialog(ID_DIALOG_PROGRESS);

		title = getResources().getString(R.string.publisher);
		name = null;
		description = null;
		BoardGame boardGame = ViewBoardGame.boardGame;

		if (boardGame != null) {
			selectedId = boardGame.getPublisherByPosition(position).Id;

			// check if the artist is in the database
			Uri publisherUri = Uri.withAppendedPath(Publishers.CONTENT_URI, "" + selectedId);
			Cursor cursor = managedQuery(publisherUri, null, null, null, null);

			if (cursor.moveToFirst()) {
				// found in the DB
				Long date = cursor.getLong(cursor.getColumnIndex(Artists.UPDATED_DATE));
				Long now = System.currentTimeMillis();
				if (date + cacheDuration > now) {
					// data is less than 14 days old, so use
					name = cursor.getString(cursor.getColumnIndex(Publishers.NAME));
					description = cursor.getString(cursor.getColumnIndex(Publishers.DESCRIPTION));
					removeDialog(ID_DIALOG_PROGRESS);
					showDialog(ID_DIALOG_RESULTS);
					return;
				}
			}
			// not in DB or too old, so get search BGG
			try {
				final URL url = new URL("http://www.boardgamegeek.com/xmlapi/boardgamepublisher/"
					+ selectedId);
				new Thread() {
					public void run() {
						try {
							SAXParser saxParser = SAXParserFactory.newInstance().newSAXParser();
							XMLReader xmlReader = saxParser.getXMLReader();
							PublisherHandler handler = new PublisherHandler();
							xmlReader.setContentHandler(handler);
							xmlReader.parse(new InputSource(url.openStream()));
						} catch (Exception e) {
							Log.d("BGG", "PULLING XML - Failed", e);
						}
						handler.post(updateResults);
					}
				}.start();
			} catch (MalformedURLException e) {
				Log.d("BGG", "Couldn't find publisher " + selectedId, e);
				removeDialog(ID_DIALOG_PROGRESS);
			}
		}
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		if (id == ID_DIALOG_PROGRESS) {
			ProgressDialog dialog = new ProgressDialog(this);
			dialog.setTitle(title);
			dialog.setMessage(getResources().getString(R.string.downloading_message));
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

	// Parses the designer from the XML
	private class DesignerHandler extends DefaultHandler {

		private StringBuilder currentElement;

		// after parsing, stuff the results into the database
		@Override
		public void endDocument() throws SAXException {
			super.endDocument();

			ContentValues values = new ContentValues();
			values.put(Designers.NAME, name);
			values.put(Designers.DESCRIPTION, description);
			values.put(Designers.UPDATED_DATE, Long.valueOf(System.currentTimeMillis()));

			Uri uri = Uri.withAppendedPath(Designers.CONTENT_URI, "" + selectedId);
			Cursor cursor = managedQuery(uri, null, null, null, null);
			if (cursor.moveToFirst()) {
				// update
				getContentResolver().update(uri, values, null, null);
			} else {
				// insert
				values.put(Designers._ID, selectedId);
				getContentResolver().insert(Designers.CONTENT_URI, values);
			}
		}

		@Override
		public void startElement(String uri, String localName, String qName, Attributes attributes)
			throws SAXException {
			super.startElement(uri, localName, qName, attributes);
			currentElement = new StringBuilder();
		}

		@Override
		public void endElement(String namespaceURI, String localName, String qName) throws SAXException {
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

	// Parses the artist from the XML
	private class ArtistHandler extends DefaultHandler {

		private StringBuilder currentElement;

		// after parsing, stuff the results into the database
		@Override
		public void endDocument() throws SAXException {
			super.endDocument();

			ContentValues values = new ContentValues();
			values.put(Artists.NAME, name);
			values.put(Artists.DESCRIPTION, description);
			values.put(Artists.UPDATED_DATE, Long.valueOf(System.currentTimeMillis()));

			Uri uri = Uri.withAppendedPath(Artists.CONTENT_URI, "" + selectedId);
			Cursor cursor = managedQuery(uri, null, null, null, null);
			if (cursor.moveToFirst()) {
				// update
				getContentResolver().update(uri, values, null, null);
			} else {
				// insert
				values.put(Artists._ID, selectedId);
				getContentResolver().insert(Artists.CONTENT_URI, values);
			}
		}

		@Override
		public void startElement(String uri, String localName, String qName, Attributes attributes)
			throws SAXException {
			super.startElement(uri, localName, qName, attributes);
			currentElement = new StringBuilder();
		}

		@Override
		public void endElement(String namespaceURI, String localName, String qName) throws SAXException {
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

	// Parses the publisher from the XML
	private class PublisherHandler extends DefaultHandler {

		private StringBuilder currentElement;

		// after parsing, stuff the results into the database
		@Override
		public void endDocument() throws SAXException {
			super.endDocument();

			ContentValues values = new ContentValues();
			values.put(Publishers.NAME, name);
			values.put(Publishers.DESCRIPTION, description);
			values.put(Publishers.UPDATED_DATE, Long.valueOf(System.currentTimeMillis()));

			Uri uri = Uri.withAppendedPath(Publishers.CONTENT_URI, "" + selectedId);
			Cursor cursor = managedQuery(uri, null, null, null, null);
			if (cursor.moveToFirst()) {
				// update
				getContentResolver().update(uri, values, null, null);
			} else {
				// insert
				values.put(Publishers._ID, selectedId);
				getContentResolver().insert(Publishers.CONTENT_URI, values);
			}
		}

		@Override
		public void startElement(String uri, String localName, String qName, Attributes attributes)
			throws SAXException {
			super.startElement(uri, localName, qName, attributes);
			currentElement = new StringBuilder();
		}

		@Override
		public void endElement(String namespaceURI, String localName, String qName) throws SAXException {
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
