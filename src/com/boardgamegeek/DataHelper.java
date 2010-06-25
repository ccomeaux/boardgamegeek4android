package com.boardgamegeek;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

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

public class DataHelper {

	private final static String LOG_TAG = "BoardGameGeek";

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
		boardGame
			.setStandardDeviation(cursor.getDouble(cursor.getColumnIndex(BoardGames.STANDARD_DEVIATION)));
		boardGame.setMedian(cursor.getDouble(cursor.getColumnIndex(BoardGames.MEDIAN)));
		boardGame.setOwnedCount(cursor.getInt(cursor.getColumnIndex(BoardGames.OWNED_COUNT)));
		boardGame.setTradingCount(cursor.getInt(cursor.getColumnIndex(BoardGames.TRADING_COUNT)));
		boardGame.setWantingCount(cursor.getInt(cursor.getColumnIndex(BoardGames.WANTING_COUNT)));
		boardGame.setWishingCount(cursor.getInt(cursor.getColumnIndex(BoardGames.WISHING_COUNT)));
		boardGame.setCommentCount(cursor.getInt(cursor.getColumnIndex(BoardGames.COMMENT_COUNT)));
		boardGame.setWeightCount(cursor.getInt(cursor.getColumnIndex(BoardGames.WEIGHT_COUNT)));
		boardGame.setAverageWeight(cursor.getInt(cursor.getColumnIndex(BoardGames.AVERAGE_WEIGHT)));

		boardGame.setThumbnail(Drawable.createFromPath(getThumbnailPath(boardGame.getGameId())));

		int gameId = boardGame.getGameId();

		cursor = activity.managedQuery(BoardGameDesigners.CONTENT_URI, new String[] {
			BoardGameDesigners.DESIGNER_ID, BoardGameDesigners.DESIGNER_NAME },
			BoardGameDesigners.BOARDGAME_ID + "=" + gameId, null, BoardGameDesigners.DESIGNER_NAME);
		boardGame.CreateDesigners(cursor);

		cursor = activity.managedQuery(BoardGameArtists.CONTENT_URI, new String[] {
			BoardGameArtists.ARTIST_ID, BoardGameArtists.ARTIST_NAME }, BoardGameArtists.BOARDGAME_ID + "="
			+ gameId, null, BoardGameArtists.ARTIST_NAME);
		boardGame.CreateArtists(cursor);

		cursor = activity.managedQuery(BoardGamePublishers.CONTENT_URI, new String[] {
			BoardGamePublishers.PUBLISHER_ID, BoardGamePublishers.PUBLISHER_NAME },
			BoardGamePublishers.BOARDGAME_ID + "=" + gameId, null, BoardGamePublishers.PUBLISHER_NAME);
		boardGame.CreatePublishers(cursor);

		cursor = activity.managedQuery(BoardGameCategories.CONTENT_URI, new String[] {
			BoardGameCategories.CATEGORY_ID, BoardGameCategories.CATEGORY_NAME },
			BoardGameCategories.BOARDGAME_ID + "=" + gameId, null, BoardGameCategories.CATEGORY_NAME);
		boardGame.CreateCategories(cursor);

		cursor = activity.managedQuery(BoardGameMechanics.CONTENT_URI, new String[] {
			BoardGameMechanics.MECHANIC_ID, BoardGameMechanics.MECHANIC_NAME },
			BoardGameMechanics.BOARDGAME_ID + "=" + gameId, null, BoardGameMechanics.MECHANIC_NAME);
		boardGame.CreateMechanics(cursor);

		cursor = activity.managedQuery(BoardGameExpansions.CONTENT_URI, new String[] {
			BoardGameExpansions.EXPANSION_ID, BoardGameExpansions.EXPANSION_NAME },
			BoardGameExpansions.BOARDGAME_ID + "=" + gameId, null, BoardGameExpansions.EXPANSION_NAME);
		boardGame.CreateExpanions(cursor);

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

		// delete to make sure all of the child relationships are cleaned up
		Uri uri = Uri.withAppendedPath(BoardGames.CONTENT_URI, "" + boardGame.getGameId());
		activity.getContentResolver().delete(uri, null, null);

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

		activity.getContentResolver().insert(BoardGames.CONTENT_URI, values);

