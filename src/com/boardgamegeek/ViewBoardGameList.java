package com.boardgamegeek;

import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import com.boardgamegeek.BoardGameGeekData.BoardGames;

import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class ViewBoardGameList extends ListActivity {
	// declare variables
	private String searchText;
	private List<BoardGame> boardGames = new ArrayList<BoardGame>();
	private BoardGameAdapter adapter;
	private final int ID_DIALOG_SEARCHING = 1;
	private final int ID_DIALOG_RETRY = 2;
	private final int ID_DIALOG_LOADING = 3;
	private final String LOG_TAG = "BoardGameGeek";
	private Handler handler = new Handler();
	private SharedPreferences preferences;
	private boolean exactSearch;
	private boolean skipResults;
	private boolean isFirstPass = true;
	private boolean isGeekDown = false;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(LOG_TAG, "onCreate");

		setDefaultKeyMode(DEFAULT_KEYS_SEARCH_LOCAL); // allow type-to-search
		setContentView(R.layout.viewboardgamelist);
		parseIntent(getIntent());
	}

	@Override
	public void onResume() {
		super.onResume();
		Log.d(LOG_TAG, "onResume");
		getPreferences();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		Log.d(LOG_TAG, "onSaveInstanceState");
		removeDialogs();
		super.onSaveInstanceState(outState);
	}

	@Override
	public void onNewIntent(Intent intent) {
		parseIntent(intent);
	}

	private void viewDatabase() {
		showDialog(ID_DIALOG_LOADING);
		boardGames = new ArrayList<BoardGame>();
		Cursor c = managedQuery(BoardGames.CONTENT_URI, null, null, null, null);
		if (c.moveToFirst()) {
			do {
				BoardGame bg = new BoardGame();
				bg.setGameId(c.getInt(c.getColumnIndex(BoardGames._ID)));
				bg.setYearPublished(c.getInt(c.getColumnIndex(BoardGames.YEAR)));
				bg.setName(c.getString(c.getColumnIndex(BoardGames.NAME)));
				boardGames.add(bg);
			} while (c.moveToNext());
		}
		adapter = new BoardGameAdapter();
		setListAdapter(adapter);
		setTitle(R.string.view_cache_title);
		removeDialog(ID_DIALOG_LOADING);
	}

	private void parseIntent(Intent intent) {

		if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
			searchText = intent.getExtras().getString(SearchManager.QUERY);
			if (TextUtils.isEmpty(searchText)) {
				viewDatabase();
			} else {
				getBoardGameList();
			}
		} else if (Intent.ACTION_VIEW.equals(intent.getAction())) {
			String data = intent.getDataString();
			if (TextUtils.isEmpty(data)) {
				viewDatabase();
			} else {
				Uri uri = Uri.parse(data);
				viewBoardGame(Utility.parseInt(uri.getLastPathSegment(), 0));
				finish();
			}
		} else {
			Log.w(LOG_TAG, "Received bad intent action: " + intent.getAction());
			viewDatabase();
		}
	}

	private void getBoardGameList() {
		Log.d(LOG_TAG, "getBoardGameList");

		// clear existing game list items
		boardGames.clear();

		// display a progress dialog while fetching the game data
		if (isFirstPass) {
			showDialog(ID_DIALOG_SEARCHING);
		} else {
			showDialog(ID_DIALOG_RETRY);
		}

		new Thread() {
			public void run() {
				try {
					isGeekDown = false;
					// set URL
					String queryUrl = "http://www.geekdo.com/xmlapi/search?search=" + searchText;
					if (exactSearch && isFirstPass) {
						queryUrl += "&exact=1";
					}

					URL url = new URL(queryUrl.replace(" ", "+"));
					Log.d(LOG_TAG, "Query: " + url.toString());

					// create a new SAX parser and get an XML reader from it
					SAXParser saxParser = SAXParserFactory.newInstance().newSAXParser();
					XMLReader xmlReader = saxParser.getXMLReader();

					// set the XML reader's content handler and parse the XML
					BoardGameListHandler boardGameListHandler = new BoardGameListHandler();
					xmlReader.setContentHandler(boardGameListHandler);
					xmlReader.parse(new InputSource(url.openStream()));

					// get the parsed data as an object
					boardGames = boardGameListHandler.getBoardGameList();
				} catch (UnknownHostException e) {
					// I got this when the DNS wouldn't resolve
					Log.w(LOG_TAG, "PULLING XML - Failed", e);
				} catch (Exception e) {
					Log.w(LOG_TAG, "PULLING XML - Failed", e);
					if (e.getMessage() == "down") {
						isGeekDown = true;
					}
				}
				handler.post(updateResults);
			}
		}.start();
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		if (id == ID_DIALOG_SEARCHING) {
			return createDialog(R.string.dialog_search_title, R.string.dialog_search_message);
		} else if (id == ID_DIALOG_RETRY) {
			return createDialog(R.string.dialog_retry_title, R.string.dialog_retry_message);
		} else if (id == ID_DIALOG_LOADING) {
			return createDialog(R.string.dialog_loading_title, R.string.dialog_loading_message);
		}
		return super.onCreateDialog(id);
	}

	private Dialog createDialog(int titleId, int messageId) {
		ProgressDialog dialog = new ProgressDialog(this);
		dialog.setTitle(titleId);
		dialog.setMessage(getResources().getString(messageId));
		dialog.setIndeterminate(true);
		dialog.setCancelable(true);
		return dialog;
	}

	// remove dialog boxes
	protected void removeDialogs() {
		try {
			removeDialog(ID_DIALOG_SEARCHING);
		} catch (Exception e) {
			Log.w(LOG_TAG, "ID_DIALOG_SEARCHING - Remove Failed", e);
		}
		try {
			removeDialog(ID_DIALOG_RETRY);
		} catch (Exception e) {
			Log.w(LOG_TAG, "ID_DIALOG_RETRY - Remove Failed", e);
		}
		try {
			removeDialog(ID_DIALOG_LOADING);
		} catch (Exception e) {
			Log.w(LOG_TAG, "ID_DIALOG_LOADING - Remove Failed", e);
		}
	}

	// get results from handler
	final Runnable updateResults = new Runnable() {
		public void run() {
			updateUI();
		}
	};

	private void updateMessage(String message) {
		TextView nr = (TextView) findViewById(android.R.id.empty);
		nr.setText(message);
	}

	// updates UI after running progress dialog
	private void updateUI() {
		int count = 0;
		if (boardGames != null) {
			count = boardGames.size();
		} else {
			boardGames = new ArrayList<BoardGame>();
		}
		removeDialogs();
		if (isGeekDown) {
			updateMessage(getResources().getString(R.string.bgg_down));
		} else if (count == 0 && exactSearch && isFirstPass) {
			// try again if exactSearch is on and no results were found
			isFirstPass = false;
			getBoardGameList();
		} else if (count == 0 && (!exactSearch || !isFirstPass)) {
			// display if no results are found
			updateMessage(String.format(getResources().getString(R.string.no_results), searchText));
		}

		// skip directly to game if only one result
		if (count == 1 && skipResults) {
			BoardGame boardGame = boardGames.get(0);
			viewBoardGame(boardGame.getGameId());
			finish();
			return;
		}

		// display game list (even if we skip results, since user may use back
		// button
		adapter = new BoardGameAdapter();
		setListAdapter(adapter);
		if (isGeekDown) {
			setTitle(getResources().getString(R.string.bgg_down_title));
		} else {
			setTitle(String.format(getResources().getString(R.string.bg_list_title), count, searchText));
		}
	}

	// gets the game id from the list item when clicked
	public void onListItemClick(ListView l, View v, int position, long id) {
		int gameId = ((BoardGame) (adapter.getItem(position))).getGameId();
		viewBoardGame(gameId);
	}

	// calls the board game intent
	private void viewBoardGame(int gameId) {
		Intent intent = new Intent(this, ViewBoardGame.class);
		intent.putExtra("GAME_ID", gameId);
		startActivity(intent);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);

		// inflate the menu from XML
		MenuInflater menuInflater = getMenuInflater();
		menuInflater.inflate(R.menu.menu, menu);

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.search:
			onSearchRequested();
			return true;
		case R.id.view_cache:
			Intent intent = new Intent(this, ViewBoardGameList.class);
			intent.setAction(Intent.ACTION_VIEW);
			startActivity(intent);
			return true;
		case R.id.settings:
			startActivity(new Intent(this, Preferences.class));
			return true;
		case R.id.credits:
			Utility.CreateAboutDialog(this).show();
			return true;
		}
		return false;
	}

	public void getPreferences() {
		preferences = PreferenceManager.getDefaultSharedPreferences(this);
		exactSearch = preferences.getBoolean("exactSearch", true);
		skipResults = preferences.getBoolean("skipResults", true);
	}

	class BoardGameAdapter extends ArrayAdapter<BoardGame> {
		BoardGameAdapter() {
			super(ViewBoardGameList.this, android.R.layout.simple_list_item_1, boardGames);
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			View row = convertView;
			BoardGameWrapper wrapper = null;

			if (row == null) {
				LayoutInflater inflater = getLayoutInflater();
				row = inflater.inflate(R.layout.row, null);
				wrapper = new BoardGameWrapper(row);
				row.setTag(wrapper);
			} else {
				wrapper = (BoardGameWrapper) row.getTag();
			}

			wrapper.populateFrom(boardGames.get(position));

			return row;
		}
	}

	class BoardGameWrapper {
		// this class exists to help performance in binding the board game list
		private View row = null;
		private TextView name = null;
		private TextView year = null;
		private TextView gameId = null;

		public BoardGameWrapper(View row) {
			this.row = row;
		}

		void populateFrom(BoardGame bg) {
			getName().setText(bg.getName());
			if (bg.getYearPublished() > 0) {
				getYear().setText("" + bg.getYearPublished());
			}
			getGameId().setText(
				String.format(getResources().getString(R.string.id_list_text), bg.getGameId()));
		}

		TextView getName() {
			if (name == null) {
				name = (TextView) row.findViewById(R.id.name);
			}
			return name;
		}

		TextView getYear() {
			if (year == null) {
				year = (TextView) row.findViewById(R.id.year);
			}
			return year;
		}

		TextView getGameId() {
			if (gameId == null) {
				gameId = (TextView) row.findViewById(R.id.gameId);
			}
			return gameId;
		}
	}
}