package com.boardgamegeek.view;

import java.net.URL;
import java.text.DecimalFormat;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import android.app.TabActivity;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.SQLException;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TabHost;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.boardgamegeek.BoardGameHandler;
import com.boardgamegeek.DataHelper;
import com.boardgamegeek.Preferences;
import com.boardgamegeek.R;
import com.boardgamegeek.Utility;
import com.boardgamegeek.BoardGameGeekData.BoardGames;
import com.boardgamegeek.BoardGameGeekData.Thumbnails;
import com.boardgamegeek.model.BoardGame;
import com.boardgamegeek.ui.BoardgamesActivity;
import com.boardgamegeek.ui.LogPlayActivity;

public class BoardGameView extends TabActivity {

	public static BoardGame boardGame = null;
	final Handler handler = new Handler();
	private final String LOG_TAG = "BoardGameGeek";
	private final String gameIdKey = "GAME_ID";
	private boolean imageLoad;
	private long cacheDuration;
	private int gameId;
	private TabHost tabHost;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// allow type-to-search
		setDefaultKeyMode(DEFAULT_KEYS_SEARCH_LOCAL);

		this.setContentView(R.layout.viewboardgame);
		setupTabs();

		if (savedInstanceState != null) {
			gameId = savedInstanceState.getInt(gameIdKey);
		}
		if (gameId == 0) {
			gameId = getIntent().getExtras().getInt(gameIdKey);
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
		super.onSaveInstanceState(outState);
		// save game ID; if activity is paused, it can resume with this ID
		outState.putInt(gameIdKey, gameId);
	}

	private void getBoardGame() {

		// display a progress message while fetching the game data
		showMessage(R.string.downloading_message, true);

		if (boardGame != null && boardGame.getGameId() == gameId) {
			// no need to retrieve the game, we already have it
			updateUI();
			return;
		}

		// remove the current, so it displays nothing if the connection fails
		boardGame = null;

		// see if the game is in the database
		Uri boardgameUri = Uri.withAppendedPath(BoardGames.CONTENT_URI, "" + gameId);
		Cursor cursor = managedQuery(boardgameUri, null, null, null, null);
		if (cursor.moveToFirst()) {
			// found in the database
			Long date = cursor.getLong(cursor.getColumnIndex(BoardGames.UPDATED_DATE));
			Long now = System.currentTimeMillis();
			if (date + cacheDuration > now) {
				// data is fresh enough to use
				boardGame = DataHelper.createBoardGame(this, cursor);
				updateUI();
				return;
			}
		}
		// game is not in database or is too old
		new Thread() {
			public void run() {
				try {
					// set URL
					URL url = new URL(Utility.siteUrl + "xmlapi/boardgame/" + gameId + "&stats=1");

					// create a new SAX parser and get an XML reader from it
					SAXParser saxParser = SAXParserFactory.newInstance().newSAXParser();
					XMLReader xmlReader = saxParser.getXMLReader();

					// set the XML reader's content handler and parse the XML
					BoardGameHandler boardGameHandler = new BoardGameHandler();
					xmlReader.setContentHandler(boardGameHandler);
					xmlReader.parse(new InputSource(url.openStream()));

					// get the parsed data as an object
					boardGame = boardGameHandler.getBoardGame();
				} catch (Exception e) {
					Log.d(LOG_TAG, "Exception", e);
				}
				handler.post(updateResults);
			}
		}.start();
	}

	// get results from handler
	final Runnable updateResults = new Runnable() {
		public void run() {
			if (boardGame != null) {
				updateUI();
				new Thread() {
					public void run() {
						DataHelper.addToDatabase(BoardGameView.this, boardGame);
						handler.post(toastUpdate);
					}
				}.start();
			} else {
				showMessage(R.string.error_downloading, false);
			}
		}
	};

	final Runnable toastUpdate = new Runnable() {
		public void run() {
			Toast.makeText(BoardGameView.this, R.string.database_updated, Toast.LENGTH_SHORT).show();
		}
	};

