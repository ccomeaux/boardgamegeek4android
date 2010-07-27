package com.boardgamegeek;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;

import com.boardgamegeek.BoardGameGeekData.*;

import android.app.SearchManager;
import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.text.TextUtils;
import android.util.Log;

public class BoardGameGeekProvider extends ContentProvider {

	private static final String LOG_TAG = "BoardGameGeek.Provider";

	private static final String DATABASE_NAME = "boardgamegeek.db";
	private static final int DATABASE_VERSION = 6;
	private static final String DESIGNER_TABLE = "designer";
	private static final String ARTIST_TABLE = "artist";
	private static final String PUBLISHER_TABLE = "publisher";
	private static final String CATEGORY_TABLE = "category";
	private static final String MECHANIC_TABLE = "mechanic";
	private static final String BOARDGAME_TABLE = "boardgame";
	private static final String BOARDGAMEDESIGNER_TABLE = "boardgame_designer";
	private static final String BOARDGAMEARTIST_TABLE = "boardgame_artist";
	private static final String BOARDGAMEPUBLISHER_TABLE = "boardgame_publisher";
	private static final String BOARDGAMECATEGORY_TABLE = "boardgame_category";
	private static final String BOARDGAMEMECHANIC_TABLE = "boardgame_mechanic";
	private static final String BOARDGAMEEXPANSION_TABLE = "boardgame_expansion";
	private static final String BOARDGAMEPOLL_TABLE = "boardgame_poll";
	private static final String BOARDGAMEPOLLRESULTS_TABLE = "boardgame_results";
	private static final String BOARDGAMEPOLLRESULT_TABLE = "boardgame_result";

	private static final int DESIGNERS = 1;
	private static final int DESIGNER_ID = 2;
	private static final int ARTISTS = 3;
	private static final int ARTIST_ID = 4;
	private static final int PUBLISHERS = 5;
	private static final int PUBLISHER_ID = 6;
	private static final int BOARDGAMES = 7;
	private static final int BOARDGAME_ID = 8;
	private static final int BOARDGAME_DESIGNERS = 9;
	private static final int BOARDGAME_DESIGNER_ID = 10;
	private static final int BOARDGAME_ARTISTS = 11;
	private static final int BOARDGAME_ARTIST_ID = 12;
	private static final int BOARDGAME_PUBLISHERS = 13;
	private static final int BOARDGAME_PUBLISHER_ID = 14;
	private static final int BOARDGAME_CATEGORIES = 15;
	private static final int BOARDGAME_CATEGORY_ID = 16;
	private static final int BOARDGAME_MECHANICS = 17;
	private static final int BOARDGAME_MECHANIC_ID = 18;
	private static final int BOARDGAME_EXPANSIONS = 19;
	private static final int BOARDGAME_EXPANSION_ID = 20;
	private static final int CATEGORIES = 21;
	private static final int CATEGORY_ID = 22;
	private static final int MECHANICS = 23;
	private static final int MECHANIC_ID = 24;
	private static final int BOARDGAME_POLLS = 25;
	private static final int BOARDGAME_POLL_ID = 26;
	private static final int BOARDGAME_POLL_RESULTS = 27;
	private static final int BOARDGAME_POLL_RESULTS_ID = 28;
	private static final int BOARDGAME_POLL_RESULT = 29;
	private static final int BOARDGAME_POLL_RESULT_ID = 30;
	private static final int SEARCH_SUGGEST = 31;
	private static final int SHORTCUT_REFRESH = 32;
	private static final int THUMBNAILS = 33;
	private static final int THUMBNAIL_ID = 34;

	private DatabaseHelper dbHelper;
	private static final UriMatcher uriMatcher;
	private static HashMap<String, String> designersProjectionMap;
	private static HashMap<String, String> artistsProjectionMap;
	private static HashMap<String, String> publishersProjectionMap;
	private static HashMap<String, String> categoriesProjectionMap;
	private static HashMap<String, String> mechanicsProjectionMap;
	private static HashMap<String, String> boardgamesProjectionMap;
	private static HashMap<String, String> boardgameDesignersProjectionMap;
	private static HashMap<String, String> boardgameArtistsProjectionMap;
	private static HashMap<String, String> boardgamePublishersProjectionMap;
	private static HashMap<String, String> boardgameCategoriesProjectionMap;
	private static HashMap<String, String> boardgameMechanicsProjectionMap;
	private static HashMap<String, String> boardgameExpansionsProjectionMap;
	private static HashMap<String, String> boardgamePollsProjectionMap;
	private static HashMap<String, String> boardgamePollResultsProjectionMap;
	private static HashMap<String, String> boardgamePollResultProjectionMap;
	private static HashMap<String, String> suggestionProjectionMap;
	private static HashMap<String, String> thumbnailProjectionMap;

