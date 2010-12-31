package com.boardgamegeek.ui;

import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import android.app.ListActivity;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
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
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.boardgamegeek.BoardGameListHandler;
import com.boardgamegeek.DataHelper;
import com.boardgamegeek.Preferences;
import com.boardgamegeek.R;
import com.boardgamegeek.Utility;
import com.boardgamegeek.BoardGameGeekData.BoardGames;
import com.boardgamegeek.model.BoardGame;
import com.boardgamegeek.util.UIUtils;

public class BoardgamesActivity extends ListActivity {

	private final String TAG = "BoardgamesActivity";
	private String searchText;
	private List<BoardGame> boardGames = new ArrayList<BoardGame>();
	private ListAdapter adapter;
	private Handler handler = new Handler();
	private boolean exactSearch;
	private boolean skipResults;
	private boolean isFirstPass = true;
	private boolean isGeekDown = false;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setDefaultKeyMode(DEFAULT_KEYS_SEARCH_LOCAL); // allow type-to-search
		setContentView(R.layout.activity_boardgames);
		parseIntent(getIntent());
	}

	@Override
	public void onResume() {
		super.onResume();
		getPreferences();
	}

	@Override
	public void onNewIntent(Intent intent) {
		parseIntent(intent);
	}

	@Override
	public void setTitle(CharSequence title) {
		UIUtils.setTitle(this, title);
	}

	public void onHomeClick(View v) {
		UIUtils.resetToHome(this);
	}

	public void onSearchClick(View v) {
		onSearchRequested();
	}

	private void parseIntent(Intent intent) {

		if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
			// user is searching
			searchText = intent.getExtras().getString(SearchManager.QUERY);
			if (TextUtils.isEmpty(searchText)) {
				// search text was blank, but show the database anyway
				Log.w(TAG, "Search performed with no search text");
				viewDatabase();
			} else {
				// search BGG
				getBoardGameList();
			}
		} else if (Intent.ACTION_VIEW.equals(intent.getAction())) {
			// user wants to view something
			String data = intent.getDataString();
			if (TextUtils.isEmpty(data)) {
				// no game specified means they want the whole database
				viewDatabase();
			} else {
				// a game ID is specified
				Uri uri = Uri.parse(data);
				viewBoardGame(Utility.parseInt(uri.getLastPathSegment(), 0));
				finish();
			}
		} else {
			// still show database
			Log.w(TAG, "Received bad intent action: " + intent.getAction());
			viewDatabase();
		}
	}

	private void viewDatabase() {
		updateMessage(R.string.empty_database, null, false);
		Cursor c = managedQuery(BoardGames.CONTENT_URI, null, null, null, null);
		startManagingCursor(c);
		adapter = new BoardGameCursorAdapter(this, c);
		setListAdapter(adapter);
		setTitle(R.string.view_database_title);
	}

	private void getBoardGameList() {
		// clear existing game list items
		boardGames.clear();

		// display a progress dialog while fetching the game data
		updateMessage(R.string.search_message, searchText, true);
		setTitle(R.string.search_title);

		new Thread() {
			public void run() {
				try {
					isGeekDown = false;
					// set URL
					String queryUrl = Utility.siteUrl + "xmlapi/search?search=" + searchText;
					if (exactSearch && isFirstPass) {
						queryUrl += "&exact=1";
					}

					URL url = new URL(queryUrl.replace(" ", "+"));
					Log.d(TAG, "Query: " + url.toString());

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
					Log.w(TAG, "PULLING XML - Failed", e);
				} catch (Exception e) {
					Log.w(TAG, "PULLING XML - Failed", e);
					if (e.getMessage().equals("down")) {
						isGeekDown = true;
					}
				}
				handler.post(updateResults);
			}
		}.start();
	}

	private void updateMessage(int messageResource, String optionalText, boolean showProgress) {
		TextView tv = (TextView) findViewById(R.id.listMessage);
		ProgressBar pb = (ProgressBar) findViewById(R.id.listProgress);
		if (TextUtils.isEmpty(optionalText)) {
			tv.setText(messageResource);
		} else {
			tv.setText(String.format(getResources().getString(messageResource), optionalText));
		}
		pb.setVisibility(showProgress ? View.VISIBLE : View.GONE);
	}

	// get results from handler
	final Runnable updateResults = new Runnable() {
		public void run() {
			updateUI();
		}
	};

	// updates UI after running progress dialog
	private void updateUI() {
		if (boardGames == null) {
			boardGames = new ArrayList<BoardGame>(0);
		}
		int count = boardGames.size();

		if (count == 0) {
			if (isGeekDown) {
				updateMessage(R.string.bgg_down, null, false);
				setTitle(getResources().getString(R.string.bgg_down_title));
			} else if (exactSearch && isFirstPass) {
				// try again if exactSearch is on and no results were found
				isFirstPass = false;
				getBoardGameList();
			} else {
				// display if no results are found
				updateMessage(R.string.no_results, searchText, false);
				setTitle(String.format(getResources().getString(R.string.bg_list_title), count, searchText));
			}
		} else if (count == 1) {
			if (skipResults) {
				// skip directly to game if only one result
				BoardGame boardGame = boardGames.get(0);
				viewBoardGame(boardGame.getGameId());
				finish();
			}
		} else {
			setTitle(String.format(getResources().getString(R.string.bg_list_title), count, searchText));
		}

		// display game list (even if we skip results, since user may use back
		// button)
		adapter = new BoardGameAdapter();
		setListAdapter(adapter);
	}

	// gets the game id from the list item when clicked
	public void onListItemClick(ListView l, View v, int position, long id) {
		int gameId = 0;
		Object item = adapter.getItem(position);
		if (item instanceof BoardGame) {
			gameId = ((BoardGame) item).getGameId();
		} else if (item instanceof Cursor) {
			gameId = (int) id;
		}
		if (gameId > 0) {
			viewBoardGame(gameId);
		}
	}

	// calls the board game intent
	private void viewBoardGame(int gameId) {
		Intent intent = new Intent(this, OldBoardgameActivity.class);
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
		case R.id.view_database:
			Intent intent = new Intent(this, BoardgamesActivity.class);
			intent.setAction(Intent.ACTION_VIEW);
			startActivity(intent);
			return true;
		case R.id.settings:
			startActivity(new Intent(this, Preferences.class));
			return true;
		case R.id.credits:
			startActivity(new Intent(this, AboutActivity.class));
			return true;
		}
		return false;
	}

	private void getPreferences() {
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
		exactSearch = preferences.getBoolean("exactSearch", true);
		skipResults = preferences.getBoolean("skipResults", true);
	}

	class BoardGameCursorAdapter extends CursorAdapter {
		private LayoutInflater mInflater;

		public BoardGameCursorAdapter(Context context, Cursor c) {
			super(context, c);
			mInflater = getLayoutInflater();
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			ViewHolder holder = (ViewHolder) view.getTag();
			holder.name.setText(cursor.getString(cursor.getColumnIndex(BoardGames.NAME)));
			holder.year.setText(cursor.getString(cursor.getColumnIndex(BoardGames.YEAR)));
			Drawable thumbnail = DataHelper.getThumbnail(cursor);
			if (thumbnail == null) {
				holder.thumbnail.setVisibility(View.GONE);
			} else {
				holder.thumbnail.setVisibility(View.VISIBLE);
				holder.thumbnail.setImageDrawable(thumbnail);
			}
		}

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {
			View row = mInflater.inflate(R.layout.row2, parent, false);
			ViewHolder holder = new ViewHolder(row);
			row.setTag(holder);
			return row;
		}
	}

	class BoardGameAdapter extends ArrayAdapter<BoardGame> {
		private LayoutInflater mInflater;

		BoardGameAdapter() {
			super(BoardgamesActivity.this, android.R.layout.simple_list_item_1, boardGames);
			mInflater = getLayoutInflater();
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder holder;
			if (convertView == null) {
				convertView = mInflater.inflate(R.layout.row, parent, false);
				holder = new ViewHolder(convertView);
				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}

			BoardGame bg = boardGames.get(position);
			if (bg != null) {
				holder.name.setText(bg.getName());
				if (bg.getYearPublished() > 0) {
					holder.year.setText("" + bg.getYearPublished());
				}
				holder.gameId.setText(String.format(getResources().getString(R.string.id_list_text), bg
					.getGameId()));
			}

			return convertView;
		}
	}

	static class ViewHolder {
		TextView name;
		TextView year;
		TextView gameId;
		ImageView thumbnail;

		public ViewHolder(View view) {
			name = (TextView) view.findViewById(R.id.name);
			year = (TextView) view.findViewById(R.id.year);
			gameId = (TextView) view.findViewById(R.id.gameId);
			thumbnail = (ImageView) view.findViewById(R.id.listThumbnail);
		}
	}
}