package com.boardgamegeek;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collection;

import android.app.Activity;
import android.content.ContentValues;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;

import com.boardgamegeek.BoardGameGeekData.*;
import com.boardgamegeek.model.*;

public class DataHelper {

	private final static String LOG_TAG = "BoardGameGeek";

	// creates a board game object from a cursor
	public static BoardGame createBoardGame(Activity activity, Cursor cursor) {
		BoardGame boardGame = new BoardGame();

		boardGame.setGameId(cursor.getInt(cursor.getColumnIndex(BoardGames._ID)));
		boardGame.setName(cursor.getString(cursor.getColumnIndex(BoardGames.NAME)));
		boardGame.setSortIndex(cursor.getInt(cursor.getColumnIndex(BoardGames.SORT_INDEX)));
		boardGame.setYearPublished(cursor.getInt(cursor.getColumnIndex(BoardGames.YEAR)));
		boardGame.setMinPlayers(cursor.getInt(cursor.getColumnIndex(BoardGames.MIN_PLAYERS)));
		boardGame.setMaxPlayers(cursor.getInt(cursor.getColumnIndex(BoardGames.MAX_PLAYERS)));
		boardGame.setPlayingTime(cursor.getInt(cursor.getColumnIndex(BoardGames.PLAYING_TIME)));
		boardGame.setAge(cursor.getInt(cursor.getColumnIndex(BoardGames.AGE)));
		boardGame.setDescription(cursor.getString(cursor.getColumnIndex(BoardGames.DESCRIPTION)));
		boardGame.setThumbnailUrl(cursor.getString(cursor.getColumnIndex(BoardGames.THUMBNAIL_URL)));
		boardGame.setRatingCount(cursor.getInt(cursor.getColumnIndex(BoardGames.RATING_COUNT)));
		boardGame.setAverage(cursor.getDouble(cursor.getColumnIndex(BoardGames.AVERAGE)));
		boardGame.setBayesAverage(cursor.getDouble(cursor.getColumnIndex(BoardGames.BAYES_AVERAGE)));
		boardGame.setRank(cursor.getInt(cursor.getColumnIndex(BoardGames.RANK)));
		boardGame.setRankAbstract(cursor.getInt(cursor.getColumnIndex(BoardGames.RANK_ABSTRACT)));
		boardGame.setRankCcg(cursor.getInt(cursor.getColumnIndex(BoardGames.RANK_CCG)));
		boardGame.setRankFamily(cursor.getInt(cursor.getColumnIndex(BoardGames.RANK_FAMILY)));
		boardGame.setRankKids(cursor.getInt(cursor.getColumnIndex(BoardGames.RANK_KIDS)));
		boardGame.setRankParty(cursor.getInt(cursor.getColumnIndex(BoardGames.RANK_PARTY)));
		boardGame.setRankStrategy(cursor.getInt(cursor.getColumnIndex(BoardGames.RANK_STRATEGY)));
		boardGame.setRankTheme(cursor.getInt(cursor.getColumnIndex(BoardGames.RANK_THEMATIC)));
		boardGame.setRankWar(cursor.getInt(cursor.getColumnIndex(BoardGames.RANK_WAR)));
		boardGame
			.setStandardDeviation(cursor.getDouble(cursor.getColumnIndex(BoardGames.STANDARD_DEVIATION)));
		boardGame.setMedian(cursor.getDouble(cursor.getColumnIndex(BoardGames.MEDIAN)));
		boardGame.setOwnedCount(cursor.getInt(cursor.getColumnIndex(BoardGames.OWNED_COUNT)));
		boardGame.setTradingCount(cursor.getInt(cursor.getColumnIndex(BoardGames.TRADING_COUNT)));
		boardGame.setWantingCount(cursor.getInt(cursor.getColumnIndex(BoardGames.WANTING_COUNT)));
		boardGame.setWishingCount(cursor.getInt(cursor.getColumnIndex(BoardGames.WISHING_COUNT)));
		boardGame.setCommentCount(cursor.getInt(cursor.getColumnIndex(BoardGames.COMMENT_COUNT)));
		boardGame.setWeightCount(cursor.getInt(cursor.getColumnIndex(BoardGames.WEIGHT_COUNT)));
		boardGame.setAverageWeight(cursor.getDouble(cursor.getColumnIndex(BoardGames.AVERAGE_WEIGHT)));
		boardGame.setThumbnail(Drawable.createFromPath(getThumbnailPath(BoardGames.THUMBNAIL_ID)));

		int gameId = boardGame.getGameId();

		cursor = activity.managedQuery(BoardGameDesigners.CONTENT_URI, new String[] { BoardGameDesigners._ID,
			BoardGameDesigners.DESIGNER_ID, BoardGameDesigners.DESIGNER_NAME },
			BoardGameDesigners.BOARDGAME_ID + "=" + gameId, null, BoardGameDesigners.DESIGNER_NAME);
		boardGame.createDesigners(cursor);

		cursor = activity.managedQuery(BoardGameArtists.CONTENT_URI, new String[] { BoardGameArtists._ID,
			BoardGameArtists.ARTIST_ID, BoardGameArtists.ARTIST_NAME }, BoardGameArtists.BOARDGAME_ID + "="
			+ gameId, null, BoardGameArtists.ARTIST_NAME);
		boardGame.createArtists(cursor);

		cursor = activity.managedQuery(BoardGamePublishers.CONTENT_URI, new String[] {
			BoardGamePublishers._ID, BoardGamePublishers.PUBLISHER_ID, BoardGamePublishers.PUBLISHER_NAME },
			BoardGamePublishers.BOARDGAME_ID + "=" + gameId, null, BoardGamePublishers.PUBLISHER_NAME);
		boardGame.createPublishers(cursor);

		cursor = activity.managedQuery(BoardGameCategories.CONTENT_URI, new String[] {
			BoardGameCategories._ID, BoardGameCategories.CATEGORY_ID, BoardGameCategories.CATEGORY_NAME },
			BoardGameCategories.BOARDGAME_ID + "=" + gameId, null, BoardGameCategories.CATEGORY_NAME);
		boardGame.createCategories(cursor);

		cursor = activity.managedQuery(BoardGameMechanics.CONTENT_URI, new String[] { BoardGameMechanics._ID,
			BoardGameMechanics.MECHANIC_ID, BoardGameMechanics.MECHANIC_NAME },
			BoardGameMechanics.BOARDGAME_ID + "=" + gameId, null, BoardGameMechanics.MECHANIC_NAME);
		boardGame.createMechanics(cursor);

		cursor = activity.managedQuery(BoardGameExpansions.CONTENT_URI, new String[] {
			BoardGameExpansions._ID, BoardGameExpansions.EXPANSION_ID, BoardGameExpansions.EXPANSION_NAME },
			BoardGameExpansions.BOARDGAME_ID + "=" + gameId, null, BoardGameExpansions.EXPANSION_NAME);
		boardGame.createExpanions(cursor);

		cursor = activity.managedQuery(BoardGamePolls.CONTENT_URI, null, BoardGamePolls.BOARDGAME_ID + "="
			+ gameId, null, null);
		boardGame.createPolls(cursor);

		if (cursor.moveToFirst()) {
			do {
				int pollId = cursor.getInt(cursor.getColumnIndex(BoardGamePolls._ID));
				Cursor resultsCursor = activity.managedQuery(BoardGamePollResults.CONTENT_URI, null,
					BoardGamePollResults.POLL_ID + "=" + pollId, null, null);
				boardGame.createPollResults(resultsCursor);

				if (resultsCursor.moveToFirst()) {
					do {
						int resultsId = resultsCursor.getInt(resultsCursor
							.getColumnIndex(BoardGamePollResults._ID));
						Cursor resultCursor = activity.managedQuery(BoardGamePollResult.CONTENT_URI, null,
							BoardGamePollResult.POLLRESULTS_ID + "=" + resultsId, null, null);
						boardGame.createPollResult(resultCursor);
					} while (resultsCursor.moveToNext());
				}
			} while (cursor.moveToNext());
		}

		return boardGame;
	}