	final Runnable updateImageResults = new Runnable() {
		public void run() {
			ImageView thumbnailView = (ImageView) findViewById(R.id.thumbnail);
			ProgressBar thumbnailProgressBar = (ProgressBar) findViewById(R.id.thumbnailProgress);

			thumbnailProgressBar.setVisibility(View.GONE);
			thumbnailView.setVisibility(View.VISIBLE);

			if (boardGame.getThumbnail() != null) {
				thumbnailView.setImageDrawable(boardGame.getThumbnail());
			} else {
				thumbnailView.setImageDrawable(getResources().getDrawable(R.drawable.noimage));
			}
		}
	};

	private void updateImage() {
		ImageView thumbnailView = (ImageView) findViewById(R.id.thumbnail);
		ProgressBar thumbnailProgressBar = (ProgressBar) findViewById(R.id.thumbnailProgress);

		if (imageLoad && !TextUtils.isEmpty(boardGame.getThumbnailUrl())) {
			if (boardGame.getThumbnail() != null) {
				// we already have the image; show it
				thumbnailProgressBar.setVisibility(View.GONE);
				thumbnailView.setVisibility(View.VISIBLE);
				thumbnailView.setImageDrawable(boardGame.getThumbnail());
			} else {
				// we don't have the image; go get it
				thumbnailProgressBar.setVisibility(View.VISIBLE);
				thumbnailView.setVisibility(View.GONE);
				new Thread() {
					public void run() {
						Drawable thumbnail = null;
						// try to get it from the database
						Uri uri = Uri.withAppendedPath(Thumbnails.CONTENT_URI, ""
							+ boardGame.getThumbnailId());
						Cursor cursor = managedQuery(uri, null, null, null, null);
						if (cursor.moveToFirst()) {
							// found, use it
							thumbnail = Drawable.createFromPath(cursor.getString(cursor
								.getColumnIndex(Thumbnails.PATH)));
						} else if (!TextUtils.isEmpty(boardGame.getThumbnailUrl())) {
							// not found, get it from the site
							thumbnail = Utility.getImage(boardGame.getThumbnailUrl());
							// and safe it for later
							if (thumbnail != null) {
								ContentValues values = new ContentValues();
								values.put(Thumbnails._ID, boardGame.getThumbnailId());
								values.put(Thumbnails.DATA, Utility.ConvertToByteArry(thumbnail));
								try {
									getContentResolver().insert(Thumbnails.CONTENT_URI, values);
								} catch (SQLException ex) {
									Log.w(LOG_TAG, "Didn't insert thumbnail: " + ex.toString());
								}
							}
						}
						boardGame.setThumbnail(thumbnail);
						handler.post(updateImageResults);
					}
				}.start();
			}
		} else {
			// don't show the image
			// this causes the thumbnail view to take no space
			thumbnailView.setVisibility(View.GONE);
			thumbnailProgressBar.setVisibility(View.GONE);
			thumbnailView.setImageDrawable(null);
		}
	}

	// updates UI after running progress dialog
	private void updateUI() {
		// declare the GUI variables
		TextView title = (TextView) findViewById(R.id.title);
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
		Drawable wholestar = getResources().getDrawable(R.drawable.star_yellow);
		Drawable halfstar = getResources().getDrawable(R.drawable.star_yellowhalf);
		Drawable nostar = getResources().getDrawable(R.drawable.star_white);

		String gameRating = getResources().getString(R.string.user_rating) + ": "
			+ new DecimalFormat("#0.00").format(boardGame.getAverage()) + " / 10 ("
			+ boardGame.getRatingCount() + " Ratings)";

		// display information
		title.setText(boardGame.getName());
		rating.setText(gameRating);
		((TextView) findViewById(R.id.rank)).setText("" + boardGame.getRankDescription());
		((TextView) findViewById(R.id.yearPublished)).setText(boardGame.getYearPublishedDescription());
		((TextView) findViewById(R.id.numOfPlayers)).setText(boardGame.getPlayers());
		TableRow tr = (TableRow) findViewById(R.id.playingTimeRow);
		if (boardGame.getPlayingTime() == 0) {
			tr.setVisibility(View.GONE);
		} else {
			((TextView) findViewById(R.id.playingTime)).setText(boardGame.getPlayingTimeDescription());
			tr.setVisibility(View.VISIBLE);
		}
		tr = (TableRow) findViewById(R.id.suggestedAgesRow);
		if (boardGame.getAge() == 0) {
			tr.setVisibility(View.GONE);
		} else {
			((TextView) findViewById(R.id.suggestedAges)).setText(boardGame.getAgeDescription());
			tr.setVisibility(View.VISIBLE);
		}

		((TextView) findViewById(R.id.gameId)).setText("" + boardGame.getGameId());
		((TextView) findViewById(R.id.description)).setText(boardGame.getDescription());

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

		// hide the message, show the tab host
		LinearLayout ll = (LinearLayout) findViewById(R.id.gameProgressMessage);
		TabHost th = (TabHost) findViewById(android.R.id.tabhost);
		ll.setVisibility(View.GONE);
		th.setVisibility(View.VISIBLE);

		setTitle(String.format(getResources().getString(R.string.bg_view_title), boardGame.getName()));
		updateImage();
	}