	static {
		uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		uriMatcher.addURI(BoardGameGeekData.AUTHORITY, "designers", DESIGNERS);
		uriMatcher.addURI(BoardGameGeekData.AUTHORITY, "designers/#", DESIGNER_ID);
		uriMatcher.addURI(BoardGameGeekData.AUTHORITY, "artists", ARTISTS);
		uriMatcher.addURI(BoardGameGeekData.AUTHORITY, "artists/#", ARTIST_ID);
		uriMatcher.addURI(BoardGameGeekData.AUTHORITY, "publishers", PUBLISHERS);
		uriMatcher.addURI(BoardGameGeekData.AUTHORITY, "publishers/#", PUBLISHER_ID);
		uriMatcher.addURI(BoardGameGeekData.AUTHORITY, "categories", CATEGORIES);
		uriMatcher.addURI(BoardGameGeekData.AUTHORITY, "categories/#", CATEGORY_ID);
		uriMatcher.addURI(BoardGameGeekData.AUTHORITY, "mechanics", MECHANICS);
		uriMatcher.addURI(BoardGameGeekData.AUTHORITY, "mechanics/#", MECHANIC_ID);
		uriMatcher.addURI(BoardGameGeekData.AUTHORITY, "boardgames", BOARDGAMES);
		uriMatcher.addURI(BoardGameGeekData.AUTHORITY, "boardgames/#", BOARDGAME_ID);
		uriMatcher.addURI(BoardGameGeekData.AUTHORITY, "boardgamedesigners", BOARDGAME_DESIGNERS);
		uriMatcher.addURI(BoardGameGeekData.AUTHORITY, "boardgamedesigners/#", BOARDGAME_DESIGNER_ID);
		uriMatcher.addURI(BoardGameGeekData.AUTHORITY, "boardgameartists", BOARDGAME_ARTISTS);
		uriMatcher.addURI(BoardGameGeekData.AUTHORITY, "boardgameartists/#", BOARDGAME_ARTIST_ID);
		uriMatcher.addURI(BoardGameGeekData.AUTHORITY, "boardgamepublishers", BOARDGAME_PUBLISHERS);
		uriMatcher.addURI(BoardGameGeekData.AUTHORITY, "boardgamepublishers/#", BOARDGAME_PUBLISHER_ID);
		uriMatcher.addURI(BoardGameGeekData.AUTHORITY, "boardgamecategories", BOARDGAME_CATEGORIES);
		uriMatcher.addURI(BoardGameGeekData.AUTHORITY, "boardgamecategories/#", BOARDGAME_CATEGORY_ID);
		uriMatcher.addURI(BoardGameGeekData.AUTHORITY, "boardgamemechanics", BOARDGAME_MECHANICS);
		uriMatcher.addURI(BoardGameGeekData.AUTHORITY, "boardgamemechanics/#", BOARDGAME_MECHANIC_ID);
		uriMatcher.addURI(BoardGameGeekData.AUTHORITY, "boardgameexpansions", BOARDGAME_EXPANSIONS);
		uriMatcher.addURI(BoardGameGeekData.AUTHORITY, "boardgameexpansions/#", BOARDGAME_EXPANSION_ID);
		uriMatcher.addURI(BoardGameGeekData.AUTHORITY, "boardgamepolls", BOARDGAME_POLLS);
		uriMatcher.addURI(BoardGameGeekData.AUTHORITY, "boardgamepolls/#", BOARDGAME_POLL_ID);
		uriMatcher.addURI(BoardGameGeekData.AUTHORITY, "boardgamepollresults", BOARDGAME_POLL_RESULTS);
		uriMatcher.addURI(BoardGameGeekData.AUTHORITY, "boardgamepollresults/#", BOARDGAME_POLL_RESULTS_ID);
		uriMatcher.addURI(BoardGameGeekData.AUTHORITY, "boardgamepollresult", BOARDGAME_POLL_RESULT);
		uriMatcher.addURI(BoardGameGeekData.AUTHORITY, "boardgamepollresult/#", BOARDGAME_POLL_RESULT_ID);
		uriMatcher.addURI(BoardGameGeekData.AUTHORITY, SearchManager.SUGGEST_URI_PATH_QUERY, SEARCH_SUGGEST);
		uriMatcher.addURI(BoardGameGeekData.AUTHORITY, SearchManager.SUGGEST_URI_PATH_QUERY + "/*",
			SEARCH_SUGGEST);
		uriMatcher.addURI(BoardGameGeekData.AUTHORITY, SearchManager.SUGGEST_URI_PATH_SHORTCUT,
			SHORTCUT_REFRESH);
		uriMatcher.addURI(BoardGameGeekData.AUTHORITY, SearchManager.SUGGEST_URI_PATH_SHORTCUT + "/#",
			SHORTCUT_REFRESH);
		uriMatcher.addURI(BoardGameGeekData.AUTHORITY, "thumbnails", THUMBNAILS);
		uriMatcher.addURI(BoardGameGeekData.AUTHORITY, "thumbnails/#", THUMBNAIL_ID);

		designersProjectionMap = new HashMap<String, String>();
		designersProjectionMap.put(Designers._ID, Designers._ID);
		designersProjectionMap.put(Designers.NAME, Designers.NAME);
		designersProjectionMap.put(Designers.DESCRIPTION, Designers.DESCRIPTION);
		designersProjectionMap.put(Designers.UPDATED_DATE, Designers.UPDATED_DATE);

		artistsProjectionMap = new HashMap<String, String>();
		artistsProjectionMap.put(Artists._ID, Artists._ID);
		artistsProjectionMap.put(Artists.NAME, Artists.NAME);
		artistsProjectionMap.put(Artists.DESCRIPTION, Artists.DESCRIPTION);
		artistsProjectionMap.put(Artists.UPDATED_DATE, Artists.UPDATED_DATE);

		publishersProjectionMap = new HashMap<String, String>();
		publishersProjectionMap.put(Publishers._ID, Publishers._ID);
		publishersProjectionMap.put(Publishers.NAME, Publishers.NAME);
		publishersProjectionMap.put(Publishers.DESCRIPTION, Publishers.DESCRIPTION);
		publishersProjectionMap.put(Publishers.UPDATED_DATE, Publishers.UPDATED_DATE);

		categoriesProjectionMap = new HashMap<String, String>();
		categoriesProjectionMap.put(Categories._ID, Categories._ID);
		categoriesProjectionMap.put(Categories.NAME, Categories.NAME);

		mechanicsProjectionMap = new HashMap<String, String>();
		mechanicsProjectionMap.put(Mechanics._ID, Mechanics._ID);
		mechanicsProjectionMap.put(Mechanics.NAME, Mechanics.NAME);

		boardgamesProjectionMap = new HashMap<String, String>();
		boardgamesProjectionMap.put(BoardGames._ID, BoardGames._ID);
		boardgamesProjectionMap.put(BoardGames.NAME, BoardGames.NAME);
		boardgamesProjectionMap.put(BoardGames.SORT_INDEX, BoardGames.SORT_INDEX);
		boardgamesProjectionMap.put(BoardGames.SORT_NAME, "(CASE WHEN " + BoardGames.SORT_NAME
			+ " IS NULL THEN " + BoardGames.NAME + " ELSE " + BoardGames.SORT_NAME + " END) AS "
			+ BoardGames.SORT_NAME);
		boardgamesProjectionMap.put(BoardGames.YEAR, BoardGames.YEAR);
		boardgamesProjectionMap.put(BoardGames.MIN_PLAYERS, BoardGames.MIN_PLAYERS);
		boardgamesProjectionMap.put(BoardGames.MAX_PLAYERS, BoardGames.MAX_PLAYERS);
		boardgamesProjectionMap.put(BoardGames.PLAYING_TIME, BoardGames.PLAYING_TIME);
		boardgamesProjectionMap.put(BoardGames.AGE, BoardGames.AGE);
		boardgamesProjectionMap.put(BoardGames.DESCRIPTION, BoardGames.DESCRIPTION);
		boardgamesProjectionMap.put(BoardGames.THUMBNAIL_URL, BoardGames.THUMBNAIL_URL);
		boardgamesProjectionMap.put(BoardGames.THUMBNAIL_ID, BoardGames.THUMBNAIL_ID);
		boardgamesProjectionMap.put(BoardGames.RATING_COUNT, BoardGames.RATING_COUNT);
		boardgamesProjectionMap.put(BoardGames.AVERAGE, BoardGames.AVERAGE);
		boardgamesProjectionMap.put(BoardGames.BAYES_AVERAGE, BoardGames.BAYES_AVERAGE);
		boardgamesProjectionMap.put(BoardGames.RANK, BoardGames.RANK);
		boardgamesProjectionMap.put(BoardGames.RANK_ABSTRACT, BoardGames.RANK_ABSTRACT);
		boardgamesProjectionMap.put(BoardGames.RANK_CCG, BoardGames.RANK_CCG);
		boardgamesProjectionMap.put(BoardGames.RANK_FAMILY, BoardGames.RANK_FAMILY);
		boardgamesProjectionMap.put(BoardGames.RANK_KIDS, BoardGames.RANK_KIDS);
		boardgamesProjectionMap.put(BoardGames.RANK_PARTY, BoardGames.RANK_PARTY);
		boardgamesProjectionMap.put(BoardGames.RANK_STRATEGY, BoardGames.RANK_STRATEGY);
		boardgamesProjectionMap.put(BoardGames.RANK_THEMATIC, BoardGames.RANK_THEMATIC);
		boardgamesProjectionMap.put(BoardGames.RANK_WAR, BoardGames.RANK_WAR);
		boardgamesProjectionMap.put(BoardGames.STANDARD_DEVIATION, BoardGames.STANDARD_DEVIATION);
		boardgamesProjectionMap.put(BoardGames.MEDIAN, BoardGames.MEDIAN);
		boardgamesProjectionMap.put(BoardGames.OWNED_COUNT, BoardGames.OWNED_COUNT);
		boardgamesProjectionMap.put(BoardGames.TRADING_COUNT, BoardGames.TRADING_COUNT);
		boardgamesProjectionMap.put(BoardGames.WANTING_COUNT, BoardGames.WANTING_COUNT);
		boardgamesProjectionMap.put(BoardGames.WISHING_COUNT, BoardGames.WISHING_COUNT);
		boardgamesProjectionMap.put(BoardGames.COMMENT_COUNT, BoardGames.COMMENT_COUNT);
		boardgamesProjectionMap.put(BoardGames.WEIGHT_COUNT, BoardGames.WEIGHT_COUNT);
		boardgamesProjectionMap.put(BoardGames.AVERAGE_WEIGHT, BoardGames.AVERAGE_WEIGHT);
		boardgamesProjectionMap.put(BoardGames.UPDATED_DATE, BoardGames.UPDATED_DATE);

		boardgameDesignersProjectionMap = new HashMap<String, String>();
		boardgameDesignersProjectionMap.put(BoardGameDesigners._ID, BOARDGAMEDESIGNER_TABLE + "."
			+ BoardGameDesigners._ID + " AS " + BoardGameDesigners._ID);
		boardgameDesignersProjectionMap.put(BoardGameDesigners.BOARDGAME_ID, BoardGameDesigners.BOARDGAME_ID);
		boardgameDesignersProjectionMap.put(BoardGameDesigners.DESIGNER_ID, BoardGameDesigners.DESIGNER_ID);
		boardgameDesignersProjectionMap.put(BoardGameDesigners.DESIGNER_NAME, DESIGNER_TABLE + "."
			+ Designers.NAME + " AS " + BoardGameDesigners.DESIGNER_NAME);

		boardgameArtistsProjectionMap = new HashMap<String, String>();
		boardgameArtistsProjectionMap.put(BoardGameArtists._ID, BOARDGAMEARTIST_TABLE + "."
			+ BoardGameArtists._ID + " AS " + BoardGameArtists._ID);
		boardgameArtistsProjectionMap.put(BoardGameArtists.BOARDGAME_ID, BoardGameArtists.BOARDGAME_ID);
		boardgameArtistsProjectionMap.put(BoardGameArtists.ARTIST_ID, BoardGameArtists.ARTIST_ID);
		boardgameArtistsProjectionMap.put(BoardGameArtists.ARTIST_NAME, ARTIST_TABLE + "." + Artists.NAME
			+ " AS " + BoardGameArtists.ARTIST_NAME);

		boardgamePublishersProjectionMap = new HashMap<String, String>();
		boardgamePublishersProjectionMap.put(BoardGamePublishers._ID, BOARDGAMEPUBLISHER_TABLE + "."
			+ BoardGamePublishers._ID + " AS " + BoardGamePublishers._ID);
		boardgamePublishersProjectionMap.put(BoardGamePublishers.BOARDGAME_ID,
			BoardGamePublishers.BOARDGAME_ID);
		boardgamePublishersProjectionMap.put(BoardGamePublishers.PUBLISHER_ID,
			BoardGamePublishers.PUBLISHER_ID);
		boardgamePublishersProjectionMap.put(BoardGamePublishers.PUBLISHER_NAME, PUBLISHER_TABLE + "."
			+ Publishers.NAME + " AS " + BoardGamePublishers.PUBLISHER_NAME);

		boardgameCategoriesProjectionMap = new HashMap<String, String>();
		boardgameCategoriesProjectionMap.put(BoardGameCategories._ID, BOARDGAMECATEGORY_TABLE + "."
			+ BoardGameCategories._ID + " AS " + BoardGameCategories._ID);
		boardgameCategoriesProjectionMap.put(BoardGameCategories.BOARDGAME_ID,
			BoardGameCategories.BOARDGAME_ID);
		boardgameCategoriesProjectionMap
			.put(BoardGameCategories.CATEGORY_ID, BoardGameCategories.CATEGORY_ID);
		boardgameCategoriesProjectionMap.put(BoardGameCategories.CATEGORY_NAME, CATEGORY_TABLE + "."
			+ Categories.NAME + " AS " + BoardGameCategories.CATEGORY_NAME);

		boardgameMechanicsProjectionMap = new HashMap<String, String>();
		boardgameMechanicsProjectionMap.put(BoardGameMechanics._ID, BOARDGAMEMECHANIC_TABLE + "."
			+ BoardGameMechanics._ID + " AS " + BoardGameMechanics._ID);
		boardgameMechanicsProjectionMap.put(BoardGameMechanics.BOARDGAME_ID, BoardGameMechanics.BOARDGAME_ID);
		boardgameMechanicsProjectionMap.put(BoardGameMechanics.MECHANIC_ID, BoardGameMechanics.MECHANIC_ID);
		boardgameMechanicsProjectionMap.put(BoardGameMechanics.MECHANIC_NAME, MECHANIC_TABLE + "."
			+ Mechanics.NAME + " AS " + BoardGameMechanics.MECHANIC_NAME);

		boardgameExpansionsProjectionMap = new HashMap<String, String>();
		boardgameExpansionsProjectionMap.put(BoardGameExpansions._ID, BOARDGAMEEXPANSION_TABLE + "."
			+ BoardGameExpansions._ID + " AS " + BoardGameExpansions._ID);
		boardgameExpansionsProjectionMap.put(BoardGameExpansions.BOARDGAME_ID,
			BoardGameExpansions.BOARDGAME_ID);
		boardgameExpansionsProjectionMap.put(BoardGameExpansions.EXPANSION_ID,
			BoardGameExpansions.EXPANSION_ID);
		boardgameExpansionsProjectionMap.put(BoardGameExpansions.EXPANSION_NAME, BOARDGAME_TABLE + "."
			+ BoardGames.NAME + " AS " + BoardGameExpansions.EXPANSION_NAME);

		boardgamePollsProjectionMap = new HashMap<String, String>();
		boardgamePollsProjectionMap.put(BoardGamePolls._ID, BoardGamePolls._ID);
		boardgamePollsProjectionMap.put(BoardGamePolls.BOARDGAME_ID, BoardGamePolls.BOARDGAME_ID);
		boardgamePollsProjectionMap.put(BoardGamePolls.NAME, BoardGamePolls.NAME);
		boardgamePollsProjectionMap.put(BoardGamePolls.TITLE, BoardGamePolls.TITLE);
		boardgamePollsProjectionMap.put(BoardGamePolls.VOTES, BoardGamePolls.VOTES);

		boardgamePollResultsProjectionMap = new HashMap<String, String>();
		boardgamePollResultsProjectionMap.put(BoardGamePollResults._ID, BoardGamePollResults._ID);
		boardgamePollResultsProjectionMap.put(BoardGamePollResults.POLL_ID, BoardGamePollResults.POLL_ID);
		boardgamePollResultsProjectionMap.put(BoardGamePollResults.PLAYERS, BoardGamePollResults.PLAYERS);

		boardgamePollResultProjectionMap = new HashMap<String, String>();
		boardgamePollResultProjectionMap.put(BoardGamePollResult._ID, BoardGamePollResult._ID);
		boardgamePollResultProjectionMap.put(BoardGamePollResult.POLLRESULTS_ID,
			BoardGamePollResult.POLLRESULTS_ID);
		boardgamePollResultProjectionMap.put(BoardGamePollResult.LEVEL, BoardGamePollResult.LEVEL);
		boardgamePollResultProjectionMap.put(BoardGamePollResult.VALUE, BoardGamePollResult.VALUE);
		boardgamePollResultProjectionMap.put(BoardGamePollResult.VOTES, BoardGamePollResult.VOTES);

		suggestionProjectionMap = new HashMap<String, String>();
		suggestionProjectionMap.put(BoardGames._ID, BoardGames._ID);
		suggestionProjectionMap.put(SearchManager.SUGGEST_COLUMN_TEXT_1, BoardGames.NAME + " AS "
			+ SearchManager.SUGGEST_COLUMN_TEXT_1);
		suggestionProjectionMap.put(SearchManager.SUGGEST_COLUMN_TEXT_2, BoardGames.YEAR + " AS "
			+ SearchManager.SUGGEST_COLUMN_TEXT_2);
		suggestionProjectionMap.put(SearchManager.SUGGEST_COLUMN_INTENT_DATA_ID, BOARDGAME_TABLE + "."
			+ BoardGames._ID + " AS " + SearchManager.SUGGEST_COLUMN_INTENT_DATA_ID);
		suggestionProjectionMap.put(SearchManager.SUGGEST_COLUMN_SHORTCUT_ID, BOARDGAME_TABLE + "."
			+ BoardGames._ID + " AS " + SearchManager.SUGGEST_COLUMN_SHORTCUT_ID);
		suggestionProjectionMap.put(SearchManager.SUGGEST_COLUMN_ICON_1, "0 AS "
			+ SearchManager.SUGGEST_COLUMN_ICON_1); // BGG app icon
		suggestionProjectionMap.put(SearchManager.SUGGEST_COLUMN_ICON_2, "'" + Thumbnails.CONTENT_URI
			+ "/' || " + BOARDGAME_TABLE + "." + BoardGames.THUMBNAIL_ID + " AS "
			+ SearchManager.SUGGEST_COLUMN_ICON_2);
		suggestionProjectionMap.put(BoardGames.SORT_NAME, "(CASE WHEN " + BoardGames.SORT_NAME
			+ " IS NULL THEN " + BoardGames.NAME + " ELSE " + BoardGames.SORT_NAME + " END) AS "
			+ BoardGames.SORT_NAME); // for sorting

		thumbnailProjectionMap = new HashMap<String, String>();
		thumbnailProjectionMap.put(Thumbnails._ID, Thumbnails._ID);
		thumbnailProjectionMap.put(Thumbnails.PATH, Thumbnails.PATH);
	}