	public static void addToDatabase(Activity activity, BoardGame boardGame) {

		if (boardGame == null) {
			Log.w(LOG_TAG, "boardGame was unexpectedly null");
			return;
		}

		if (boardGame.getGameId() <= 0) {
			Log.w(LOG_TAG, "Invalid boardGame ID: " + boardGame.getGameId());
		}

		// see if it is already in the database
		ContentValues values = createBoardGameValues(boardGame);
		Uri uri = Uri.withAppendedPath(BoardGames.CONTENT_URI, "" + boardGame.getGameId());
		Cursor cursor = activity.managedQuery(uri, null, null, null, null);
		BoardGame oldBoardGame;
		if (cursor.moveToFirst()) {
			// update
			oldBoardGame = createBoardGame(activity, cursor);
			activity.getContentResolver().update(uri, values, null, null);

			// delete thumbnail on disk
			if (!boardGame.getThumbnailUrl().equals(oldBoardGame.getThumbnailUrl())) {
				DataHelper.deleteThumbnail(oldBoardGame.getThumbnailId());
				oldBoardGame.setThumbnail(null);
			}
		} else {
			// insert
			oldBoardGame = new BoardGame();
			activity.getContentResolver().insert(BoardGames.CONTENT_URI, values);
		}

		Collection<Integer> designerIds = oldBoardGame.getDesignerIds();
		for (int i = 0; i < boardGame.getDesignerCount(); i++) {
			int designerId = boardGame.getDesignerByPosition(i).Id;
			String designerName = boardGame.getDesignerByPosition(i).Name;

			values.clear();
			values.put(Designers._ID, designerId);
			values.put(Designers.NAME, designerName);

			// ensure designer record is present and correct
			Uri designerUri = Uri.withAppendedPath(Designers.CONTENT_URI, "" + designerId);
			cursor = activity.managedQuery(designerUri, null, null, null, null);
			if (cursor.moveToFirst()) {
				if (!designerName.equals(cursor.getString(cursor.getColumnIndex(Designers.NAME)))) {
					activity.getContentResolver().update(designerUri, values, null, null);
				}
			} else {
				activity.getContentResolver().insert(Designers.CONTENT_URI, values);
			}

			// see if this designer already exists on this board game
			if (designerIds.contains(designerId)) {
				// remove from collection
				designerIds.remove(designerId);
			} else {
				// insert
				values.clear();
				values.put(BoardGameDesigners.BOARDGAME_ID, boardGame.getGameId());
				values.put(BoardGameDesigners.DESIGNER_ID, designerId);
				uri = activity.getContentResolver().insert(BoardGameDesigners.CONTENT_URI, values);
			}
		}
		// remove unused designers
		for (int designerId : designerIds) {
			int key = oldBoardGame.getDesignerKey(designerId);
			Uri designerUri = Uri.withAppendedPath(BoardGameDesigners.CONTENT_URI, "" + key);
			activity.getContentResolver().delete(designerUri, null, null);
		}

		Collection<Integer> artistIds = oldBoardGame.getArtistIds();
		for (int i = 0; i < boardGame.getArtistCount(); i++) {
			int artistId = boardGame.getArtistByPosition(i).Id;
			String artistName = boardGame.getArtistByPosition(i).Name;

			values.clear();
			values.put(Artists._ID, artistId);
			values.put(Artists.NAME, artistName);

			Uri artistUri = Uri.withAppendedPath(Artists.CONTENT_URI, "" + artistId);
			cursor = activity.managedQuery(artistUri, null, null, null, null);
			if (cursor.moveToFirst()) {
				if (!artistName.equals(cursor.getString(cursor.getColumnIndex(Artists.NAME)))) {
					activity.getContentResolver().update(artistUri, values, null, null);
				}
			} else {
				activity.getContentResolver().insert(Artists.CONTENT_URI, values);
			}

			// see if this artist already exists on this board game
			if (artistIds.contains(artistId)) {
				// remove from collection
				artistIds.remove(artistId);
			} else {
				values.clear();
				values.put(BoardGameArtists.BOARDGAME_ID, boardGame.getGameId());
				values.put(BoardGameArtists.ARTIST_ID, artistId);
				uri = activity.getContentResolver().insert(BoardGameArtists.CONTENT_URI, values);
			}
		}
		// remove unused artists
		for (int artistId : artistIds) {
			int key = oldBoardGame.getArtistKey(artistId);
			Uri artistUri = Uri.withAppendedPath(BoardGameArtists.CONTENT_URI, "" + key);
			activity.getContentResolver().delete(artistUri, null, null);
		}

		Collection<Integer> publisherIds = oldBoardGame.getPublisherIds();
		for (int i = 0; i < boardGame.getPublisherCount(); i++) {
			int publisherId = boardGame.getPublisherByPosition(i).Id;
			String publisherName = boardGame.getPublisherByPosition(i).Name;

			values.clear();
			values.put(Publishers._ID, publisherId);
			values.put(Publishers.NAME, publisherName);

			// ensure publisher record is present and correct
			Uri publisherUri = Uri.withAppendedPath(Publishers.CONTENT_URI, "" + publisherId);
			cursor = activity.managedQuery(publisherUri, null, null, null, null);
			if (cursor.moveToFirst()) {
				if (!publisherName.equals(cursor.getString(cursor.getColumnIndex(Publishers.NAME)))) {
					activity.getContentResolver().update(publisherUri, values, null, null);
				}
			} else {
				activity.getContentResolver().insert(Publishers.CONTENT_URI, values);
			}

			// see if this publisher already exists on this board game
			if (publisherIds.contains(publisherId)) {
				// remove from collection
				publisherIds.remove(publisherId);
			} else {
				values.clear();
				values.put(BoardGamePublishers.BOARDGAME_ID, boardGame.getGameId());
				values.put(BoardGamePublishers.PUBLISHER_ID, publisherId);
				uri = activity.getContentResolver().insert(BoardGamePublishers.CONTENT_URI, values);
			}
		}
		// remove unused publishers
		for (int publisherId : publisherIds) {
			int key = oldBoardGame.getPublisherKey(publisherId);
			Uri publisherUri = Uri.withAppendedPath(BoardGamePublishers.CONTENT_URI, "" + key);
			activity.getContentResolver().delete(publisherUri, null, null);
		}

		Collection<Integer> categoryIds = oldBoardGame.getCategoryIds();
		for (int i = 0; i < boardGame.getCategoryCount(); i++) {
			int categoryId = boardGame.getCategoryByPosition(i).Id;
			String categoryName = boardGame.getCategoryByPosition(i).Name;

			values.clear();
			values.put(Categories._ID, categoryId);
			values.put(Categories.NAME, categoryName);

			// ensure category record is present and correct
			Uri categoryUri = Uri.withAppendedPath(Categories.CONTENT_URI, "" + categoryId);
			cursor = activity.managedQuery(categoryUri, null, null, null, null);
			if (cursor.moveToFirst()) {
				if (!categoryName.equals(cursor.getString(cursor.getColumnIndex(Categories.NAME)))) {
					activity.getContentResolver().update(categoryUri, values, null, null);
				}
			} else {
				activity.getContentResolver().insert(Categories.CONTENT_URI, values);
			}

			// see if this category already exists on this board game
			if (categoryIds.contains(categoryId)) {
				// remove from collection
				categoryIds.remove(categoryId);
			} else {
				values.clear();
				values.put(BoardGameCategories.BOARDGAME_ID, boardGame.getGameId());
				values.put(BoardGameCategories.CATEGORY_ID, categoryId);
				uri = activity.getContentResolver().insert(BoardGameCategories.CONTENT_URI, values);
			}
		}
		// remove unused categories
		for (int categoryId : categoryIds) {
			int key = oldBoardGame.getCategoryKey(categoryId);
			Uri categoryUri = Uri.withAppendedPath(BoardGameCategories.CONTENT_URI, "" + key);
			activity.getContentResolver().delete(categoryUri, null, null);
		}

		Collection<Integer> mechanicIds = oldBoardGame.getMechanicIds();
		for (int i = 0; i < boardGame.getMechanicCount(); i++) {
			int mechanicId = boardGame.getMechanicByPosition(i).Id;
			String mechanicName = boardGame.getMechanicByPosition(i).Name;

			values.clear();
			values.put(Mechanics._ID, mechanicId);
			values.put(Mechanics.NAME, mechanicName);

			// ensure mechanic record is present and correct
			Uri mechanicUri = Uri.withAppendedPath(Mechanics.CONTENT_URI, "" + mechanicId);
			cursor = activity.managedQuery(mechanicUri, null, null, null, null);
			if (cursor.moveToFirst()) {
				if (!mechanicName.equals(cursor.getString(cursor.getColumnIndex(Mechanics.NAME)))) {
					activity.getContentResolver().update(mechanicUri, values, null, null);
				}
			} else {
				activity.getContentResolver().insert(Mechanics.CONTENT_URI, values);
			}

			// see if this mechanic already exists on this board game
			if (mechanicIds.contains(mechanicId)) {
				// remove from collection
				mechanicIds.remove(mechanicId);
			} else {
				values.clear();
				values.put(BoardGameMechanics.BOARDGAME_ID, boardGame.getGameId());
				values.put(BoardGameMechanics.MECHANIC_ID, mechanicId);
				uri = activity.getContentResolver().insert(BoardGameMechanics.CONTENT_URI, values);
			}
		}
		// remove unused mechanics
		for (int mechanicId : mechanicIds) {
			int key = oldBoardGame.getMechanicKey(mechanicId);
			Uri mechanicUri = Uri.withAppendedPath(BoardGameMechanics.CONTENT_URI, "" + key);
			activity.getContentResolver().delete(mechanicUri, null, null);
		}

		Collection<Integer> expansionIds = oldBoardGame.getExpansionIds();
		for (int i = 0; i < boardGame.getExpansionCount(); i++) {
			int expansionId = boardGame.getExpansionByPosition(i).Id;
			String expansionName = boardGame.getExpansionByPosition(i).Name;

			values.clear();
			values.put(BoardGames._ID, expansionId);
			values.put(BoardGames.NAME, expansionName);

			// ensure expansion record is present and correct
			Uri expansionUri = Uri.withAppendedPath(BoardGames.CONTENT_URI, "" + expansionId);
			cursor = activity.managedQuery(expansionUri, null, null, null, null);
			if (cursor.moveToFirst()) {
				if (!expansionName.equals(cursor.getString(cursor.getColumnIndex(BoardGames.NAME)))) {
					activity.getContentResolver().update(expansionUri, values, null, null);
				}
			} else {
				activity.getContentResolver().insert(BoardGames.CONTENT_URI, values);
			}

			// see if this expansion already exists on this board game
			if (expansionIds.contains(expansionId)) {
				// remove from collection
				expansionIds.remove(expansionId);
			} else {
				values.clear();
				values.put(BoardGameExpansions.BOARDGAME_ID, boardGame.getGameId());
				values.put(BoardGameExpansions.EXPANSION_ID, expansionId);
				uri = activity.getContentResolver().insert(BoardGameExpansions.CONTENT_URI, values);
			}
		}
		// remove unused expansions
		for (int expansionId : expansionIds) {
			int key = oldBoardGame.getExpansionKey(expansionId);
			Uri expansionUri = Uri.withAppendedPath(BoardGameExpansions.CONTENT_URI, "" + key);
			activity.getContentResolver().delete(expansionUri, null, null);
		}

		// collect poll list to delete
		Collection<Integer> oldPollIds = null;
		oldPollIds = oldBoardGame.getPollIds();
		for (int i = 0; i < boardGame.getPollCount(); i++) {
			Poll poll = boardGame.getPollByPosition(i);
			Poll oldPoll = oldBoardGame.getPollByName(poll.getName());

			int pollId;
			values.clear();
			values.put(BoardGamePolls.BOARDGAME_ID, boardGame.getGameId());
			values.put(BoardGamePolls.NAME, poll.getName());
			values.put(BoardGamePolls.TITLE, poll.getTitle());
			values.put(BoardGamePolls.VOTES, poll.getTotalVotes());
			if (oldPoll == null) {
				// insert
				uri = activity.getContentResolver().insert(BoardGamePolls.CONTENT_URI, values);
				pollId = Utility.parseInt(uri.getLastPathSegment());
			} else {
				// update
				pollId = oldPoll.getId();
				Uri pollUri = Uri.withAppendedPath(BoardGamePolls.CONTENT_URI, "" + pollId);
				activity.getContentResolver().update(pollUri, values, null, null);
				oldPollIds.remove(pollId);
			}

			// collect poll results list to delete
			Collection<Integer> oldResultsIds = null;
			if (oldPoll != null) {
				oldResultsIds = oldPoll.getResultsIds();
			}
			for (PollResults results : poll.getResultsList()) {
				int resultsId;
				PollResults oldPollResults = null;
				if (oldPoll != null) {
					oldPollResults = oldPoll.getResultsByPlayers(results.getNumberOfPlayers());
				}
				values.clear();
				values.put(BoardGamePollResults.POLL_ID, pollId);
				values.put(BoardGamePollResults.PLAYERS, results.getNumberOfPlayers());
				if (oldPollResults == null) {
					// insert
					uri = activity.getContentResolver().insert(BoardGamePollResults.CONTENT_URI, values);
					resultsId = Utility.parseInt(uri.getLastPathSegment());
				} else {
					// update
					resultsId = oldPollResults.getId();
					uri = Uri.withAppendedPath(BoardGamePollResults.CONTENT_URI, "" + resultsId);
					activity.getContentResolver().update(uri, values, null, null);
					oldResultsIds.remove(resultsId);
				}

				// collect poll result list to delete
				Collection<Integer> oldResultIds = null;
				if (oldPollResults != null) {
					oldResultIds = oldPollResults.getResultIds();
				}
				for (PollResult result : results.getResultList()) {
					PollResult oldPollResult = null;
					if (oldPollResults != null) {
						oldPollResult = oldPollResults.getResultByValue(result.getValue());
					}
					values.clear();
					values.put(BoardGamePollResult.POLLRESULTS_ID, resultsId);
					values.put(BoardGamePollResult.LEVEL, result.getLevel());
					values.put(BoardGamePollResult.VALUE, result.getValue());
					values.put(BoardGamePollResult.VOTES, result.getNumberOfVotes());
					if (oldPollResult == null) {
						// insert
						uri = activity.getContentResolver().insert(BoardGamePollResult.CONTENT_URI, values);
					} else {
						// update
						int resultId = oldPollResult.getId();
						uri = Uri.withAppendedPath(BoardGamePollResult.CONTENT_URI, "" + resultId);
						activity.getContentResolver().update(uri, values, null, null);
						oldResultIds.remove(resultId);
					}
				}
				// remove old results
				if (oldResultIds != null) {
					for (int oldResultId : oldResultIds) {
						uri = Uri.withAppendedPath(BoardGamePollResult.CONTENT_URI, "" + oldResultId);
						activity.getContentResolver().delete(uri, null, null);
					}
				}
			}
			// remove old resultses
			if (oldResultsIds != null) {
				for (int oldResultsId : oldResultsIds) {
					uri = Uri.withAppendedPath(BoardGamePollResults.CONTENT_URI, "" + oldResultsId);
					activity.getContentResolver().delete(uri, null, null);
				}
			}
		}
		// remove old polls
		if (oldPollIds != null) {
			for (int oldPollId : oldPollIds) {
				uri = Uri.withAppendedPath(BoardGamePolls.CONTENT_URI, "" + oldPollId);
				activity.getContentResolver().delete(uri, null, null);
			}
		}
	}