	private void showMessage(int messageResource, boolean showProgress) {
		TextView tv = (TextView) findViewById(R.id.gameMessage);
		ProgressBar pb = (ProgressBar) findViewById(R.id.gameProgress);
		tv.setText(messageResource);

		// hide the tab host, show the tab message
		LinearLayout ll = (LinearLayout) findViewById(R.id.gameProgressMessage);
		TabHost th = (TabHost) findViewById(android.R.id.tabhost);
		ll.setVisibility(View.VISIBLE);
		th.setVisibility(View.GONE);
		pb.setVisibility(showProgress ? View.VISIBLE : View.GONE);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);

		// inflate the menu from XML
		MenuInflater menuInflater = getMenuInflater();
		menuInflater.inflate(R.menu.boardgame_menu, menu);

		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		// only allow logging a play once the game is populated
		MenuItem mi = menu.findItem(R.id.log_play);
		mi.setEnabled(boardGame != null);
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.search:
			onSearchRequested();
			return true;
		case R.id.view_database:
			Intent intent = new Intent(this, BoardgamesActivity.class);
			intent.setAction(Intent.ACTION_VIEW);
			startActivity(intent);
			return true;
		case R.id.log_play:
			logPlay(false);
			break;
		case R.id.log_play_quick:
			logPlay(true);
			break;
		case R.id.settings:
			startActivity(new Intent(this, Preferences.class));
			return true;
		case R.id.credits:
			startActivity(new Intent(this, AboutView.class));
			return true;
		}
		return false;
	}

	private void logPlay(boolean quick) {
		Intent intent = new Intent(this, LogPlayActivity.class);
		intent.setAction(quick ? Intent.ACTION_VIEW: Intent.ACTION_EDIT);
		intent.putExtra("GAME_ID", gameId);
		intent.putExtra("GAME_NAME", boardGame.getName());
		startActivity(intent);
	}

	public void getPreferences() {
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
		imageLoad = preferences.getBoolean("imageLoad", true);
		cacheDuration = Utility.parseInt(preferences.getString("cacheDuration", "259200000"), 259200000);
	}

	private void setupTabs() {
		tabHost = getTabHost();
		tabHost.addTab(tabHost.newTabSpec("tabMain")
			.setIndicator(getResources().getString(R.string.main_tab_title), getResources().getDrawable(R.drawable.ic_tab_main))
			.setContent(R.id.mainTab));
		tabHost.addTab(tabHost.newTabSpec("tabStats")
			.setIndicator(getResources().getString(R.string.stats_tab_title), getResources().getDrawable(R.drawable.ic_tab_stats))
			.setContent(new Intent(this, BoardGameStatsTab.class)));
		tabHost.addTab(tabHost.newTabSpec("tabExtra")
			.setIndicator(getResources().getString(R.string.extra_tab_title), getResources().getDrawable(R.drawable.ic_tab_extra))
			.setContent(new Intent(this, BoardGameExtraTab.class)));
		tabHost.addTab(tabHost.newTabSpec("tabLinks")
			.setIndicator(getResources().getString(R.string.links_tab_title), getResources().getDrawable(R.drawable.ic_tab_links))
			.setContent(new Intent(this, BoardGameLinksTab.class)));
		tabHost.addTab(tabHost.newTabSpec("tabPolls")
			.setIndicator(getResources().getString(R.string.polls_tab_title), getResources().getDrawable(R.drawable.ic_tab_polls))
			.setContent(new Intent(this, BoardGamePollsTab.class)));
	}
}