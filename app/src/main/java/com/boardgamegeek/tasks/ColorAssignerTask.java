package com.boardgamegeek.tasks;

import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.widget.Toast;

import com.boardgamegeek.R;
import com.boardgamegeek.events.ColorAssignmentCompleteEvent;
import com.boardgamegeek.model.Play;
import com.boardgamegeek.model.Player;
import com.boardgamegeek.provider.BggContract.GameColors;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.tasks.ColorAssignerTask.Results;
import com.boardgamegeek.util.ResolverUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import de.greenrobot.event.EventBus;
import timber.log.Timber;

public class ColorAssignerTask extends AsyncTask<Void, Void, Results> {
	private static final int SUCCESS = 1;
	private static final int ERROR = -1;
	private static final int ERROR_NO_PLAYERS = -2;
	private static final int ERROR_MISSING_PLAYER_NAME = -3;
	private static final int ERROR_TOO_FEW_COLORS = -4;
	private static final int ERROR_DUPLICATE_PLAYER = -5;
	private static final int ERROR_SOMETHING_CHANGED = -99;

	private static final int TYPE_PLAYER_USER = 1;
	private static final int TYPE_PLAYER_NON_USER = 2;

	private final Random mRandom;
	private final Context mContext;
	private final Play mPlay;
	private List<String> mRemainingColors;
	private List<PlayerColors> mRemainingPlayers;
	private final Results mResults;

	public ColorAssignerTask(Context context, Play play) {
		mContext = context;
		mPlay = play;
		mResults = new Results();
		mRandom = new Random();
	}

	@Override
	protected Results doInBackground(Void... params) {
		// extract list of players
		int result = extractPlayers();
		if (result != SUCCESS) {
			mResults.resultCode = result;
			return mResults;
		}

		if (mRemainingPlayers.size() == 0) {
			mResults.resultCode = ERROR_NO_PLAYERS;
			return mResults;
		}

		mRemainingColors = ResolverUtils.queryStrings(mContext.getContentResolver(), Games.buildColorsUri(mPlay.gameId), GameColors.COLOR);
		for (Player player : mPlay.getPlayers()) {
			if (!TextUtils.isEmpty(player.color)) {
				mRemainingColors.remove(player.color);
			}
		}

		if (mRemainingColors.size() < mRemainingPlayers.size()) {
			mResults.resultCode = ERROR_TOO_FEW_COLORS;
			return mResults;
		}

		populatePlayerColorChoices();

		boolean shouldContinue = true;
		while (shouldContinue && mRemainingPlayers.size() > 0) {
			while (shouldContinue && mRemainingPlayers.size() > 0) {
				shouldContinue = assignTopChoice();
			}
			shouldContinue = assignHighestChoice();
		}

		// assign a random player a random color
		while (mRemainingPlayers.size() > 0) {
			String color = mRemainingColors.get(mRandom.nextInt(mRemainingColors.size()));
			PlayerColors username = mRemainingPlayers.get(mRandom.nextInt(mRemainingPlayers.size()));
			assignColorToPlayer(color, username, "random");
		}

		mResults.resultCode = SUCCESS;
		return mResults;
	}

	@Override
	protected void onPostExecute(Results results) {
		if (results == null) {
			results = new Results();
			results.resultCode = ERROR;
		}

		if (results.resultCode == SUCCESS) {
			for (PlayerResult pr : mResults.results) {
				Player player = getPlayerFromResult(pr);
				if (player == null) {
					results.resultCode = ERROR_SOMETHING_CHANGED;
					break;
				} else {
					player.color = pr.color;
				}
			}
		}

		if (results.resultCode < 0) {
			int messageId = R.string.title_error;
			switch (results.resultCode) {
				case ERROR_NO_PLAYERS:
					messageId = R.string.msg_color_error_no_players;
					break;
				case ERROR_DUPLICATE_PLAYER:
					messageId = R.string.msg_color_error_duplicate_player;
					break;
				case ERROR_MISSING_PLAYER_NAME:
					messageId = R.string.msg_color_error_missing_player_name;
					break;
				case ERROR_SOMETHING_CHANGED:
					messageId = R.string.msg_color_error_something_changed;
					break;
				case ERROR_TOO_FEW_COLORS:
					messageId = R.string.msg_color_error_too_few_colors;
					break;
			}
			Toast.makeText(mContext, messageId, Toast.LENGTH_LONG).show();
		}

		EventBus.getDefault().postSticky(new ColorAssignmentCompleteEvent(results.resultCode == SUCCESS));
	}