	private static ContentValues createBoardGameValues(BoardGame boardGame) {
		ContentValues values = new ContentValues();
		values.put(BoardGames._ID, boardGame.getGameId());
		values.put(BoardGames.NAME, boardGame.getName());
		values.put(BoardGames.SORT_INDEX, boardGame.getSortIndex());
		values.put(BoardGames.SORT_NAME, boardGame.getSortName());
		values.put(BoardGames.YEAR, boardGame.getYearPublished());
		values.put(BoardGames.MIN_PLAYERS, boardGame.getMinPlayers());
		values.put(BoardGames.MAX_PLAYERS, boardGame.getMaxPlayers());
		values.put(BoardGames.PLAYING_TIME, boardGame.getPlayingTime());
		values.put(BoardGames.AGE, boardGame.getAge());
		values.put(BoardGames.DESCRIPTION, boardGame.getDescription());
		values.put(BoardGames.THUMBNAIL_URL, boardGame.getThumbnailUrl());
		values.put(BoardGames.THUMBNAIL_ID, boardGame.getThumbnailId());
		values.put(BoardGames.RATING_COUNT, boardGame.getRatingCount());
		values.put(BoardGames.AVERAGE, boardGame.getAverage());
		values.put(BoardGames.BAYES_AVERAGE, boardGame.getBayesAverage());
		values.put(BoardGames.RANK, boardGame.getRank());
		values.put(BoardGames.RANK_ABSTRACT, boardGame.getRankAbstract());
		values.put(BoardGames.RANK_CCG, boardGame.getRankCcg());
		values.put(BoardGames.RANK_FAMILY, boardGame.getRankFamily());
		values.put(BoardGames.RANK_KIDS, boardGame.getRankKids());
		values.put(BoardGames.RANK_PARTY, boardGame.getRankParty());
		values.put(BoardGames.RANK_STRATEGY, boardGame.getRankStrategy());
		values.put(BoardGames.RANK_THEMATIC, boardGame.getRankTheme());
		values.put(BoardGames.RANK_WAR, boardGame.getRankWar());
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
		return values;
	}

