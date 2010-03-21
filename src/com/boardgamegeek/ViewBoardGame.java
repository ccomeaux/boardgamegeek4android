package com.boardgamegeek;

import java.net.URL;
import java.text.DecimalFormat;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import com.boardgamegeek.BoardGameGeekData.Artists;
import com.boardgamegeek.BoardGameGeekData.BoardGameArtists;
import com.boardgamegeek.BoardGameGeekData.BoardGameCategories;
import com.boardgamegeek.BoardGameGeekData.BoardGameDesigners;
import com.boardgamegeek.BoardGameGeekData.BoardGameExpansions;
import com.boardgamegeek.BoardGameGeekData.BoardGameMechanics;
import com.boardgamegeek.BoardGameGeekData.BoardGamePollResult;
import com.boardgamegeek.BoardGameGeekData.BoardGamePollResults;
import com.boardgamegeek.BoardGameGeekData.BoardGamePolls;
import com.boardgamegeek.BoardGameGeekData.BoardGamePublishers;
import com.boardgamegeek.BoardGameGeekData.BoardGames;
import com.boardgamegeek.BoardGameGeekData.Categories;
import com.boardgamegeek.BoardGameGeekData.Designers;
import com.boardgamegeek.BoardGameGeekData.Mechanics;
import com.boardgamegeek.BoardGameGeekData.Publishers;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.app.TabActivity;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TabHost;
import android.widget.TextView;

public class ViewBoardGame extends TabActivity {

	public static BoardGame boardGame = null;
	private final int ID_DIALOG_SEARCHING = 1;
	final Handler handler = new Handler();
	private final String LOG_TAG = "BoardGameGeek";
	private final String gameIdKey = "GAME_ID";
	private SharedPreferences preferences;
	private boolean imageLoad;
	private String gameId;
	private TabHost tabHost;
	private static final long diff = 259200000; // 3 days

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// allow type-to-search
		setDefaultKeyMode(DEFAULT_KEYS_SEARCH_LOCAL);

		this.setContentView(R.layout.viewboardgame);
		setupTabs();