	/**
	 * Populates the remaining players list with their color choices, limited to the colors remaining in the game.
	 */
	private void populatePlayerColorChoices() {
		for (PlayerColors player : mRemainingPlayers) {
			if (player.type == TYPE_PLAYER_USER) {
				Cursor cursor = null;
				try {
					cursor = mContext.getContentResolver().query(
						com.boardgamegeek.provider.BggContract.PlayerColors.buildUserUri(player.name),
						new String[] { com.boardgamegeek.provider.BggContract.PlayerColors.PLAYER_COLOR, com.boardgamegeek.provider.BggContract.PlayerColors.PLAYER_COLOR_SORT_ORDER },
						null, null, null);
					while (cursor.moveToNext()) {
						String color = cursor.getString(0);
						if (mRemainingColors.contains(color)) {
							player.colors.add(new ColorChoice(color, cursor.getInt(1)));
						}
					}
				} catch (Exception e) {
					Timber.w(e, "Couldn't get the colors for " + player);
				} finally {
					if (cursor != null) {
						cursor.close();
					}
				}
			}
		}
	}

	/**
	 * Assigns a player their top color choice if no one else has that top choice as well.
	 *
	 * @return <code>true</code> if a color was assigned, <code>false</code> if not.
	 */
	private boolean assignTopChoice() {
		for (String color : mRemainingColors) {
			PlayerColors savedPlayer = null;
			for (PlayerColors player : mRemainingPlayers) {
				ColorChoice topChoice = player.getTopChoice();
				if (topChoice != null) {
					if (color.equals(topChoice.color)) {
						if (savedPlayer == null) {
							savedPlayer = player;
						} else {
							savedPlayer = null;
							break;
						}
					}
				}
			}
			if (savedPlayer != null) {
				assignColorToPlayer(color, savedPlayer, "top choice");
				return true;
			}
		}
		Timber.i("No player has a unique top choice");
		return false;
	}

	/**
	 * Assigns a color to a player who has the highest choice choice remaining.
	 */
	private boolean assignHighestChoice() {
		PlayerColors savedPlayer = null;
		for (PlayerColors player : mRemainingPlayers) {
			ColorChoice currentTopChoice = player.getTopChoice();
			if (currentTopChoice != null) {
				if (savedPlayer == null ||
					(savedPlayer.getTopChoice().sortOrder > currentTopChoice.sortOrder) ||
					((savedPlayer.getTopChoice().sortOrder > currentTopChoice.sortOrder) && !savedPlayer.getTopChoice().color.equals(currentTopChoice.color))) {
					savedPlayer = player;
				} else if ((savedPlayer.getTopChoice().sortOrder == currentTopChoice.sortOrder) &&
					savedPlayer.getTopChoice().color.equals(currentTopChoice.color)) {
					ColorChoice currentSecondChoice = player.getSecondChoice();
					ColorChoice savedSecondChoice = savedPlayer.getSecondChoice();
					if (currentSecondChoice == null && savedSecondChoice == null) {
						// TODO: random
					} else if (currentSecondChoice == null) {
						savedPlayer = player;
					} else if (savedSecondChoice == null) {
						// else keep saved player
					} else if (currentSecondChoice.sortOrder > savedSecondChoice.sortOrder) {
						savedPlayer = player;
					}
				}
			}
		}
		if (savedPlayer != null) {
			assignColorToPlayer(savedPlayer.getTopChoice().color, savedPlayer, "highest choice");
			return true;
		}
		return false;
	}