		for (int i = 0; i < boardGame.getDesignerCount(); i++) {
			String designerId = boardGame.getDesignerIdByPosition(i);
			String designerName = boardGame.getDesignerNameById(designerId);

			values.clear();
			values.put(Designers._ID, designerId);
			values.put(Designers.NAME, designerName);

			// ensure designer record is present and correct
			Uri designerUri = Uri.withAppendedPath(Designers.CONTENT_URI, designerId);
			Cursor cursor = activity.managedQuery(designerUri, null, null, null, null);
			if (cursor.moveToFirst()) {
				if (designerName != cursor.getString(cursor.getColumnIndex(Designers.NAME))) {
					activity.getContentResolver().update(designerUri, values, null, null);
				}
			} else {
				activity.getContentResolver().insert(Designers.CONTENT_URI, values);
			}

			// add game/designer relationship record
			values.clear();
			values.put(BoardGameDesigners.BOARDGAME_ID, boardGame.getGameId());
			values.put(BoardGameDesigners.DESIGNER_ID, designerId);
			uri = activity.getContentResolver().insert(BoardGameDesigners.CONTENT_URI, values);
		}

		for (int i = 0; i < boardGame.getArtistCount(); i++) {
			String artistId = boardGame.getArtistIdByPosition(i);
			String artistName = boardGame.getArtistNameById(artistId);

			values.clear();
			values.put(Artists._ID, artistId);
			values.put(Artists.NAME, artistId);

			Uri artistUri = Uri.withAppendedPath(Artists.CONTENT_URI, artistId);
			Cursor cursor = activity.managedQuery(artistUri, null, null, null, null);
			if (cursor.moveToFirst()) {
				if (artistName != cursor.getString(cursor.getColumnIndex(Artists.NAME))) {
					activity.getContentResolver().update(artistUri, values, null, null);
				}
			} else {
				activity.getContentResolver().insert(Artists.CONTENT_URI, values);
			}

			// add game/artist relationship record
			values.clear();
			values.put(BoardGameArtists.BOARDGAME_ID, boardGame.getGameId());
			values.put(BoardGameArtists.ARTIST_ID, artistId);
			uri = activity.getContentResolver().insert(BoardGameArtists.CONTENT_URI, values);
		}

		for (int i = 0; i < boardGame.getPublisherCount(); i++) {
			String publisherId = boardGame.getPublisherIdByPosition(i);
			String publisherName = boardGame.getPublisherNameById(publisherId);

			values.clear();
			values.put(Publishers._ID, publisherId);
			values.put(Publishers.NAME, publisherName);

			// ensure publisher record is present and correct
			Uri publisherUri = Uri.withAppendedPath(Publishers.CONTENT_URI, publisherId);
			Cursor cursor = activity.managedQuery(publisherUri, null, null, null, null);
			if (cursor.moveToFirst()) {
				if (publisherName != cursor.getString(cursor.getColumnIndex(Publishers.NAME))) {
					activity.getContentResolver().update(publisherUri, values, null, null);
				}
			} else {
				activity.getContentResolver().insert(Publishers.CONTENT_URI, values);
			}

			// add game/Publisher relationship record
			values.clear();
			values.put(BoardGamePublishers.BOARDGAME_ID, boardGame.getGameId());
			values.put(BoardGamePublishers.PUBLISHER_ID, publisherId);
			uri = activity.getContentResolver().insert(BoardGamePublishers.CONTENT_URI, values);
		}

		for (int i = 0; i < boardGame.getCategoryCount(); i++) {
			String CategoryId = boardGame.getCategoryIdByPosition(i);
			String CategoryName = boardGame.getCategoryNameById(CategoryId);

			values.clear();
			values.put(Categories._ID, CategoryId);
			values.put(Categories.NAME, CategoryName);

			// ensure category record is present and correct
			Uri categoryUri = Uri.withAppendedPath(Categories.CONTENT_URI, CategoryId);
			Cursor cursor = activity.managedQuery(categoryUri, null, null, null, null);
			if (cursor.moveToFirst()) {
				if (CategoryName != cursor.getString(cursor.getColumnIndex(Categories.NAME))) {
					activity.getContentResolver().update(categoryUri, values, null, null);
				}
			} else {
				activity.getContentResolver().insert(Categories.CONTENT_URI, values);
			}

			// add game/category relationship record
			values.clear();
			values.put(BoardGameCategories.BOARDGAME_ID, boardGame.getGameId());
			values.put(BoardGameCategories.CATEGORY_ID, CategoryId);
			uri = activity.getContentResolver().insert(BoardGameCategories.CONTENT_URI, values);
		}