	@Override
	public boolean onCreate() {
		dbHelper = new DatabaseHelper(getContext());
		return true;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
		String sortOrder) {

		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
		String defaultOrderBy = null;

		switch (uriMatcher.match(uri)) {
		case DESIGNERS:
			qb.setTables(DESIGNER_TABLE);
			qb.setProjectionMap(designersProjectionMap);
			defaultOrderBy = Designers.DEFAULT_SORT_ORDER;
			break;
		case DESIGNER_ID:
			qb.setTables(DESIGNER_TABLE);
			qb.setProjectionMap(designersProjectionMap);
			qb.appendWhere(Designers._ID + "=" + uri.getPathSegments().get(1));
			defaultOrderBy = Designers.DEFAULT_SORT_ORDER;
			break;
		case ARTISTS:
			qb.setTables(ARTIST_TABLE);
			qb.setProjectionMap(artistsProjectionMap);
			defaultOrderBy = Artists.DEFAULT_SORT_ORDER;
			break;
		case ARTIST_ID:
			qb.setTables(ARTIST_TABLE);
			qb.setProjectionMap(artistsProjectionMap);
			qb.appendWhere(Artists._ID + "=" + uri.getPathSegments().get(1));
			defaultOrderBy = Artists.DEFAULT_SORT_ORDER;
			break;
		case PUBLISHERS:
			qb.setTables(PUBLISHER_TABLE);
			qb.setProjectionMap(publishersProjectionMap);
			defaultOrderBy = Publishers.DEFAULT_SORT_ORDER;
			break;
		case PUBLISHER_ID:
			qb.setTables(PUBLISHER_TABLE);
			qb.setProjectionMap(publishersProjectionMap);
			qb.appendWhere(Publishers._ID + "=" + uri.getPathSegments().get(1));
			defaultOrderBy = Publishers.DEFAULT_SORT_ORDER;
			break;
		case CATEGORIES:
			qb.setTables(CATEGORY_TABLE);
			qb.setProjectionMap(categoriesProjectionMap);
			defaultOrderBy = Categories.DEFAULT_SORT_ORDER;
			break;
		case CATEGORY_ID:
			qb.setTables(CATEGORY_TABLE);
			qb.setProjectionMap(categoriesProjectionMap);
			qb.appendWhere(Categories._ID + "=" + uri.getPathSegments().get(1));
			defaultOrderBy = Categories.DEFAULT_SORT_ORDER;
			break;
		case MECHANICS:
			qb.setTables(MECHANIC_TABLE);
			qb.setProjectionMap(mechanicsProjectionMap);
			defaultOrderBy = Mechanics.DEFAULT_SORT_ORDER;
			break;
		case MECHANIC_ID:
			qb.setTables(MECHANIC_TABLE);
			qb.setProjectionMap(mechanicsProjectionMap);
			qb.appendWhere(Mechanics._ID + "=" + uri.getPathSegments().get(1));
			defaultOrderBy = Mechanics.DEFAULT_SORT_ORDER;
			break;
		case BOARDGAMES:
			qb.setTables(BOARDGAME_TABLE);
			qb.setProjectionMap(boardgamesProjectionMap);
			defaultOrderBy = BoardGames.DEFAULT_SORT_ORDER;
			break;
		case BOARDGAME_ID:
			qb.setTables(BOARDGAME_TABLE);
			qb.setProjectionMap(boardgamesProjectionMap);
			qb.appendWhere(BoardGames._ID + "=" + uri.getPathSegments().get(1));
			defaultOrderBy = BoardGames.DEFAULT_SORT_ORDER;
			break;
		case BOARDGAME_DESIGNERS:
			qb.setTables(BOARDGAMEDESIGNER_TABLE + "," + DESIGNER_TABLE);
			qb.setProjectionMap(boardgameDesignersProjectionMap);
			qb.appendWhere(BOARDGAMEDESIGNER_TABLE + "." + BoardGameDesigners.DESIGNER_ID + " = "
				+ DESIGNER_TABLE + "." + Designers._ID);
			defaultOrderBy = BoardGameDesigners.DEFAULT_SORT_ORDER;
			break;
		case BOARDGAME_ARTISTS:
			qb.setTables(BOARDGAMEARTIST_TABLE + "," + ARTIST_TABLE);
			qb.setProjectionMap(boardgameArtistsProjectionMap);
			qb.appendWhere(BOARDGAMEARTIST_TABLE + "." + BoardGameArtists.ARTIST_ID + " = " + ARTIST_TABLE
				+ "." + Artists._ID);
			defaultOrderBy = BoardGameArtists.DEFAULT_SORT_ORDER;
			break;
		case BOARDGAME_PUBLISHERS:
			qb.setTables(BOARDGAMEPUBLISHER_TABLE + "," + PUBLISHER_TABLE);
			qb.setProjectionMap(boardgamePublishersProjectionMap);
			qb.appendWhere(BOARDGAMEPUBLISHER_TABLE + "." + BoardGamePublishers.PUBLISHER_ID + " = "
				+ PUBLISHER_TABLE + "." + Publishers._ID);
			defaultOrderBy = BoardGamePublishers.DEFAULT_SORT_ORDER;
			break;
		case BOARDGAME_CATEGORIES:
			qb.setTables(BOARDGAMECATEGORY_TABLE + "," + CATEGORY_TABLE);
			qb.setProjectionMap(boardgameCategoriesProjectionMap);
			qb.appendWhere(BOARDGAMECATEGORY_TABLE + "." + BoardGameCategories.CATEGORY_ID + " = "
				+ CATEGORY_TABLE + "." + Categories._ID);
			defaultOrderBy = BoardGameCategories.DEFAULT_SORT_ORDER;
			break;
		case BOARDGAME_MECHANICS:
			qb.setTables(BOARDGAMEMECHANIC_TABLE + "," + MECHANIC_TABLE);
			qb.setProjectionMap(boardgameMechanicsProjectionMap);
			qb.appendWhere(BOARDGAMEMECHANIC_TABLE + "." + BoardGameMechanics.MECHANIC_ID + " = "
				+ MECHANIC_TABLE + "." + Mechanics._ID);
			defaultOrderBy = BoardGameMechanics.DEFAULT_SORT_ORDER;
			break;
		case BOARDGAME_EXPANSIONS:
			qb.setTables(BOARDGAMEEXPANSION_TABLE + "," + BOARDGAME_TABLE);
			qb.setProjectionMap(boardgameExpansionsProjectionMap);
			qb.appendWhere(BOARDGAMEEXPANSION_TABLE + "." + BoardGameExpansions.EXPANSION_ID + " = "
				+ BOARDGAME_TABLE + "." + BoardGames._ID);
			defaultOrderBy = BoardGameExpansions.DEFAULT_SORT_ORDER;
			break;
		case BOARDGAME_POLLS:
			qb.setTables(BOARDGAMEPOLL_TABLE);
			qb.setProjectionMap(boardgamePollsProjectionMap);
			defaultOrderBy = BoardGamePolls.DEFAULT_SORT_ORDER;
			break;
		case BOARDGAME_POLL_RESULTS:
			qb.setTables(BOARDGAMEPOLLRESULTS_TABLE);
			qb.setProjectionMap(boardgamePollResultsProjectionMap);
			defaultOrderBy = BoardGamePollResults.DEFAULT_SORT_ORDER;
			break;
		case BOARDGAME_POLL_RESULT:
			qb.setTables(BOARDGAMEPOLLRESULT_TABLE);
			qb.setProjectionMap(boardgamePollResultProjectionMap);
			defaultOrderBy = BoardGamePollResult.DEFAULT_SORT_ORDER;
			break;
		case SEARCH_SUGGEST:
			String query = null;
			if (uri.getPathSegments().size() > 1) {
				query = uri.getLastPathSegment().toLowerCase();
			}
			if (query == null) {
				return null;
			} else {
				query = Utility.querifyText(query);
				qb.setTables(BOARDGAME_TABLE);
				qb.setProjectionMap(suggestionProjectionMap);
				qb.appendWhere("(" + BOARDGAME_TABLE + "." + BoardGames.NAME + " like '" + query + "%' OR "
					+ BOARDGAME_TABLE + "." + BoardGames.NAME + " like '% " + query + "%') AND "
					+ BOARDGAME_TABLE + "." + BoardGames.UPDATED_DATE + " IS NOT NULL");
				defaultOrderBy = BoardGames.DEFAULT_SORT_ORDER;
			}
			break;
		case SHORTCUT_REFRESH:
			String shortcutId = null;
			if (uri.getPathSegments().size() > 1) {
				shortcutId = uri.getLastPathSegment();
			}
			if (TextUtils.isEmpty(shortcutId)) {
				return null;
			} else {
				qb.setTables(BOARDGAME_TABLE);
				qb.setProjectionMap(suggestionProjectionMap);
				qb.appendWhere(SearchManager.SUGGEST_COLUMN_SHORTCUT_ID + "=" + uri.getPathSegments().get(1));
			}
			break;
		case THUMBNAIL_ID:
			// TODO: honor projection map?
			MatrixCursor thumbnailCursor = new MatrixCursor(new String[] { Thumbnails._ID, Thumbnails.PATH });
			String thumbnailId = uri.getLastPathSegment();
			String fileName = DataHelper.getThumbnailPath(thumbnailId);
			if (!TextUtils.isEmpty(fileName)) {
				File file = new File(fileName);
				if (file.exists()) {
					thumbnailCursor.addRow(new Object[] { thumbnailId, fileName });
				}
			}
			return thumbnailCursor;
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}

		// If no sort order is specified use the default
		String orderBy;
		if (TextUtils.isEmpty(sortOrder)) {
			orderBy = defaultOrderBy;
		} else {
			orderBy = sortOrder;
		}

		// Get the database and run the query
		SQLiteDatabase db = dbHelper.getReadableDatabase();
		Cursor c = qb.query(db, projection, selection, selectionArgs, null, null, orderBy);

		// Tell the cursor what URI to watch, so it knows when its source data
		// changes
		c.setNotificationUri(getContext().getContentResolver(), uri);
		Log.d(LOG_TAG, "Queried URI " + uri);
		return c;
	}