	/**
	 * Extract players from the play into the local list of remaining players.
	 *
	 * @return status code
	 */
	private int extractPlayers() {
		mRemainingPlayers = new ArrayList<>();
		List<String> users = new ArrayList<>();
		List<String> nonusers = new ArrayList<>();
		for (Player player : mPlay.getPlayers()) {
			if (TextUtils.isEmpty(player.color)) {
				if (TextUtils.isEmpty(player.username)) {
					if (TextUtils.isEmpty(player.name)) {
						return ERROR_MISSING_PLAYER_NAME;
					}
					if (nonusers.contains(player.name)) {
						return ERROR_DUPLICATE_PLAYER;
					}
					nonusers.add(player.name);
					mRemainingPlayers.add(new PlayerColors(player.name, TYPE_PLAYER_NON_USER));
				} else {
					if (users.contains(player.username)) {
						return ERROR_DUPLICATE_PLAYER;
					}
					users.add(player.username);
					mRemainingPlayers.add(new PlayerColors(player.username, TYPE_PLAYER_USER));
				}
			}
		}
		return SUCCESS;
	}

	/**
	 * Gets the player from the play based on the player result. Returns null if the player couldn't be found or their color is already set.
	 */
	private Player getPlayerFromResult(PlayerResult pr) {
		for (Player player : mPlay.getPlayers()) {
			if ((pr.type == TYPE_PLAYER_USER && pr.name.equals(player.username)) ||
				pr.type == TYPE_PLAYER_NON_USER && pr.name.equals(player.name) && TextUtils.isEmpty(player.username)) {
				if (TextUtils.isEmpty(player.color)) {
					return player;
				}
			}
		}
		return null;
	}

	/**
	 * Assign a color to a player, and remove both from the list of remaining colors and players. This can't be called
	 * from a for each loop without ending the ending the iteration.
	 */
	private void assignColorToPlayer(String color, PlayerColors player, String reason) {
		PlayerResult pr = new PlayerResult();
		pr.color = color;
		pr.name = player.name;
		pr.type = player.type;

		mResults.results.add(pr);
		mRemainingColors.remove(color);
		mRemainingPlayers.remove(player);

		for (PlayerColors pc : mRemainingPlayers) {
			pc.removeChoice(color);
		}

		Timber.i("Assigned " + pr + ": " + reason);
	}

	public class Results {
		int resultCode;
		final List<PlayerResult> results = new ArrayList<>();
	}

	public class PlayerResult {
		String name;
		int type;
		String color;

		@Override
		public String toString() {
			return name + " (" + type + ") - " + color;
		}
	}

	private class PlayerColors {
		final String name;
		final int type;
		final List<ColorChoice> colors;

		public PlayerColors(String name, int type) {
			this.name = name;
			this.type = type;
			this.colors = new ArrayList<>();
		}

		/**
		 * Gets the player's top remaining color choice, or <code>null</code> if they have no choices left.
		 */
		public ColorChoice getTopChoice() {
			if (colors.size() > 0) {
				return colors.get(0);
			}
			return null;
		}

		/**
		 * Gets the player's second remaining color choice, or <code>null</code> if they have fewer than 2 choices left.
		 */
		public ColorChoice getSecondChoice() {
			if (colors.size() > 1) {
				return colors.get(1);
			}
			return null;
		}

		public boolean removeChoice(String color) {
			if (TextUtils.isEmpty(color)) {
				return false;
			}
			for (ColorChoice colorChoice : colors) {
				if (color.equals(colorChoice.color)) {
					colors.remove(colorChoice);
					return true;
				}
			}
			return false;
		}

		@Override
		public String toString() {
			String s = name + " (" + type + ")";
			if (colors.size() > 0) {
				s += " [ ";
				boolean prependComma = false;
				for (ColorChoice color : colors) {
					if (prependComma) {
						s += ", ";
					}
					s += color;
					prependComma = true;
				}
				s += " ]";
			}
			return s;
		}
	}

	private class ColorChoice {
		final String color;
		final int sortOrder;

		public ColorChoice(String color, int sortOrder) {
			this.color = color;
			this.sortOrder = sortOrder;
		}

		@Override
		public String toString() {
			return "#" + sortOrder + ": " + color;
		}
	}
}