		if (savedInstanceState != null) {
			gameId = savedInstanceState.getString(gameIdKey);
		}
		if (gameId == null || gameId.length() == 0) {
			gameId = getIntent().getExtras().getString(gameIdKey);
		}
	}

	@Override
	public void onResume() {
		super.onResume();

		getPreferences();
		getBoardGame();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		Log.d(LOG_TAG, "onSaveInstanceState");
		removeDialogs();
		super.onSaveInstanceState(outState);

		// save game ID; if activity is paused, it can resume with this ID
		outState.putString(gameIdKey, gameId);
	}

	private void getBoardGame() {

		if (boardGame != null && boardGame.getGameId().equalsIgnoreCase(gameId)) {
			// no need to retrieve the game, we already have it
			updateUI();
			return;
		}

		// display a progress dialog while fetching the game data
		showDialog(ID_DIALOG_SEARCHING);

		// remove the current, so it displays nothing if the connection fails
		boardGame = null;

		// see if the game is in the database
		Uri boardgameUri = Uri.withAppendedPath(BoardGames.CONTENT_URI, gameId);
		Cursor cursor = managedQuery(boardgameUri, null, null, null, null);
		if (cursor.moveToFirst()) {
			// found in the database
			Long date = cursor.getLong(cursor.getColumnIndex(BoardGames.UPDATED_DATE));
			Long now = System.currentTimeMillis();
			if (date + diff > now) {
				// data is fresh enough to use
				createBoardGame(cursor);

				// update the UI
				updateResults.run();
				return;
			}
		}
		// game is not in database or is too old
		new Thread() {
			public void run() {
				try {
					// set URL
					URL url = new URL("http://www.boardgamegeek.com/xmlapi/boardgame/" + gameId + "&stats=1");

					// create a new SAX parser and get an XML reader from it
					SAXParser saxParser = SAXParserFactory.newInstance().newSAXParser();
					XMLReader xmlReader = saxParser.getXMLReader();

					// set the XML reader's content handler and parse the XML
					BoardGameHandler boardGameHandler = new BoardGameHandler();
					xmlReader.setContentHandler(boardGameHandler);
					xmlReader.parse(new InputSource(url.openStream()));

					// get the parsed data as an object
					boardGame = boardGameHandler.getBoardGame();

					addToDatabase();
				} catch (Exception e) {
					Log.d(LOG_TAG, "Exception", e);
				}
				handler.post(updateResults);
			}
		}.start();
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		if (id == ID_DIALOG_SEARCHING) {
			// show dialog box while searching and parsing XML
			ProgressDialog dialog = new ProgressDialog(this);
			dialog.setTitle(R.string.dialog_working_title);
			dialog.setMessage(getResources().getString(R.string.dialog_working_message));
			dialog.setIndeterminate(true);
			dialog.setCancelable(true);
			return dialog;
		}

		return super.onCreateDialog(id);
	}

	protected void removeDialogs() {
		try {
			removeDialog(ID_DIALOG_SEARCHING);
		} catch (Exception e) {
			Log.w(LOG_TAG, "removeDialogs Failed", e);
		}
	}

	// get results from handler
	final Runnable updateResults = new Runnable() {
		public void run() {
			if (imageLoad) {
				new Thread() {
					public void run() {
						boardGame.setThumbnail(Utility.getImage(boardGame.getThumbnailUrl()));
						handler.post(updateImageResults);
					}
				}.start();
			}
			updateUI();
		}
	};

	final Runnable updateImageResults = new Runnable() {
		public void run() {
			updateImage();
		}
	};

	private void updateImage() {
		ImageView thumbnail = (ImageView) findViewById(R.id.thumbnail);
		if (imageLoad) {
			if (boardGame.getThumbnail() != null && !boardGame.getThumbnailUrl().equals("")) {
				thumbnail.setImageDrawable(boardGame.getThumbnail());
			} else {
				thumbnail.setImageDrawable(getResources().getDrawable(R.drawable.noimage));
			}
		} else {
			thumbnail.setImageDrawable(null);
		}
	}

	// updates UI after running progress dialog
	private void updateUI() {
		// declare the GUI variables
		TextView title = (TextView) findViewById(R.id.title);
		TextView rank = (TextView) findViewById(R.id.rank);
		TextView rating = (TextView) findViewById(R.id.rating);
		ImageView star1 = (ImageView) findViewById(R.id.star1);
		ImageView star2 = (ImageView) findViewById(R.id.star2);
		ImageView star3 = (ImageView) findViewById(R.id.star3);
		ImageView star4 = (ImageView) findViewById(R.id.star4);
		ImageView star5 = (ImageView) findViewById(R.id.star5);
		ImageView star6 = (ImageView) findViewById(R.id.star6);
		ImageView star7 = (ImageView) findViewById(R.id.star7);
		ImageView star8 = (ImageView) findViewById(R.id.star8);
		ImageView star9 = (ImageView) findViewById(R.id.star9);
		ImageView star10 = (ImageView) findViewById(R.id.star10);
		TextView information = (TextView) findViewById(R.id.information);
		TextView description = (TextView) findViewById(R.id.description);
		Drawable wholestar = getResources().getDrawable(R.drawable.star_yellow);
		Drawable halfstar = getResources().getDrawable(R.drawable.star_yellowhalf);
		Drawable nostar = getResources().getDrawable(R.drawable.star_white);

		// get the game information from the object
		String gameRank;
		if (boardGame.getRank() == 0) {
			gameRank = String.format(getResources().getString(R.string.rank), getResources().getString(
				R.string.not_available));
		} else {
			gameRank = String.format(getResources().getString(R.string.rank), boardGame.getRank());
		}
		String gameRating = getResources().getString(R.string.user_rating) + ": "
			+ new DecimalFormat("#0.00").format(boardGame.getAverage()) + " / 10 ("
			+ boardGame.getRatingCount() + " Ratings)";
		String gameInfo = boardGame.getGameInfo();
		String gameDescription = boardGame.getDescription();

		// display information
		title.setText(boardGame.getName());
		rank.setText(gameRank);
		updateImage();
		rating.setText(gameRating);

		// calculate and display star rating
		if (boardGame.getAverage() >= 0.75)
			star1.setImageDrawable(wholestar);
		else if (boardGame.getAverage() >= 0.25)
			star1.setImageDrawable(halfstar);
		else
			star1.setImageDrawable(nostar);
		if (boardGame.getAverage() >= 1.75)
			star2.setImageDrawable(wholestar);
		else if (boardGame.getAverage() >= 1.25)
			star2.setImageDrawable(halfstar);
		else
			star2.setImageDrawable(nostar);
		if (boardGame.getAverage() >= 2.75)
			star3.setImageDrawable(wholestar);
		else if (boardGame.getAverage() >= 2.25)
			star3.setImageDrawable(halfstar);
		else
			star3.setImageDrawable(nostar);
		if (boardGame.getAverage() >= 3.75)
			star4.setImageDrawable(wholestar);
		else if (boardGame.getAverage() >= 3.25)
			star4.setImageDrawable(halfstar);
		else
			star4.setImageDrawable(nostar);
		if (boardGame.getAverage() >= 4.75)
			star5.setImageDrawable(wholestar);
		else if (boardGame.getAverage() >= 4.25)
			star5.setImageDrawable(halfstar);
		else
			star5.setImageDrawable(nostar);
		if (boardGame.getAverage() >= 5.75)
			star6.setImageDrawable(wholestar);
		else if (boardGame.getAverage() >= 5.25)
			star6.setImageDrawable(halfstar);
		else
			star6.setImageDrawable(nostar);
		if (boardGame.getAverage() >= 6.75)
			star7.setImageDrawable(wholestar);
		else if (boardGame.getAverage() >= 6.25)
			star7.setImageDrawable(halfstar);
		else
			star7.setImageDrawable(nostar);
		if (boardGame.getAverage() >= 7.75)
			star8.setImageDrawable(wholestar);
		else if (boardGame.getAverage() >= 7.25)
			star8.setImageDrawable(halfstar);
		else
			star8.setImageDrawable(nostar);
		if (boardGame.getAverage() >= 8.75)
			star9.setImageDrawable(wholestar);
		else if (boardGame.getAverage() >= 8.25)
			star9.setImageDrawable(halfstar);
		else
			star9.setImageDrawable(nostar);
		if (boardGame.getAverage() >= 9.75)
			star10.setImageDrawable(wholestar);
		else if (boardGame.getAverage() >= 9.25)
			star10.setImageDrawable(halfstar);
		else
			star10.setImageDrawable(nostar);

		// display rest of information
		information.setText(gameInfo);
		description.setText(gameDescription);
		// remove progress dialog (if any)
		removeDialogs();

		setTitle(String.format(getResources().getString(R.string.bg_view_title), boardGame.getName()));
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
			getBoardGame();
			return true;
		case R.id.settings:
			startActivity(new Intent(this, Preferences.class));
			return true;
		case R.id.credits:
			Dialog dialog = new Dialog(this);
			dialog.setContentView(R.layout.dialog);
			dialog.setTitle(R.string.thanks_title);
			dialog.show();
			return true;
		}
		return false;
	}

	public void getPreferences() {
		preferences = PreferenceManager.getDefaultSharedPreferences(this);
		imageLoad = preferences.getBoolean("imageLoad", true);
	}

	private void setupTabs() {
		tabHost = getTabHost();
		tabHost.addTab(tabHost.newTabSpec("tabMain").setIndicator(
			getResources().getString(R.string.main_tab_title)).setContent(R.id.mainTab));
		tabHost.addTab(tabHost.newTabSpec("tabStats").setIndicator(
			getResources().getString(R.string.stats_tab_title)).setContent(
			new Intent(this, BoardGameStatsTab.class)));
		tabHost.addTab(tabHost.newTabSpec("tabExtra").setIndicator(
			getResources().getString(R.string.extra_tab_title)).setContent(
			new Intent(this, BoardGameExtraTab.class)));
		tabHost.addTab(tabHost.newTabSpec("tabLinks").setIndicator(
			getResources().getString(R.string.links_tab_title)).setContent(
			new Intent(this, BoardGameLinksTab.class)));
		tabHost.addTab(tabHost.newTabSpec("tabPolls").setIndicator(
			getResources().getString(R.string.polls_tab_title)).setContent(
			new Intent(this, BoardGamePollsTab.class)));
	}

	private void addToDatabase() {

		if (boardGame == null) {
			Log.w(LOG_TAG, "boardGame was unexpectedly null");
			return;
		}

		// delete to make sure all of the child relationships are cleaned up
		Uri uri = Uri.withAppendedPath(BoardGames.CONTENT_URI, boardGame.getGameId());
		getContentResolver().delete(uri, null, null);

		ContentValues values = new ContentValues();
		values.put(BoardGames._ID, boardGame.getGameId());
		values.put(BoardGames.NAME, boardGame.getName());
		values.put(BoardGames.YEAR, boardGame.getYearPublished());
		values.put(BoardGames.MIN_PLAYERS, boardGame.getMinPlayers());
		values.put(BoardGames.MAX_PLAYERS, boardGame.getMaxPlayers());
		values.put(BoardGames.PLAYING_TIME, boardGame.getPlayingTime());
		values.put(BoardGames.AGE, boardGame.getAge());
		values.put(BoardGames.DESCRIPTION, boardGame.getDescription());
		values.put(BoardGames.THUMBNAIL_URL, boardGame.getThumbnailUrl());
		values.put(BoardGames.RATING_COUNT, boardGame.getRatingCount());
		values.put(BoardGames.AVERAGE, boardGame.getAverage());
		values.put(BoardGames.BAYES_AVERAGE, boardGame.getBayesAverage());
		values.put(BoardGames.RANK, boardGame.getRank());
		values.put(BoardGames.STANDARD_DEVIATION, boardGame.getStandardDeviation());
		values.put(BoardGames.MEDIAN, boardGame.getMedian());
		values.put(BoardGames.OWNED_COUNT, boardGame.getOwnedCount());
		values.put(BoardGames.TRADING_COUNT, boardGame.getTradingCount());
		values.put(BoardGames.WANTING_COUNT, boardGame.getWantingCount());
		values.put(BoardGames.WISHING_COUNT, boardGame.getWishingCount());
		values.put(BoardGames.COMMENT_COUNT, boardGame.getCommentCount());
		values.put(BoardGames.WEIGHT_COUNT, boardGame.getWeightCount());
		values.put(BoardGames.AVERAGE_WEIGHT, boardGame.getAverageWeight());
		values.put(BoardGames.UPDATED_DATE, Long.valueOf(System.currentTimeMillis()));

		getContentResolver().insert(BoardGames.CONTENT_URI, values);

		for (int i = 0; i < boardGame.getDesignerCount(); i++) {
			String designerId = boardGame.getDesignerIdByPosition(i);
			String designerName = boardGame.getDesignerNameById(designerId);

			values.clear();
			values.put(Designers._ID, designerId);
			values.put(Designers.NAME, designerName);

			// ensure designer record is present and correct
			Uri designerUri = Uri.withAppendedPath(Designers.CONTENT_URI, designerId);
			Cursor cursor = managedQuery(designerUri, null, null, null, null);
			if (cursor.moveToFirst()) {
				if (designerName != cursor.getString(cursor.getColumnIndex(Designers.NAME))) {
					getContentResolver().update(designerUri, values, null, null);
				}
			} else {
				getContentResolver().insert(Designers.CONTENT_URI, values);
			}

			// add game/designer relationship record
			values.clear();
			values.put(BoardGameDesigners.BOARDGAME_ID, boardGame.getGameId());
			values.put(BoardGameDesigners.DESIGNER_ID, designerId);
			uri = getContentResolver().insert(BoardGameDesigners.CONTENT_URI, values);
		}

		for (int i = 0; i < boardGame.getArtistCount(); i++) {
			String artistId = boardGame.getArtistIdByPosition(i);
			String artistName = boardGame.getArtistNameById(artistId);

			values.clear();
			values.put(Artists._ID, artistId);
			values.put(Artists.NAME, artistId);

			Uri artistUri = Uri.withAppendedPath(Artists.CONTENT_URI, artistId);
			Cursor cursor = managedQuery(artistUri, null, null, null, null);
			if (cursor.moveToFirst()) {
				if (artistName != cursor.getString(cursor.getColumnIndex(Artists.NAME))) {
					getContentResolver().update(artistUri, values, null, null);
				}
			} else {
				getContentResolver().insert(Artists.CONTENT_URI, values);
			}

			// add game/artist relationship record
			values.clear();
			values.put(BoardGameArtists.BOARDGAME_ID, boardGame.getGameId());
			values.put(BoardGameArtists.ARTIST_ID, artistId);
			uri = getContentResolver().insert(BoardGameArtists.CONTENT_URI, values);
		}

		for (int i = 0; i < boardGame.getPublisherCount(); i++) {
			String publisherId = boardGame.getPublisherIdByPosition(i);
			String publisherName = boardGame.getPublisherNameById(publisherId);

			values.clear();
			values.put(Publishers._ID, publisherId);
			values.put(Publishers.NAME, publisherName);

			// ensure publisher record is present and correct
			Uri publisherUri = Uri.withAppendedPath(Publishers.CONTENT_URI, publisherId);
			Cursor cursor = managedQuery(publisherUri, null, null, null, null);
			if (cursor.moveToFirst()) {
				if (publisherName != cursor.getString(cursor.getColumnIndex(Publishers.NAME))) {
					getContentResolver().update(publisherUri, values, null, null);
				}
			} else {
				getContentResolver().insert(Publishers.CONTENT_URI, values);
			}

			// add game/Publisher relationship record
			values.clear();
			values.put(BoardGamePublishers.BOARDGAME_ID, boardGame.getGameId());
			values.put(BoardGamePublishers.PUBLISHER_ID, publisherId);
			uri = getContentResolver().insert(BoardGamePublishers.CONTENT_URI, values);
		}

		for (int i = 0; i < boardGame.getCategoryCount(); i++) {
			String CategoryId = boardGame.getCategoryIdByPosition(i);
			String CategoryName = boardGame.getCategoryNameById(CategoryId);

			values.clear();
			values.put(Categories._ID, CategoryId);
			values.put(Categories.NAME, CategoryName);

			// ensure category record is present and correct
			Uri categoryUri = Uri.withAppendedPath(Categories.CONTENT_URI, CategoryId);
			Cursor cursor = managedQuery(categoryUri, null, null, null, null);
			if (cursor.moveToFirst()) {
				if (CategoryName != cursor.getString(cursor.getColumnIndex(Categories.NAME))) {
					getContentResolver().update(categoryUri, values, null, null);
				}
			} else {
				getContentResolver().insert(Categories.CONTENT_URI, values);
			}

			// add game/category relationship record
			values.clear();
			values.put(BoardGameCategories.BOARDGAME_ID, boardGame.getGameId());
			values.put(BoardGameCategories.CATEGORY_ID, CategoryId);
			uri = getContentResolver().insert(BoardGameCategories.CONTENT_URI, values);
		}

		for (int i = 0; i < boardGame.getMechanicCount(); i++) {
			String mechanicId = boardGame.getMechanicIdByPosition(i);
			String mechanicName = boardGame.getMechanicNameById(mechanicId);

			values.clear();
			values.put(Mechanics._ID, mechanicId);
			values.put(Mechanics.NAME, mechanicName);

			// ensure mechanic record is present and correct
			Uri mechanicUri = Uri.withAppendedPath(Mechanics.CONTENT_URI, mechanicId);
			Cursor cursor = managedQuery(mechanicUri, null, null, null, null);
			if (cursor.moveToFirst()) {
				if (mechanicName != cursor.getString(cursor.getColumnIndex(Mechanics.NAME))) {
					getContentResolver().update(mechanicUri, values, null, null);
				}
			} else {
				getContentResolver().insert(Mechanics.CONTENT_URI, values);
			}

			// add game/mechanic relationship record
			values.clear();
			values.put(BoardGameMechanics.BOARDGAME_ID, boardGame.getGameId());
			values.put(BoardGameMechanics.MECHANIC_ID, mechanicId);
			uri = getContentResolver().insert(BoardGameMechanics.CONTENT_URI, values);
		}

		for (int i = 0; i < boardGame.getExpansionCount(); i++) {
			String expansionId = boardGame.getExpansionIdByPosition(i);
			String expansionName = boardGame.getExpansionNameById(expansionId);

			values.clear();
			values.put(BoardGames._ID, expansionId);
			values.put(BoardGames.NAME, expansionName);

			// ensure expansion record is present and correct
			Uri expansionUri = Uri.withAppendedPath(BoardGames.CONTENT_URI, expansionId);
			Cursor cursor = managedQuery(expansionUri, null, null, null, null);
			if (cursor.moveToFirst()) {
				if (expansionName != cursor.getString(cursor.getColumnIndex(BoardGames.NAME))) {
					getContentResolver().update(expansionUri, values, null, null);
				}
			} else {
				getContentResolver().insert(BoardGames.CONTENT_URI, values);
			}

			// add game/expansion relationship record
			values.clear();
			values.put(BoardGameExpansions.BOARDGAME_ID, boardGame.getGameId());
			values.put(BoardGameExpansions.EXPANSION_ID, expansionId);
			uri = getContentResolver().insert(BoardGameExpansions.CONTENT_URI, values);
		}

		for (int i = 0; i < boardGame.getPollCount(); i++) {
			Poll poll = boardGame.getPollByPosition(i);

			values.clear();
			values.put(BoardGamePolls.BOARDGAME_ID, boardGame.getGameId());
			values.put(BoardGamePolls.NAME, poll.getName());
			values.put(BoardGamePolls.TITLE, poll.getTitle());
			values.put(BoardGamePolls.VOTES, poll.getTotalVotes());
			uri = getContentResolver().insert(BoardGamePolls.CONTENT_URI, values);

			String pollId = uri.getLastPathSegment();
			for (PollResults results : poll.getResultsList()) {
				values.clear();
				values.put(BoardGamePollResults.POLL_ID, pollId);
				values.put(BoardGamePollResults.PLAYERS, results.getNumberOfPlayers());
				uri = getContentResolver().insert(BoardGamePollResults.CONTENT_URI, values);

				String resultsId = uri.getLastPathSegment();
				for (PollResult result : results.getResultList()) {
					values.clear();
					values.put(BoardGamePollResult.POLLRESULTS_ID, resultsId);
					values.put(BoardGamePollResult.LEVEL, result.getLevel());
					values.put(BoardGamePollResult.VALUE, result.getValue());
					values.put(BoardGamePollResult.VOTES, result.getNumberOfVotes());
					uri = getContentResolver().insert(BoardGamePollResult.CONTENT_URI, values);
				}
			}
		}

	}

	private void createBoardGame(Cursor cursor) {
		boardGame = new BoardGame(cursor);

		cursor = managedQuery(BoardGameDesigners.CONTENT_URI, new String[] { BoardGameDesigners.DESIGNER_ID,
			BoardGameDesigners.DESIGNER_NAME }, BoardGameDesigners.BOARDGAME_ID + "=" + gameId, null,
			BoardGameDesigners.DESIGNER_NAME);
		boardGame.CreateDesigners(cursor);

		cursor = managedQuery(BoardGameArtists.CONTENT_URI, new String[] { BoardGameArtists.ARTIST_ID,
			BoardGameArtists.ARTIST_NAME }, BoardGameArtists.BOARDGAME_ID + "=" + gameId, null,
			BoardGameArtists.ARTIST_NAME);
		boardGame.CreateArtists(cursor);

		cursor = managedQuery(BoardGamePublishers.CONTENT_URI, new String[] {
			BoardGamePublishers.PUBLISHER_ID, BoardGamePublishers.PUBLISHER_NAME },
			BoardGamePublishers.BOARDGAME_ID + "=" + gameId, null, BoardGamePublishers.PUBLISHER_NAME);
		boardGame.CreatePublishers(cursor);

		cursor = managedQuery(BoardGameCategories.CONTENT_URI, new String[] {
			BoardGameCategories.CATEGORY_ID, BoardGameCategories.CATEGORY_NAME },
			BoardGameCategories.BOARDGAME_ID + "=" + gameId, null, BoardGameCategories.CATEGORY_NAME);
		boardGame.CreateCategories(cursor);

		cursor = managedQuery(BoardGameMechanics.CONTENT_URI, new String[] { BoardGameMechanics.MECHANIC_ID,
			BoardGameMechanics.MECHANIC_NAME }, BoardGameMechanics.BOARDGAME_ID + "=" + gameId, null,
			BoardGameMechanics.MECHANIC_NAME);
		boardGame.CreateMechanics(cursor);

		cursor = managedQuery(BoardGameExpansions.CONTENT_URI, new String[] {
			BoardGameExpansions.EXPANSION_ID, BoardGameExpansions.EXPANSION_NAME },
			BoardGameExpansions.BOARDGAME_ID + "=" + gameId, null, BoardGameExpansions.EXPANSION_NAME);
		boardGame.CreateExpanions(cursor);

		cursor = managedQuery(BoardGamePolls.CONTENT_URI, null, BoardGamePolls.BOARDGAME_ID + "=" + gameId,
			null, null);
		boardGame.createPolls(cursor);

		if (cursor.moveToFirst()) {
			do {
				int pollId = cursor.getInt(cursor.getColumnIndex(BoardGamePolls._ID));
				Cursor resultsCursor = managedQuery(BoardGamePollResults.CONTENT_URI, null,
					BoardGamePollResults.POLL_ID + "=" + pollId, null, null);
				boardGame.createPollResults(resultsCursor);

				if (resultsCursor.moveToFirst()) {
					do {
						int resultsId = resultsCursor.getInt(resultsCursor
							.getColumnIndex(BoardGamePollResults._ID));
						Cursor resultCursor = managedQuery(BoardGamePollResult.CONTENT_URI, null,
							BoardGamePollResult.POLLRESULTS_ID + "=" + resultsId, null, null);
						boardGame.createPollResult(resultCursor);
					} while (resultsCursor.moveToNext());
				}
			} while (cursor.moveToNext());
		}
	}
}