		for (int i = 0; i < boardGame.getMechanicCount(); i++) {
			String mechanicId = boardGame.getMechanicIdByPosition(i);
			String mechanicName = boardGame.getMechanicNameById(mechanicId);

			values.clear();
			values.put(Mechanics._ID, mechanicId);
			values.put(Mechanics.NAME, mechanicName);

			// ensure mechanic record is present and correct
			Uri mechanicUri = Uri.withAppendedPath(Mechanics.CONTENT_URI, mechanicId);
			Cursor cursor = activity.managedQuery(mechanicUri, null, null, null, null);
			if (cursor.moveToFirst()) {
				if (mechanicName != cursor.getString(cursor.getColumnIndex(Mechanics.NAME))) {
					activity.getContentResolver().update(mechanicUri, values, null, null);
				}
			} else {
				activity.getContentResolver().insert(Mechanics.CONTENT_URI, values);
			}

			// add game/mechanic relationship record
			values.clear();
			values.put(BoardGameMechanics.BOARDGAME_ID, boardGame.getGameId());
			values.put(BoardGameMechanics.MECHANIC_ID, mechanicId);
			uri = activity.getContentResolver().insert(BoardGameMechanics.CONTENT_URI, values);
		}

		for (int i = 0; i < boardGame.getExpansionCount(); i++) {
			int expansionId = boardGame.getExpansionIdByPosition(i);
			String expansionName = boardGame.getExpansionNameById(expansionId);

			values.clear();
			values.put(BoardGames._ID, expansionId);
			values.put(BoardGames.NAME, expansionName);

			// ensure expansion record is present and correct
			Uri expansionUri = Uri.withAppendedPath(BoardGames.CONTENT_URI, "" + expansionId);
			Cursor cursor = activity.managedQuery(expansionUri, null, null, null, null);
			if (cursor.moveToFirst()) {
				if (expansionName != cursor.getString(cursor.getColumnIndex(BoardGames.NAME))) {
					activity.getContentResolver().update(expansionUri, values, null, null);
				}
			} else {
				activity.getContentResolver().insert(BoardGames.CONTENT_URI, values);
			}

			// add game/expansion relationship record
			values.clear();
			values.put(BoardGameExpansions.BOARDGAME_ID, boardGame.getGameId());
			values.put(BoardGameExpansions.EXPANSION_ID, expansionId);
			uri = activity.getContentResolver().insert(BoardGameExpansions.CONTENT_URI, values);
		}

		for (int i = 0; i < boardGame.getPollCount(); i++) {
			Poll poll = boardGame.getPollByPosition(i);

			values.clear();
			values.put(BoardGamePolls.BOARDGAME_ID, boardGame.getGameId());
			values.put(BoardGamePolls.NAME, poll.getName());
			values.put(BoardGamePolls.TITLE, poll.getTitle());
			values.put(BoardGamePolls.VOTES, poll.getTotalVotes());
			uri = activity.getContentResolver().insert(BoardGamePolls.CONTENT_URI, values);

			String pollId = uri.getLastPathSegment();
			for (PollResults results : poll.getResultsList()) {
				values.clear();
				values.put(BoardGamePollResults.POLL_ID, pollId);
				values.put(BoardGamePollResults.PLAYERS, results.getNumberOfPlayers());
				uri = activity.getContentResolver().insert(BoardGamePollResults.CONTENT_URI, values);

				String resultsId = uri.getLastPathSegment();
				for (PollResult result : results.getResultList()) {
					values.clear();
					values.put(BoardGamePollResult.POLLRESULTS_ID, resultsId);
					values.put(BoardGamePollResult.LEVEL, result.getLevel());
					values.put(BoardGamePollResult.VALUE, result.getValue());
					values.put(BoardGamePollResult.VOTES, result.getNumberOfVotes());
					uri = activity.getContentResolver().insert(BoardGamePollResult.CONTENT_URI, values);
				}
			}
		}
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
		Log.d(LOG_TAG, "Getting thumbnail path");

		if (thumbnailId == 0) {
			Log.w(LOG_TAG, "thumbnailId is empty");
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

		return folder.getAbsolutePath() + "/" + thumbnailId + ".jpg";
	}

	public static String getThumbnailPath(String thumbnailId) {
		return getThumbnailPath(Utility.parseInt(thumbnailId));
	}

	public static String getThumbnailPath(int thumbnailId) {
		return getThumbnailPath(thumbnailId, false);
	}

	public static Boolean deleteThumbnail(int thumbnailId) {
		boolean success = false;
		if (thumbnailId > 0) {
			String fileName = getThumbnailPath(thumbnailId);
			if (!TextUtils.isEmpty(fileName)) {
				File file = new File(fileName);
				if (file.exists()) {
					// SecurityManager().checkDelete(fileName);
					success = file.delete();
				}
			}
		}
		return success;
	}

	public static int deleteThumbnails() {
		int count = 0;
		File folder = getThumbnailFolder();
		if (folder != null) {
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
			File file = new File(fileName);
			fos = new FileOutputStream(file);
			Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
			bitmap.compress(CompressFormat.JPEG, 100, fos);
			fos.flush();
			return true;
		} catch (IOException e) {
			Log.e(LOG_TAG, "Error saving thumbnail", e);
			return false;
		}catch (NullPointerException e){
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
}
