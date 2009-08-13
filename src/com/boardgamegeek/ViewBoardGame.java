package com.boardgamegeek;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DecimalFormat;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.app.TabActivity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TabHost;
import android.widget.TextView;

public class ViewBoardGame extends TabActivity {
	// declare variables
	private BoardGame boardGame = null;
	private final int ID_DIALOG_SEARCHING = 1;
	private Drawable thumbnail_drawable;
	final Handler handler = new Handler();
	private final String DEBUG_TAG = "BoardGameGeek DEBUG:";
	private static final int IO_BUFFER_SIZE = 4 * 1024;
	private SharedPreferences preferences;
	boolean imageLoad;
	boolean viewLoaded = false;
	private TabHost tabHost;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// allow type-to-search
		setDefaultKeyMode(DEFAULT_KEYS_SEARCH_LOCAL);

		// get preferences
		getPreferences();

		// get the board game
		getBoardGame();
	}

	@Override
	public void onResume() {
		super.onResume();

		// get preferences
		getPreferences();
	}

	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);

		if (viewLoaded) {
			// update the UI
			updateUI();
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		Log.d(DEBUG_TAG, "onSaveInstanceState");

		// remove progress dialog (if any)
		removeDialogs();

		super.onSaveInstanceState(outState);
	}

	private void getBoardGame() {
		// get the game ID from the intent
		final String gameId = getIntent().getExtras().getString("GAME_ID");

		// display a progress dialog while fetching the game data
		showDialog(ID_DIALOG_SEARCHING);

		new Thread() {
			public void run() {
				try {
					// set URL
					URL url = new URL(
							"http://www.boardgamegeek.com/xmlapi/boardgame/"
									+ gameId + "&stats=1");

					// create a new SAX parser and get an XML reader from it
					SAXParser saxParser = SAXParserFactory.newInstance()
							.newSAXParser();
					XMLReader xmlReader = saxParser.getXMLReader();

					// set the XML reader's content handler and parse the XML
					BoardGameHandler boardGameHandler = new BoardGameHandler();
					xmlReader.setContentHandler(boardGameHandler);
					xmlReader.parse(new InputSource(url.openStream()));

					// get the parsed data as an object
					boardGame = boardGameHandler.getBoardGame();

					// get the image as a drawable, since that takes a while
					if (imageLoad) {
						thumbnail_drawable = getImage(boardGame
								.getThumbnailUrl());
					}
				} catch (Exception e) {
					Log.d(DEBUG_TAG, "Exception", e);
				}
				handler.post(updateResults);
			}
		}.start();
	}

	// override progress dialog
	@Override
	protected Dialog onCreateDialog(int id) {
		if (id == ID_DIALOG_SEARCHING) {
			Log.d(DEBUG_TAG, "ID_DIALOG_SEARCHING - Created");
			ProgressDialog dialog = new ProgressDialog(this);
			dialog.setTitle(R.string.dialog_working_title);
			dialog.setMessage(getResources().getString(
					R.string.dialog_working_message));
			dialog.setIndeterminate(true);
			dialog.setCancelable(true);
			return dialog;
		}

		return super.onCreateDialog(id);
	}

	// remove dialog boxes
	protected void removeDialogs() {
		try {
			removeDialog(ID_DIALOG_SEARCHING);
			Log.d(DEBUG_TAG, "ID_DIALOG_SEARCHING - Removed");
		} catch (Exception e) {
			Log.d(DEBUG_TAG, "ID_DIALOG_SEARCHING - Remove Failed", e);
		}
	}

	// get results from handler
	final Runnable updateResults = new Runnable() {
		public void run() {
			updateUI();
		}
	};

	// updates UI after running progress dialog
	private void updateUI() {
		// call the XML layout
		this.setContentView(R.layout.viewboardgame);

		// setup tabs
		tabHost = getTabHost();
		tabHost.addTab(tabHost.newTabSpec("tabMain").setIndicator(
				getResources().getString(R.string.main_tab_title)).setContent(
				R.id.mainTab));
		tabHost.addTab(tabHost.newTabSpec("tabStats").setIndicator(
				getResources().getString(R.string.stats_tab_title)).setContent(
				R.id.statsTab));
		tabHost.addTab(tabHost.newTabSpec("tabExtra").setIndicator(
				getResources().getString(R.string.extra_tab_title)).setContent(
				R.id.extraTab));

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
		ImageView thumbnail = (ImageView) findViewById(R.id.thumbnail);
		TextView information = (TextView) findViewById(R.id.information);
		TextView description = (TextView) findViewById(R.id.description);
		Drawable wholestar = getResources().getDrawable(R.drawable.star_yellow);
		Drawable halfstar = getResources().getDrawable(
				R.drawable.star_yellowhalf);
		Drawable nostar = getResources().getDrawable(R.drawable.star_white);

		// get the game information from the object
		String gameRank;
		if (boardGame.getRank() == 0) {
			gameRank = String.format(getResources().getString(R.string.rank),
					getResources().getString(R.string.not_available));
		} else {
			gameRank = String.format(getResources().getString(R.string.rank),
					boardGame.getRank());
		}
		String gameRating = getResources().getString(R.string.user_rating)
				+ ": "
				+ new DecimalFormat("#0.00").format(boardGame.getAverage())
				+ " / 10 (" + boardGame.getUsersRated() + " Ratings)";
		String gameInfo = boardGame.getGameInfo();
		String gameDescription = boardGame.getDescription();

		// display information
		title.setText(boardGame.getName());
		rank.setText(gameRank);
		if (imageLoad) {
			if (thumbnail_drawable != null
					&& !boardGame.getThumbnailUrl().equals("")) {
				thumbnail.setImageDrawable(thumbnail_drawable);
			} else {
				thumbnail.setImageDrawable(getResources().getDrawable(
						R.drawable.noimage));
			}
		}
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

		// statistics
		DecimalFormat statFormat = new DecimalFormat("#0.000");

		// ratings
		((TextView) findViewById(R.id.statsName)).setText(boardGame.getName());
		TextView r = (TextView) findViewById(R.id.statsRank);
		if (boardGame.getRank() == 0) {
			r.setText(String.format(getResources().getString(R.string.rank),
					getResources().getString(R.string.not_available)));
		} else {
			r.setText(String.format(getResources().getString(R.string.rank),
					boardGame.getRank()));
		}
		((TextView) findViewById(R.id.statsRatingCount)).setText(String.format(
				getResources().getString(R.string.rating_count), boardGame
						.getUsersRated()));
		((TextView) findViewById(R.id.averageText)).setText(String.format(
				getResources().getString(R.string.average), statFormat
						.format(boardGame.getAverage())));
		setMeterView(R.id.averageMeter, (float) boardGame.getAverage());
		((TextView) findViewById(R.id.bayesText)).setText(String.format(
				getResources().getString(R.string.bayes_average), statFormat
						.format(boardGame.getBayesAverage())));
		setMeterView(R.id.bayesMeter, (float) boardGame.getBayesAverage());
		((TextView) findViewById(R.id.medianText)).setText(String.format(
				getResources().getString(R.string.median), statFormat
						.format(boardGame.getMedian())));
		setMeterView(R.id.medianMeter, (float) boardGame.getMedian());
		((TextView) findViewById(R.id.stdDevText)).setText(String.format(
				getResources().getString(R.string.stdDev), statFormat
						.format(boardGame.getStandardDeviation())));
		setMeterView(R.id.stdDevMeter,
				(float) boardGame.getStandardDeviation(), 5);

		// weight
		((TextView) findViewById(R.id.statsWeightCount)).setText(String.format(
				getResources().getString(R.string.weight_count), boardGame
						.getWeightCount()));
		setMeterView(R.id.weightMeter, (float) boardGame.getAverageWeight(), 5);
		((TextView) findViewById(R.id.weightText)).setText(String.format(
				getResources().getString(R.string.average), boardGame
						.getAverageWeight()));

		// users
		int max = Math
				.max(boardGame.getUsersRated(), boardGame.getOwnedCount());
		max = Math.max(max, boardGame.getTradingCount());
		max = Math.max(max, boardGame.getWantingCount());
		max = Math.max(max, boardGame.getWishingCount());
		max = Math.max(max, boardGame.getWeightCount());
		((TextView) findViewById(R.id.usersCount)).setText(String.format(
				getResources().getString(R.string.user_total), max));
		setMeterView(R.id.owningMeter, (float) boardGame.getOwnedCount(), max);
		((TextView) findViewById(R.id.owningText)).setText(String.format(
				getResources().getString(R.string.owning_meter_text), boardGame
						.getOwnedCount()));
		setMeterView(R.id.ratingMeter, (float) boardGame.getUsersRated(), max);
		((TextView) findViewById(R.id.ratingText)).setText(String.format(
				getResources().getString(R.string.rating_meter_text), boardGame
						.getUsersRated()));
		setMeterView(R.id.tradingMeter, (float) boardGame.getTradingCount(),
				max);
		((TextView) findViewById(R.id.tradingText)).setText(String.format(
				getResources().getString(R.string.trading_meter_text),
				boardGame.getTradingCount()));
		setMeterView(R.id.wantingMeter, (float) boardGame.getWantingCount(),
				max);
		((TextView) findViewById(R.id.wantingText)).setText(String.format(
				getResources().getString(R.string.wanting_meter_text),
				boardGame.getWantingCount()));
		setMeterView(R.id.wishingMeter, (float) boardGame.getWishingCount(),
				max);
		((TextView) findViewById(R.id.wishingText)).setText(String.format(
				getResources().getString(R.string.wishing_meter_text),
				boardGame.getWishingCount()));
		//TODO: adds weights and comments?

		// extra
		TextView extraView = (TextView) findViewById(R.id.extra);
		StringBuilder extra = new StringBuilder();
		extra.append("Designers:");
		for (String designer : boardGame.getDesignerNames()) {
			extra.append("\n").append(designer);
		}
		extra.append("\n\nArtists:");
		for (String artist : boardGame.getArtistNames()) {
			extra.append("\n").append(artist);
		}
		extra.append("\n\nPublishers:");
		for (String publisher : boardGame.getPublisherNames()) {
			extra.append("\n").append(publisher);
		}
		extra.append("\n\nCategories:");
		for (String category : boardGame.getCategoryNames()) {
			extra.append("\n").append(category);
		}
		extra.append("\n\nMechanics:");
		for (String mechanic : boardGame.getMechanicNames()) {
			extra.append("\n").append(mechanic);
		}
		extra.append("\n\nExpansions:");
		for (String expansion : boardGame.getExpansionNames()) {
			extra.append("\n").append(expansion);
		}
		extraView.setText(extra.toString());

		tabHost.setCurrentTab(0);

		// remove progress dialog (if any)
		removeDialogs();

		viewLoaded = true;
	}

	private Drawable getImage(String url) {
		try {
			// connect to URL and open input stream
			URL imageURL = new URL(url);
			InputStream inputStream = (InputStream) imageURL.getContent();
			BufferedInputStream bufferedInputStream = new BufferedInputStream(
					inputStream, IO_BUFFER_SIZE);

			// open output stream and copy from input stream
			// this is to workaround a persistent "jpeg error 91" bug
			// solution per
			// http://groups.google.com/group/android-developers/browse_thread/thread/4ed17d7e48899b26/a15129024bb845bf?show_docid=a15129024bb845bf
			final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(
					outputStream, IO_BUFFER_SIZE);
			copy(bufferedInputStream, bufferedOutputStream);
			bufferedOutputStream.flush();

			// get bitmap and convert to drawable
			// if we didn't have to deal with "jpeg error 91" I think we'd only
			// need one line:
			// Drawable thumbnail = Drawable.createFromStream(inputstream,
			// "src");
			final byte[] data = outputStream.toByteArray();
			Bitmap thumbnail_bmp = BitmapFactory.decodeByteArray(data, 0,
					data.length);
			BitmapDrawable thumbnail_drawable = new BitmapDrawable(
					thumbnail_bmp);

			// close input stream
			bufferedInputStream.close();
			inputStream.close();

			// return drawable
			return thumbnail_drawable;
		} catch (MalformedURLException e) {
			Log.d(DEBUG_TAG, "MalformedURLException", e);
			return null;
		} catch (IOException e) {
			Log.d(DEBUG_TAG, "IOException", e);
			return null;
		}
	}

	private static void copy(InputStream in, OutputStream out)
			throws IOException {
		byte[] b = new byte[IO_BUFFER_SIZE];
		int read;
		while ((read = in.read(b)) != -1) {
			out.write(b, 0, read);
		}
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

	private void setMeterView(int meterId, float percentage) {
		setMeterView(meterId, percentage, 10);
	}

	private void setMeterView(int meterId, float percentage, float max) {
		try {
			percentage = Math.max(0, percentage);
			percentage = Math.min(max, percentage);
			TextView meter = (TextView) findViewById(meterId);
			LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) meter
					.getLayoutParams();
			params.weight = (float) (percentage / (max - percentage));
			meter.setLayoutParams(params);
		} catch (Throwable t) {
			Log.d(DEBUG_TAG, "Throwable", t);
		}
	}
}