	@Override
	public String getType(Uri uri) {
		switch (uriMatcher.match(uri)) {
		case DESIGNERS:
			return Designers.CONTENT_TYPE;
		case DESIGNER_ID:
			return Designers.CONTENT_ITEM_TYPE;
		case ARTISTS:
			return Artists.CONTENT_TYPE;
		case ARTIST_ID:
			return Artists.CONTENT_ITEM_TYPE;
		case PUBLISHERS:
			return Publishers.CONTENT_TYPE;
		case PUBLISHER_ID:
			return Publishers.CONTENT_ITEM_TYPE;
		case CATEGORIES:
			return Publishers.CONTENT_TYPE;
		case CATEGORY_ID:
			return Publishers.CONTENT_ITEM_TYPE;
		case MECHANICS:
			return Publishers.CONTENT_TYPE;
		case MECHANIC_ID:
			return Publishers.CONTENT_ITEM_TYPE;
		case BOARDGAMES:
			return BoardGames.CONTENT_TYPE;
		case BOARDGAME_ID:
			return BoardGames.CONTENT_ITEM_TYPE;
		case BOARDGAME_DESIGNERS:
			return BoardGameDesigners.CONTENT_TYPE;
		case BOARDGAME_DESIGNER_ID:
			return BoardGameDesigners.CONTENT_ITEM_TYPE;
		case BOARDGAME_ARTISTS:
			return BoardGameArtists.CONTENT_TYPE;
		case BOARDGAME_ARTIST_ID:
			return BoardGameArtists.CONTENT_ITEM_TYPE;
		case BOARDGAME_PUBLISHERS:
			return BoardGamePublishers.CONTENT_TYPE;
		case BOARDGAME_PUBLISHER_ID:
			return BoardGamePublishers.CONTENT_ITEM_TYPE;
		case BOARDGAME_CATEGORIES:
			return BoardGameCategories.CONTENT_TYPE;
		case BOARDGAME_CATEGORY_ID:
			return BoardGameCategories.CONTENT_ITEM_TYPE;
		case BOARDGAME_MECHANICS:
			return BoardGameMechanics.CONTENT_TYPE;
		case BOARDGAME_MECHANIC_ID:
			return BoardGameMechanics.CONTENT_ITEM_TYPE;
		case BOARDGAME_EXPANSIONS:
			return BoardGameExpansions.CONTENT_TYPE;
		case BOARDGAME_EXPANSION_ID:
			return BoardGameExpansions.CONTENT_ITEM_TYPE;
		case BOARDGAME_POLLS:
			return BoardGamePolls.CONTENT_TYPE;
		case BOARDGAME_POLL_ID:
			return BoardGamePolls.CONTENT_ITEM_TYPE;
		case BOARDGAME_POLL_RESULTS:
			return BoardGamePollResults.CONTENT_TYPE;
		case BOARDGAME_POLL_RESULTS_ID:
			return BoardGamePollResults.CONTENT_ITEM_TYPE;
		case BOARDGAME_POLL_RESULT:
			return BoardGamePollResult.CONTENT_TYPE;
		case BOARDGAME_POLL_RESULT_ID:
			return BoardGamePollResult.CONTENT_ITEM_TYPE;
		case SEARCH_SUGGEST:
			return SearchManager.SUGGEST_MIME_TYPE;
		case SHORTCUT_REFRESH:
			return SearchManager.SHORTCUT_MIME_TYPE;
		case THUMBNAIL_ID:
			return Thumbnails.CONTENT_ITEM_TYPE;
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {

		ContentValues newValues;
		if (values != null) {
			newValues = new ContentValues(values);
		} else {
			newValues = new ContentValues();
		}

		// Validate the requested URI
		switch (uriMatcher.match(uri)) {
		case DESIGNERS:
			return insertDesigner(uri, newValues);
		case ARTISTS:
			return insertArtist(uri, newValues);
		case PUBLISHERS:
			return insertPublisher(uri, newValues);
		case CATEGORIES:
			return insertCategory(uri, newValues);
		case MECHANICS:
			return insertMechanic(uri, newValues);
		case BOARDGAMES:
			return insertBoardGame(uri, newValues);
		case BOARDGAME_DESIGNERS:
			return insertBoardGameDesigner(uri, newValues);
		case BOARDGAME_ARTISTS:
			return insertBoardGameArtist(uri, newValues);
		case BOARDGAME_PUBLISHERS:
			return insertBoardGamePublisher(uri, newValues);
		case BOARDGAME_CATEGORIES:
			return insertBoardGameCategory(uri, newValues);
		case BOARDGAME_MECHANICS:
			return insertBoardGameMechanic(uri, newValues);
		case BOARDGAME_EXPANSIONS:
			return insertBoardGameExpansion(uri, newValues);
		case BOARDGAME_POLLS:
			return insertBoardGamePoll(uri, newValues);
		case BOARDGAME_POLL_RESULTS:
			return insertBoardGamePollResults(uri, newValues);
		case BOARDGAME_POLL_RESULT:
			return insertBoardGamePollResult(uri, newValues);
		case THUMBNAILS:
			return insertThumbnail(uri, newValues);
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}
	}

	private Uri insertThumbnail(Uri uri, ContentValues values) {
		// Verify column data
		if (values.containsKey(Thumbnails._ID) == false) {
			throw new SQLException("Can't insert without an ID.");
		}
		if (values.containsKey(Thumbnails.DATA) == false) {
			throw new SQLException("Can't insert without data.");
		}
		if (DataHelper.saveThumbnail(values.getAsInteger(Thumbnails._ID), values
			.getAsByteArray(Thumbnails.DATA))) {
			Uri newUri = ContentUris.withAppendedId(Thumbnails.CONTENT_URI, values
				.getAsInteger(Thumbnails._ID));
			getContext().getContentResolver().notifyChange(newUri, null);
			return newUri;
		}

		throw new SQLException("Failed to insert row into " + uri);
	}

	private Uri insertDesigner(Uri uri, ContentValues values) {

		// Verify column data
		if (values.containsKey(Designers._ID) == false) {
			throw new SQLException("Can't insert without an ID.");
		}
		if (values.containsKey(Designers.NAME) == false) {
			throw new SQLException("Can't insert without a name.");
		}

		// Insert the data and return the URI
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		long rowId = db.insert(DESIGNER_TABLE, Designers.NAME, values);
		if (rowId > 0) {
			Uri newUri = ContentUris
				.withAppendedId(Designers.CONTENT_URI, values.getAsInteger(Designers._ID));
			getContext().getContentResolver().notifyChange(newUri, null);
			return newUri;
		}

		throw new SQLException("Failed to insert row into " + uri);
	}

	private Uri insertArtist(Uri uri, ContentValues values) {

		// Verify column data
		if (values.containsKey(Artists._ID) == false) {
			throw new SQLException("Can't insert without an ID.");
		}
		if (values.containsKey(Artists.NAME) == false) {
			throw new SQLException("Can't insert without a name.");
		}

		SQLiteDatabase db = dbHelper.getWritableDatabase();
		long rowId = db.insert(ARTIST_TABLE, Artists.NAME, values);
		if (rowId > 0) {
			Uri newUri = ContentUris.withAppendedId(Artists.CONTENT_URI, values.getAsInteger(Artists._ID));
			getContext().getContentResolver().notifyChange(newUri, null);
			return newUri;
		}

		throw new SQLException("Failed to insert row into " + uri);
	}

	private Uri insertPublisher(Uri uri, ContentValues values) {

		// Verify column data
		if (values.containsKey(Publishers._ID) == false) {
			throw new SQLException("Can't insert without an ID.");
		}
		if (values.containsKey(Publishers.NAME) == false) {
			throw new SQLException("Can't insert without a name.");
		}

		// Insert the data and return the URI
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		long rowId = db.insert(PUBLISHER_TABLE, Publishers.NAME, values);
		if (rowId > 0) {
			Uri newUri = ContentUris.withAppendedId(Publishers.CONTENT_URI, values
				.getAsInteger(Publishers._ID));
			getContext().getContentResolver().notifyChange(newUri, null);
			return newUri;
		}

		throw new SQLException("Failed to insert row into " + uri);
	}

	private Uri insertCategory(Uri uri, ContentValues values) {

		// Verify column data
		if (values.containsKey(Categories._ID) == false) {
			throw new SQLException("Can't insert without an ID.");
		}
		if (values.containsKey(Categories.NAME) == false) {
			throw new SQLException("Can't insert without a name.");
		}

		// Insert the data and return the URI
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		long rowId = db.insert(CATEGORY_TABLE, Categories.NAME, values);
		if (rowId > 0) {
			Uri newUri = ContentUris.withAppendedId(Categories.CONTENT_URI, values
				.getAsInteger(Categories._ID));
			getContext().getContentResolver().notifyChange(newUri, null);
			return newUri;
		}

		throw new SQLException("Failed to insert row into " + uri);
	}

	private Uri insertMechanic(Uri uri, ContentValues values) {

		// Verify column data
		if (values.containsKey(Mechanics._ID) == false) {
			throw new SQLException("Can't insert without an ID.");
		}
		if (values.containsKey(Mechanics.NAME) == false) {
			throw new SQLException("Can't insert without a name.");
		}

		// Insert the data and return the URI
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		long rowId = db.insert(MECHANIC_TABLE, Mechanics.NAME, values);
		if (rowId > 0) {
			Uri newUri = ContentUris
				.withAppendedId(Mechanics.CONTENT_URI, values.getAsInteger(Mechanics._ID));
			getContext().getContentResolver().notifyChange(newUri, null);
			return newUri;
		}

		throw new SQLException("Failed to insert row into " + uri);
	}

	private Uri insertBoardGame(Uri uri, ContentValues values) {

		// Verify column data
		if (values.containsKey(BoardGames._ID) == false) {
			throw new SQLException("Can't insert without an ID.");
		}
		if (values.containsKey(BoardGames.NAME) == false) {
			throw new SQLException("Can't insert without a name.");
		}

		// Insert the data and return the URI
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		long rowId = db.insert(BOARDGAME_TABLE, BoardGames.NAME, values);
		if (rowId > 0) {
			Uri newUri = ContentUris.withAppendedId(BoardGames.CONTENT_URI, values
				.getAsInteger(BoardGames._ID));
			getContext().getContentResolver().notifyChange(newUri, null);
			return newUri;
		}

		throw new SQLException("Failed to insert row into " + uri);
	}

	private Uri insertBoardGameDesigner(Uri uri, ContentValues values) {
		// Verify column data
		if (values.containsKey(BoardGameDesigners.BOARDGAME_ID) == false) {
			throw new SQLException("Can't insert without a boardgame ID.");
		}
		if (values.containsKey(BoardGameDesigners.DESIGNER_ID) == false) {
			throw new SQLException("Can't insert without a designer ID.");
		}

		// TODO: validate the above IDs exist? x6

		// Insert the data and return the URI
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		long rowId = db.insert(BOARDGAMEDESIGNER_TABLE, BoardGameDesigners.BOARDGAME_ID, values);
		if (rowId > 0) {
			Uri newUri = ContentUris.withAppendedId(BoardGameDesigners.CONTENT_URI, rowId);
			getContext().getContentResolver().notifyChange(newUri, null);
			return newUri;
		}

		throw new SQLException("Failed to insert row into " + uri);
	}

	private Uri insertBoardGameArtist(Uri uri, ContentValues values) {
		// Verify column data
		if (values.containsKey(BoardGameArtists.BOARDGAME_ID) == false) {
			throw new SQLException("Can't insert without a boardgame ID.");
		}
		if (values.containsKey(BoardGameArtists.ARTIST_ID) == false) {
			throw new SQLException("Can't insert without a artist ID.");
		}

		// Insert the data and return the URI
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		long rowId = db.insert(BOARDGAMEARTIST_TABLE, BoardGameArtists.BOARDGAME_ID, values);
		if (rowId > 0) {
			Uri newUri = ContentUris.withAppendedId(BoardGameArtists.CONTENT_URI, rowId);
			getContext().getContentResolver().notifyChange(newUri, null);
			return newUri;
		}

		throw new SQLException("Failed to insert row into " + uri);
	}

	private Uri insertBoardGamePublisher(Uri uri, ContentValues values) {
		// Verify column data
		if (values.containsKey(BoardGamePublishers.BOARDGAME_ID) == false) {
			throw new SQLException("Can't insert without a boardgame ID.");
		}
		if (values.containsKey(BoardGamePublishers.PUBLISHER_ID) == false) {
			throw new SQLException("Can't insert without a publisher ID.");
		}

		// Insert the data and return the URI
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		long rowId = db.insert(BOARDGAMEPUBLISHER_TABLE, BoardGamePublishers.BOARDGAME_ID, values);
		if (rowId > 0) {
			Uri newUri = ContentUris.withAppendedId(BoardGamePublishers.CONTENT_URI, rowId);
			getContext().getContentResolver().notifyChange(newUri, null);
			return newUri;
		}

		throw new SQLException("Failed to insert row into " + uri);
	}

	private Uri insertBoardGameCategory(Uri uri, ContentValues values) {
		// Verify column data
		if (values.containsKey(BoardGameCategories.BOARDGAME_ID) == false) {
			throw new SQLException("Can't insert without a boardgame ID.");
		}
		if (values.containsKey(BoardGameCategories.CATEGORY_ID) == false) {
			throw new SQLException("Can't insert without a category ID.");
		}

		// Insert the data and return the URI
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		long rowId = db.insert(BOARDGAMECATEGORY_TABLE, BoardGameCategories.BOARDGAME_ID, values);
		if (rowId > 0) {
			Uri newUri = ContentUris.withAppendedId(BoardGameCategories.CONTENT_URI, rowId);
			getContext().getContentResolver().notifyChange(newUri, null);
			return newUri;
		}

		throw new SQLException("Failed to insert row into " + uri);
	}

	private Uri insertBoardGameMechanic(Uri uri, ContentValues values) {
		// Verify column data
		if (values.containsKey(BoardGameMechanics.BOARDGAME_ID) == false) {
			throw new SQLException("Can't insert without a boardgame ID.");
		}
		if (values.containsKey(BoardGameMechanics.MECHANIC_ID) == false) {
			throw new SQLException("Can't insert without a mechanic ID.");
		}

		// Insert the data and return the URI
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		long rowId = db.insert(BOARDGAMEMECHANIC_TABLE, BoardGameMechanics.BOARDGAME_ID, values);
		if (rowId > 0) {
			Uri newUri = ContentUris.withAppendedId(BoardGameMechanics.CONTENT_URI, rowId);
			getContext().getContentResolver().notifyChange(newUri, null);
			return newUri;
		}

		throw new SQLException("Failed to insert row into " + uri);
	}

	private Uri insertBoardGameExpansion(Uri uri, ContentValues values) {
		// Verify column data
		if (values.containsKey(BoardGameExpansions.BOARDGAME_ID) == false) {
			throw new SQLException("Can't insert without a boardgame ID.");
		}
		if (values.containsKey(BoardGameExpansions.EXPANSION_ID) == false) {
			throw new SQLException("Can't insert without an expansion ID.");
		}

		// Insert the data and return the URI
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		long rowId = db.insert(BOARDGAMEEXPANSION_TABLE, BoardGameExpansions.BOARDGAME_ID, values);
		if (rowId > 0) {
			Uri newUri = ContentUris.withAppendedId(BoardGameExpansions.CONTENT_URI, rowId);
			getContext().getContentResolver().notifyChange(newUri, null);
			return newUri;
		}

		throw new SQLException("Failed to insert row into " + uri);
	}

	private Uri insertBoardGamePoll(Uri uri, ContentValues values) {
		// Verify column data
		if (values.containsKey(BoardGamePolls.BOARDGAME_ID) == false) {
			throw new SQLException("Can't insert without a boardgame ID.");
		}
		if (values.containsKey(BoardGamePolls.NAME) == false) {
			throw new SQLException("Can't insert without a name.");
		}

		// Insert the data and return the URI
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		long rowId = db.insert(BOARDGAMEPOLL_TABLE, BoardGamePolls.NAME, values);
		if (rowId > 0) {
			Uri newUri = ContentUris.withAppendedId(BoardGamePolls.CONTENT_URI, rowId);
			getContext().getContentResolver().notifyChange(newUri, null);
			return newUri;
		}

		throw new SQLException("Failed to insert row into " + uri);
	}

	private Uri insertBoardGamePollResults(Uri uri, ContentValues values) {
		// Verify column data
		if (values.containsKey(BoardGamePollResults.POLL_ID) == false) {
			throw new SQLException("Can't insert without a poll ID.");
		}

		// Insert the data and return the URI
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		long rowId = db.insert(BOARDGAMEPOLLRESULTS_TABLE, BoardGamePollResults.POLL_ID, values);
		if (rowId > 0) {
			Uri newUri = ContentUris.withAppendedId(BoardGamePollResults.CONTENT_URI, rowId);
			getContext().getContentResolver().notifyChange(newUri, null);
			return newUri;
		}

		throw new SQLException("Failed to insert row into " + uri);
	}

	private Uri insertBoardGamePollResult(Uri uri, ContentValues values) {
		// Verify column data
		if (values.containsKey(BoardGamePollResult.POLLRESULTS_ID) == false) {
			throw new SQLException("Can't insert without a poll results ID.");
		}

		// Insert the data and return the URI
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		long rowId = db.insert(BOARDGAMEPOLLRESULT_TABLE, BoardGamePollResult.POLLRESULTS_ID, values);
		if (rowId > 0) {
			Uri newUri = ContentUris.withAppendedId(BoardGamePollResult.CONTENT_URI, rowId);
			getContext().getContentResolver().notifyChange(newUri, null);
			return newUri;
		}

		throw new SQLException("Failed to insert row into " + uri);
	}

	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {

		SQLiteDatabase db = dbHelper.getWritableDatabase();
		int count = 0;

		switch (uriMatcher.match(uri)) {
		case DESIGNERS:
			count = db.update(DESIGNER_TABLE, values, selection, selectionArgs);
			break;
		case DESIGNER_ID:
			count = db.update(DESIGNER_TABLE, values, Designers._ID + "=" + uri.getPathSegments().get(1)
				+ (!TextUtils.isEmpty(selection) ? " AND (" + selection + ')' : ""), selectionArgs);
			break;
		case ARTISTS:
			count = db.update(ARTIST_TABLE, values, selection, selectionArgs);
			break;
		case ARTIST_ID:
			count = db.update(ARTIST_TABLE, values, Artists._ID + "=" + uri.getPathSegments().get(1)
				+ (!TextUtils.isEmpty(selection) ? " AND (" + selection + ')' : ""), selectionArgs);
			break;
		case PUBLISHERS:
			count = db.update(PUBLISHER_TABLE, values, selection, selectionArgs);
			break;
		case PUBLISHER_ID:
			count = db.update(PUBLISHER_TABLE, values, Publishers._ID + "=" + uri.getPathSegments().get(1)
				+ (!TextUtils.isEmpty(selection) ? " AND (" + selection + ')' : ""), selectionArgs);
			break;
		case CATEGORIES:
			count = db.update(CATEGORY_TABLE, values, selection, selectionArgs);
			break;
		case CATEGORY_ID:
			count = db.update(CATEGORY_TABLE, values, Categories._ID + "=" + uri.getPathSegments().get(1)
				+ (!TextUtils.isEmpty(selection) ? " AND (" + selection + ')' : ""), selectionArgs);
			break;
		case MECHANICS:
			count = db.update(MECHANIC_TABLE, values, selection, selectionArgs);
			break;
		case MECHANIC_ID:
			count = db.update(MECHANIC_TABLE, values, Mechanics._ID + "=" + uri.getPathSegments().get(1)
				+ (!TextUtils.isEmpty(selection) ? " AND (" + selection + ')' : ""), selectionArgs);
			break;
		case BOARDGAMES:
			count = db.update(BOARDGAME_TABLE, values, selection, selectionArgs);
			break;
		case BOARDGAME_ID:
			count = db.update(BOARDGAME_TABLE, values, BoardGames._ID + "=" + uri.getPathSegments().get(1)
				+ (!TextUtils.isEmpty(selection) ? " AND (" + selection + ')' : ""), selectionArgs);
			break;
		case BOARDGAME_DESIGNERS:
			count = db.update(BOARDGAMEDESIGNER_TABLE, values, selection, selectionArgs);
			break;
		case BOARDGAME_ARTISTS:
			count = db.update(BOARDGAMEARTIST_TABLE, values, selection, selectionArgs);
			break;
		case BOARDGAME_PUBLISHERS:
			count = db.update(BOARDGAMEPUBLISHER_TABLE, values, selection, selectionArgs);
			break;
		case BOARDGAME_CATEGORIES:
			count = db.update(BOARDGAMECATEGORY_TABLE, values, selection, selectionArgs);
			break;
		case BOARDGAME_MECHANICS:
			count = db.update(BOARDGAMEMECHANIC_TABLE, values, selection, selectionArgs);
			break;
		case BOARDGAME_EXPANSIONS:
			count = db.update(BOARDGAMEEXPANSION_TABLE, values, selection, selectionArgs);
			break;
		case BOARDGAME_POLL_ID:
			count = db.update(BOARDGAMEPOLL_TABLE, values, BoardGamePolls._ID + "="
				+ uri.getPathSegments().get(1)
				+ (!TextUtils.isEmpty(selection) ? " AND (" + selection + ')' : ""), selectionArgs);
			break;
		case BOARDGAME_POLL_RESULTS_ID:
			count = db.update(BOARDGAMEPOLLRESULTS_TABLE, values, BoardGamePollResult._ID + "="
				+ uri.getPathSegments().get(1)
				+ (!TextUtils.isEmpty(selection) ? " AND (" + selection + ')' : ""), selectionArgs);
			break;
		case BOARDGAME_POLL_RESULT_ID:
			count = db.update(BOARDGAMEPOLLRESULT_TABLE, values, BoardGamePollResult._ID + "="
				+ uri.getPathSegments().get(1)
				+ (!TextUtils.isEmpty(selection) ? " AND (" + selection + ')' : ""), selectionArgs);
			break;
		case THUMBNAILS:
			Uri thumbnailUri = insertThumbnail(uri, values);
			if (uriMatcher.match(thumbnailUri) == THUMBNAIL_ID) {
				count = 1;
			}
			break;
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}

		getContext().getContentResolver().notifyChange(uri, null);
		Log.d(LOG_TAG, "Updated URI " + uri);
		return count;
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {

		SQLiteDatabase db = dbHelper.getWritableDatabase();
		int count = 0;

		switch (uriMatcher.match(uri)) {
		case DESIGNERS:
			count = db.delete(DESIGNER_TABLE, selection, selectionArgs);
			break;
		case DESIGNER_ID:
			count = db.delete(DESIGNER_TABLE, Designers._ID + "=" + uri.getPathSegments().get(1)
				+ (!TextUtils.isEmpty(selection) ? " AND (" + selection + ')' : ""), selectionArgs);
			break;
		case ARTISTS:
			count = db.delete(ARTIST_TABLE, selection, selectionArgs);
			break;
		case ARTIST_ID:
			count = db.delete(ARTIST_TABLE, Artists._ID + "=" + uri.getPathSegments().get(1)
				+ (!TextUtils.isEmpty(selection) ? " AND (" + selection + ')' : ""), selectionArgs);
			break;
		case PUBLISHERS:
			count = db.delete(PUBLISHER_TABLE, selection, selectionArgs);
			break;
		case PUBLISHER_ID:
			count = db.delete(PUBLISHER_TABLE, Publishers._ID + "=" + uri.getPathSegments().get(1)
				+ (!TextUtils.isEmpty(selection) ? " AND (" + selection + ')' : ""), selectionArgs);
			break;
		case CATEGORIES:
			count = db.delete(CATEGORY_TABLE, selection, selectionArgs);
			break;
		case CATEGORY_ID:
			count = db.delete(CATEGORY_TABLE, Categories._ID + "=" + uri.getPathSegments().get(1)
				+ (!TextUtils.isEmpty(selection) ? " AND (" + selection + ')' : ""), selectionArgs);
			break;
		case MECHANICS:
			count = db.delete(MECHANIC_TABLE, selection, selectionArgs);
			break;
		case MECHANIC_ID:
			count = db.delete(MECHANIC_TABLE, Mechanics._ID + "=" + uri.getPathSegments().get(1)
				+ (!TextUtils.isEmpty(selection) ? " AND (" + selection + ')' : ""), selectionArgs);
			break;
		case BOARDGAMES:
			count = db.delete(BOARDGAME_TABLE, selection, selectionArgs);
			// TODO: delete thumbnails selectively
			if (TextUtils.isEmpty(selection)) {
				count += DataHelper.deleteThumbnails();
			}
			count += db.delete(BOARDGAMEDESIGNER_TABLE, selection, selectionArgs);
			count += db.delete(BOARDGAMEARTIST_TABLE, selection, selectionArgs);
			count += db.delete(BOARDGAMEPUBLISHER_TABLE, selection, selectionArgs);
			count += db.delete(BOARDGAMECATEGORY_TABLE, selection, selectionArgs);
			count += db.delete(BOARDGAMEMECHANIC_TABLE, selection, selectionArgs);
			count += db.delete(BOARDGAMEEXPANSION_TABLE, selection, selectionArgs);
			count += db.delete(BOARDGAMEPOLL_TABLE, selection, selectionArgs);
			count += db.delete(BOARDGAMEPOLLRESULTS_TABLE, selection, selectionArgs);
			count += db.delete(BOARDGAMEPOLLRESULT_TABLE, selection, selectionArgs);
			break;
		case BOARDGAME_ID:
			String boardgameId = uri.getPathSegments().get(1);
			// need to get this before deleting the game
			String thumbnailId = getThumbnailId(boardgameId);
			count = db.delete(BOARDGAME_TABLE, BoardGames._ID + "=" + boardgameId
				+ (!TextUtils.isEmpty(selection) ? " AND (" + selection + ')' : ""), selectionArgs);
			if (count > 0) {
				if (DataHelper.deleteThumbnail(thumbnailId)) {
					count++;
				}
				count += db.delete(BOARDGAMEDESIGNER_TABLE, BoardGameDesigners.BOARDGAME_ID + "="
					+ boardgameId, null);
				count += db.delete(BOARDGAMEARTIST_TABLE, BoardGameArtists.BOARDGAME_ID + "=" + boardgameId,
					null);
				count += db.delete(BOARDGAMEPUBLISHER_TABLE, BoardGameArtists.BOARDGAME_ID + "="
					+ boardgameId, null);
				count += db.delete(BOARDGAMECATEGORY_TABLE,
					BoardGameArtists.BOARDGAME_ID + "=" + boardgameId, null);
				count += db.delete(BOARDGAMEMECHANIC_TABLE,
					BoardGameArtists.BOARDGAME_ID + "=" + boardgameId, null);
				count += db.delete(BOARDGAMEEXPANSION_TABLE, BoardGameExpansions.BOARDGAME_ID + "="
					+ boardgameId, null);
				count += db.delete(BOARDGAMEPOLLRESULT_TABLE, BOARDGAMEPOLLRESULT_TABLE + "."
					+ BoardGamePollResult._ID + " IN (SELECT " + BOARDGAMEPOLLRESULT_TABLE + "."
					+ BoardGamePollResult._ID + " FROM " + BOARDGAMEPOLLRESULT_TABLE + ", "
					+ BOARDGAMEPOLLRESULTS_TABLE + ", " + BOARDGAMEPOLL_TABLE + " WHERE "
					+ BOARDGAMEPOLLRESULT_TABLE + "." + BoardGamePollResult.POLLRESULTS_ID + " = "
					+ BOARDGAMEPOLLRESULTS_TABLE + "." + BoardGamePollResults._ID + " AND "
					+ BOARDGAMEPOLLRESULTS_TABLE + "." + BoardGamePollResults.POLL_ID + " = "
					+ BOARDGAMEPOLL_TABLE + "." + BoardGamePolls._ID + " AND " + BOARDGAMEPOLL_TABLE + "."
					+ BoardGamePolls.BOARDGAME_ID + "=" + boardgameId + ")", null);
				count += db.delete(BOARDGAMEPOLLRESULTS_TABLE, BOARDGAMEPOLLRESULTS_TABLE + "."
					+ BoardGamePollResults._ID + " IN (SELECT " + BOARDGAMEPOLLRESULTS_TABLE + "."
					+ BoardGamePollResults._ID + " FROM " + BOARDGAMEPOLLRESULTS_TABLE + ", "
					+ BOARDGAMEPOLL_TABLE + " WHERE " + BOARDGAMEPOLLRESULTS_TABLE + "."
					+ BoardGamePollResults.POLL_ID + " = " + BOARDGAMEPOLL_TABLE + "." + BoardGamePolls._ID
					+ " AND " + BOARDGAMEPOLL_TABLE + "." + BoardGamePolls.BOARDGAME_ID + "=" + boardgameId
					+ ")", null);
				count += db
					.delete(BOARDGAMEPOLL_TABLE, BoardGamePolls.BOARDGAME_ID + "=" + boardgameId, null);
			}
			break;
		case BOARDGAME_DESIGNER_ID:
			count = db.delete(BOARDGAMEDESIGNER_TABLE, BoardGameDesigners._ID + "="
				+ uri.getPathSegments().get(1)
				+ (!TextUtils.isEmpty(selection) ? " AND (" + selection + ')' : ""), selectionArgs);
			break;
		case BOARDGAME_ARTIST_ID:
			count = db.delete(BOARDGAMEARTIST_TABLE, BoardGameArtists._ID + "="
				+ uri.getPathSegments().get(1)
				+ (!TextUtils.isEmpty(selection) ? " AND (" + selection + ')' : ""), selectionArgs);
			break;
		case BOARDGAME_PUBLISHER_ID:
			count = db.delete(BOARDGAMEPUBLISHER_TABLE, BoardGamePublishers._ID + "="
				+ uri.getPathSegments().get(1)
				+ (!TextUtils.isEmpty(selection) ? " AND (" + selection + ')' : ""), selectionArgs);
			break;
		case BOARDGAME_CATEGORY_ID:
			count = db.delete(BOARDGAMECATEGORY_TABLE, BoardGameCategories._ID + "="
				+ uri.getPathSegments().get(1)
				+ (!TextUtils.isEmpty(selection) ? " AND (" + selection + ')' : ""), selectionArgs);
			break;
		case BOARDGAME_MECHANIC_ID:
			count = db.delete(BOARDGAMEMECHANIC_TABLE, BoardGameMechanics._ID + "="
				+ uri.getPathSegments().get(1)
				+ (!TextUtils.isEmpty(selection) ? " AND (" + selection + ')' : ""), selectionArgs);
			break;
		case BOARDGAME_EXPANSION_ID:
			count = db.delete(BOARDGAMEEXPANSION_TABLE, BoardGameExpansions._ID + "="
				+ uri.getPathSegments().get(1)
				+ (!TextUtils.isEmpty(selection) ? " AND (" + selection + ')' : ""), selectionArgs);
			break;
		case BOARDGAME_POLL_ID:
			count = db.delete(BOARDGAMEPOLL_TABLE, BoardGamePolls._ID + "=" + uri.getPathSegments().get(1)
				+ (!TextUtils.isEmpty(selection) ? " AND (" + selection + ')' : ""), selectionArgs);
			break;
		case BOARDGAME_POLL_RESULTS_ID:
			count = db.delete(BOARDGAMEPOLLRESULTS_TABLE, BoardGamePollResults._ID + "="
				+ uri.getPathSegments().get(1)
				+ (!TextUtils.isEmpty(selection) ? " AND (" + selection + ')' : ""), selectionArgs);
			break;
		case BOARDGAME_POLL_RESULT_ID:
			count = db.delete(BOARDGAMEPOLLRESULT_TABLE, BoardGamePollResult._ID + "="
				+ uri.getPathSegments().get(1)
				+ (!TextUtils.isEmpty(selection) ? " AND (" + selection + ')' : ""), selectionArgs);
			break;
		case THUMBNAIL_ID:
			if (DataHelper.deleteThumbnail(uri.getLastPathSegment())) {
				count = 1;
			}
			break;
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}

		getContext().getContentResolver().notifyChange(uri, null);
		Log.d(LOG_TAG, "Deleted URI " + uri);
		return count;
	}

	@Override
	public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
		if (uriMatcher.match(uri) != THUMBNAIL_ID) {
			Log.w(LOG_TAG, "openFile got an invalid uri: " + uri.toString());
			throw new IllegalArgumentException("openFile only supports thumbnails");
		}

		String thumbnailId = uri.getLastPathSegment();
		String fileName = DataHelper.getThumbnailPath(thumbnailId);
		if (!TextUtils.isEmpty(fileName)) {
			File file = new File(fileName);
			if (file.exists()) {
				return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
			}
		}
		return null;
	}

	private String getThumbnailId(String boardgameId) {
		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
		qb.setTables(BOARDGAME_TABLE);
		HashMap<String, String> bpm = new HashMap<String, String>();
		bpm.put(BoardGames._ID, BoardGames._ID);
		bpm.put(BoardGames.THUMBNAIL_URL, BoardGames.THUMBNAIL_ID);
		qb.setProjectionMap(bpm);
		qb.appendWhere(BoardGames._ID + "=" + boardgameId);
		Cursor c = qb.query(dbHelper.getReadableDatabase(), null, null, null, null, null, null);
		if (c.moveToFirst()) {
			return c.getString(c.getColumnIndex(BoardGames.THUMBNAIL_ID));
		}
		return null;
	}

	private static class DatabaseHelper extends SQLiteOpenHelper {

		DatabaseHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			Log.i(LOG_TAG, "Creating the BGG database.");

			String sql = "create table " + DESIGNER_TABLE + " (" + Designers._ID + " integer primary key, "
				+ Designers.NAME + " text not null, " + Designers.DESCRIPTION + " text, "
				+ Designers.UPDATED_DATE + " integer)";
			db.execSQL(sql);
			Log.i(LOG_TAG, String.format("Created %s table.", DESIGNER_TABLE));

			sql = "create table " + ARTIST_TABLE + " (" + Artists._ID + " integer primary key, "
				+ Artists.NAME + " text not null, " + Artists.DESCRIPTION + " text, " + Artists.UPDATED_DATE
				+ " integer)";
			db.execSQL(sql);
			Log.i(LOG_TAG, String.format("Created %s table.", ARTIST_TABLE));

			sql = "create table " + PUBLISHER_TABLE + " (" + Publishers._ID + " integer primary key, "
				+ Publishers.NAME + " text not null, " + Publishers.DESCRIPTION + " text, "
				+ Publishers.UPDATED_DATE + " integer)";
			db.execSQL(sql);
			Log.i(LOG_TAG, String.format("Created %s table.", PUBLISHER_TABLE));

			sql = "create table " + CATEGORY_TABLE + " (" + Categories._ID + " integer primary key, "
				+ Categories.NAME + " text not null)";
			db.execSQL(sql);
			Log.i(LOG_TAG, String.format("Created %s table.", CATEGORY_TABLE));

			sql = "create table " + MECHANIC_TABLE + " (" + Mechanics._ID + " integer primary key, "
				+ Mechanics.NAME + " text not null)";
			db.execSQL(sql);
			Log.i(LOG_TAG, String.format("Created %s table.", MECHANIC_TABLE));

			sql = "create table " + BOARDGAME_TABLE + " (" + BoardGames._ID + " integer primary key, "
				+ BoardGames.NAME + " text not null, " + BoardGames.SORT_INDEX + " integer, "
				+ BoardGames.SORT_NAME + " text, " + BoardGames.YEAR + " integer, " + BoardGames.MIN_PLAYERS
				+ " integer, " + BoardGames.MAX_PLAYERS + " integer, " + BoardGames.PLAYING_TIME
				+ " integer, " + BoardGames.AGE + " integer, " + BoardGames.DESCRIPTION + " text, "
				+ BoardGames.THUMBNAIL_URL + " text, " + BoardGames.THUMBNAIL_ID + " integer, "
				+ BoardGames.RATING_COUNT + " integer, " + BoardGames.AVERAGE + " real, "
				+ BoardGames.BAYES_AVERAGE + " real, " + BoardGames.RANK + " integer, "
				+ BoardGames.STANDARD_DEVIATION + " real, " + BoardGames.MEDIAN + " real, "
				+ BoardGames.OWNED_COUNT + " integer, " + BoardGames.TRADING_COUNT + " integer, "
				+ BoardGames.WANTING_COUNT + " integer, " + BoardGames.WISHING_COUNT + " integer, "
				+ BoardGames.COMMENT_COUNT + " integer, " + BoardGames.WEIGHT_COUNT + " integer, "
				+ BoardGames.AVERAGE_WEIGHT + " real, " + BoardGames.UPDATED_DATE + " integer, "
				+ BoardGames.RANK_ABSTRACT + " integer, " + BoardGames.RANK_CCG + " integer, "
				+ BoardGames.RANK_FAMILY + " integer, " + BoardGames.RANK_KIDS + " integer, "
				+ BoardGames.RANK_PARTY + " integer, " + BoardGames.RANK_STRATEGY + " integer, "
				+ BoardGames.RANK_THEMATIC + " integer, " + BoardGames.RANK_WAR + " integer)";
			db.execSQL(sql);
			Log.i(LOG_TAG, String.format("Created %s table.", BOARDGAME_TABLE));

			sql = "create table " + BOARDGAMEDESIGNER_TABLE + " (" + BoardGameDesigners._ID
				+ " integer primary key autoincrement, " + BoardGameDesigners.BOARDGAME_ID
				+ " integer not null, " + BoardGameDesigners.DESIGNER_ID + " integer not null)";
			db.execSQL(sql);
			Log.i(LOG_TAG, String.format("Created %s table.", BOARDGAMEDESIGNER_TABLE));

			sql = "create table " + BOARDGAMEARTIST_TABLE + " (" + BoardGameArtists._ID
				+ " integer primary key autoincrement, " + BoardGameArtists.BOARDGAME_ID
				+ " integer not null, " + BoardGameArtists.ARTIST_ID + " integer not null)";
			db.execSQL(sql);
			Log.i(LOG_TAG, String.format("Created %s table.", BOARDGAMEARTIST_TABLE));

			sql = "create table " + BOARDGAMEPUBLISHER_TABLE + " (" + BoardGamePublishers._ID
				+ " integer primary key autoincrement, " + BoardGamePublishers.BOARDGAME_ID
				+ " integer not null, " + BoardGamePublishers.PUBLISHER_ID + " integer not null)";
			db.execSQL(sql);
			Log.i(LOG_TAG, String.format("Created %s table.", BOARDGAMEPUBLISHER_TABLE));

			sql = "create table " + BOARDGAMECATEGORY_TABLE + " (" + BoardGameCategories._ID
				+ " integer primary key autoincrement, " + BoardGameCategories.BOARDGAME_ID
				+ " integer not null, " + BoardGameCategories.CATEGORY_ID + " integer not null)";
			db.execSQL(sql);
			Log.i(LOG_TAG, String.format("Created %s table.", BOARDGAMECATEGORY_TABLE));

			sql = "create table " + BOARDGAMEMECHANIC_TABLE + " (" + BoardGameMechanics._ID
				+ " integer primary key autoincrement, " + BoardGameMechanics.BOARDGAME_ID
				+ " integer not null, " + BoardGameMechanics.MECHANIC_ID + " integer not null)";
			db.execSQL(sql);
			Log.i(LOG_TAG, String.format("Created %s table.", BOARDGAMEMECHANIC_TABLE));

			sql = "create table " + BOARDGAMEEXPANSION_TABLE + " (" + BoardGameExpansions._ID
				+ " integer primary key autoincrement, " + BoardGameExpansions.BOARDGAME_ID
				+ " integer not null, " + BoardGameExpansions.EXPANSION_ID + " integer not null)";
			db.execSQL(sql);
			Log.i(LOG_TAG, String.format("Created %s table.", BOARDGAMEEXPANSION_TABLE));

			sql = "create table " + BOARDGAMEPOLL_TABLE + " (" + BoardGamePolls._ID
				+ " integer primary key autoincrement, " + BoardGamePolls.BOARDGAME_ID
				+ " integer not null, " + BoardGamePolls.NAME + " text not null, " + BoardGamePolls.TITLE
				+ " text, " + BoardGamePolls.VOTES + " integer)";
			db.execSQL(sql);
			Log.i(LOG_TAG, String.format("Created %s table.", BOARDGAMEPOLL_TABLE));

			sql = "create table " + BOARDGAMEPOLLRESULTS_TABLE + " (" + BoardGamePollResults._ID
				+ " integer primary key autoincrement, " + BoardGamePollResults.POLL_ID
				+ " integer not null, " + BoardGamePollResults.PLAYERS + " string)";
			db.execSQL(sql);
			Log.i(LOG_TAG, String.format("Created %s table.", BOARDGAMEPOLLRESULTS_TABLE));

			sql = "create table " + BOARDGAMEPOLLRESULT_TABLE + " (" + BoardGamePollResult._ID
				+ " integer primary key autoincrement, " + BoardGamePollResult.POLLRESULTS_ID
				+ " integer not null, " + BoardGamePollResult.LEVEL + " integer, "
				+ BoardGamePollResult.VALUE + " text not null, " + BoardGamePollResult.VOTES
				+ " integer not null)";
			db.execSQL(sql);
			Log.i(LOG_TAG, String.format("Created %s table.", BOARDGAMEPOLLRESULT_TABLE));
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			Log
				.w(LOG_TAG, String
					.format("Upgrading database from version %s to %s.", oldVersion, newVersion));

			if (newVersion < 6) {
				dropTable(db, DESIGNER_TABLE);
				dropTable(db, ARTIST_TABLE);
				dropTable(db, PUBLISHER_TABLE);
				dropTable(db, CATEGORY_TABLE);
				dropTable(db, MECHANIC_TABLE);
				dropTable(db, BOARDGAME_TABLE);
				dropTable(db, BOARDGAMEDESIGNER_TABLE);
				dropTable(db, BOARDGAMEARTIST_TABLE);
				dropTable(db, BOARDGAMEPUBLISHER_TABLE);
				dropTable(db, BOARDGAMECATEGORY_TABLE);
				dropTable(db, BOARDGAMEMECHANIC_TABLE);
				dropTable(db, BOARDGAMEEXPANSION_TABLE);
				dropTable(db, BOARDGAMEPOLL_TABLE);
				dropTable(db, BOARDGAMEPOLLRESULTS_TABLE);
				dropTable(db, BOARDGAMEPOLLRESULT_TABLE);
				onCreate(db);
			}
			if (oldVersion < 6){
				DataHelper.renameThumbnailFolder();
			}
		}

		private static void dropTable(SQLiteDatabase db, String tableName) {
			db.execSQL("DROP TABLE IF EXISTS " + tableName);
			Log.i(LOG_TAG, String.format("Dropped %s table.", tableName));
		}
	}
}