	private static File getThumbnailFolder() {
		if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
			Log.w(LOG_TAG, "SD card not available");
			return null;
		}
		File folder = new File(Environment.getExternalStorageDirectory() + "/" + BoardGameGeekData.AUTHORITY
			+ "/thumbnails");
		return folder;
	}

	private static String getThumbnailPath(int thumbnailId, Boolean create) {

		String fileName = getThumbnailFileName(thumbnailId);
		if (TextUtils.isEmpty(fileName)) {
			Log.w(LOG_TAG, "thumbnail file name is empty");
			return null;
		}

		File folder = getThumbnailFolder();
		if (folder == null) {
			return null;
		}

		if (!folder.exists()) {
			if (create) {
				if (!folder.mkdirs()) {
					Log.w(LOG_TAG, "Folder can't be created");
					return null;
				}
			} else {
				Log.w(LOG_TAG, "Folder doesn't exist");
				return null;
			}
		}

		return folder.getAbsolutePath() + "/" + fileName;
	}

	public static String getThumbnailPath(int thumbnailId) {
		return getThumbnailPath(thumbnailId, false);
	}

	public static String getThumbnailPath(String thumbnailId) {
		return getThumbnailPath(Utility.parseInt(thumbnailId), false);
	}

	public static boolean deleteThumbnail(int thumbnailId) {
		boolean success = false;
		String fileName = getThumbnailPath(thumbnailId);
		if (!TextUtils.isEmpty(fileName)) {
			File file = new File(fileName);
			if (file != null && file.exists()) {
				// SecurityManager().checkDelete(fileName);
				success = file.delete();
			}
		}
		return success;
	}

	public static boolean deleteThumbnail(String thumbnailId) {
		return deleteThumbnail(Utility.parseInt(thumbnailId));
	}

	public static int deleteThumbnails() {
		int count = 0;
		File folder = getThumbnailFolder();
		if (folder != null && folder.listFiles() != null) {
			for (File file : folder.listFiles()) {
				if (file.delete()) {
					count++;
				}
			}
		}
		return count;
	}

	public static Boolean saveThumbnail(int thumbnailId, byte[] data) {
		FileOutputStream fos = null;
		String fileName = null;
		try {
			fileName = getThumbnailPath(thumbnailId, true);
			if (TextUtils.isEmpty(fileName)) {
				return false;
			}
			fos = new FileOutputStream(new File(fileName));
			Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
			bitmap.compress(CompressFormat.JPEG, 100, fos);
			fos.flush();
			return true;
		} catch (IOException e) {
			Log.e(LOG_TAG, "Error saving thumbnail", e);
			return false;
		} catch (NullPointerException e) {
			Log.e(LOG_TAG, "Bad fileName: " + fileName, e);
			return false;
		} finally {
			if (fos != null) {
				try {
					fos.close();
				} catch (IOException e) {}
			}
		}
	}

	public static int getThumbnailId(String thumbnailUrl) {
		if (TextUtils.isEmpty(thumbnailUrl))
			return 0;
		Uri uri = Uri.parse(thumbnailUrl);
		if (uri == null)
			return 0;
		String fileName = uri.getLastPathSegment();
		if (TextUtils.isEmpty(fileName))
			return 0;
		if (fileName.startsWith("pic")) {
			fileName = fileName.substring(3);
		}
		if (fileName.endsWith("_t.jpg")) {
			fileName = fileName.substring(0, fileName.length() - 6);
		}
		return Utility.parseInt(fileName);
	}

	private static String getThumbnailFileName(int id) {
		if (id == 0)
			return null;
		return "pic" + id + "_t.jpg";
	}
}
