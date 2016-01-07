package com.boardgamegeek.tasks;

import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.text.TextUtils;
import android.widget.Toast;

import com.boardgamegeek.R;
import com.boardgamegeek.events.ColorAssignmentCompleteEvent;
import com.boardgamegeek.model.Play;
import com.boardgamegeek.model.Player;
import com.boardgamegeek.provider.BggContract.GameColors;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.provider.BggContract.PlayerColors;
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

	private static final int NO_MESSAGE = 0;

	@NonNull private final Random random;
	private final Context context;
	private final Play play;
	private List<String> colorsAvailable;
	private List<PlayerColorChoices> playersNeedingColor;
	@NonNull private final Results results;

	public ColorAssignerTask(Context context, Play play) {
		this.context = context;
		this.play = play;
		results = new Results();
		random = new Random();
	}

	@NonNull
	@Override
	protected Results doInBackground(Void... params) {
		int result = populatePlayersNeedingColor();
		if (result != SUCCESS) {
			results.resultCode = result;
			return results;
		}

		if (playersNeedingColor.size() == 0) {
			results.resultCode = ERROR_NO_PLAYERS;
			return results;
		}

		populateColorsAvailable();

		if (colorsAvailable.size() < playersNeedingColor.size()) {
			results.resultCode = ERROR_TOO_FEW_COLORS;
			return results;
		}

		populatePlayerColorChoices();

		boolean shouldContinue = true;
		while (shouldContinue && playersNeedingColor.size() > 0) {
			while (shouldContinue && playersNeedingColor.size() > 0) {
				shouldContinue = assignTopChoice();
			}
			shouldContinue = assignHighestChoice();
		}

		// assign a random player a random color
		while (playersNeedingColor.size() > 0) {
			String color = colorsAvailable.get(random.nextInt(colorsAvailable.size()));
			PlayerColorChoices username = playersNeedingColor.get(random.nextInt(playersNeedingColor.size()));
			assignColorToPlayer(color, username, "random");
		}

		results.resultCode = SUCCESS;
		return results;
	}

	@Override
	protected void onPostExecute(@Nullable Results results) {
		results = ensureResultsAreNotNull(results);
		if (results.resultCode == SUCCESS) {
			setPlayerColorsFromResults(results);
		}
		notifyCompletion(results, getMessageIdFromResults(results));
	}

	@NonNull
	private Results ensureResultsAreNotNull(@Nullable Results results) {
		if (results == null) {
			results = new Results();
			results.resultCode = ERROR;
		}
		return results;
	}

	private void setPlayerColorsFromResults(@NonNull Results results) {
		for (PlayerResult pr : this.results.results) {
			Player player = getPlayerFromResult(pr);
			if (player == null) {
				results.resultCode = ERROR_SOMETHING_CHANGED;
				break;
			} else {
				player.color = pr.color;
			}
		}
	}

	private int getMessageIdFromResults(@NonNull Results results) {
		@StringRes int messageId = NO_MESSAGE;
		if (results.hasError()) {
			messageId = R.string.title_error;
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
		}
		return messageId;
	}

	private void notifyCompletion(@NonNull Results results, @StringRes int messageId) {
		if (messageId != NO_MESSAGE) {
			// TODO - make this a snackbar
			Toast.makeText(context, messageId, Toast.LENGTH_LONG).show();
		}
		EventBus.getDefault().postSticky(new ColorAssignmentCompleteEvent(results.resultCode == SUCCESS));
	}

	/**
	 * Assigns a player their top color choice if no one else has that top choice as well.
	 *
	 * @return <code>true</code> if a color was assigned, <code>false</code> if not.
	 */
	private boolean assignTopChoice() {
		for (String colorToAssign : colorsAvailable) {
			PlayerColorChoices playerWhoWantThisColor = null;
			for (PlayerColorChoices player : playersNeedingColor) {
				ColorChoice currentPlayersTopChoice = player.getTopChoice();
				if (currentPlayersTopChoice != null) {
					if (colorToAssign.equals(currentPlayersTopChoice.color)) {
						if (playerWhoWantThisColor == null) {
							playerWhoWantThisColor = player;
						} else {
							playerWhoWantThisColor = null;
							break;
						}
					}
				}
			}
			if (playerWhoWantThisColor != null) {
				assignColorToPlayer(colorToAssign, playerWhoWantThisColor, "top choice");
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
		PlayerColorChoices currentPlayerWithHighestChoice = null;
		for (PlayerColorChoices player : playersNeedingColor) {
			ColorChoice newPlayersTopChoice = player.getTopChoice();
			if (newPlayersTopChoice != null) {
				if (currentPlayerWithHighestChoice == null ||
					(currentPlayerWithHighestChoice.getTopChoice().sortOrder > newPlayersTopChoice.sortOrder) ||
					((currentPlayerWithHighestChoice.getTopChoice().sortOrder > newPlayersTopChoice.sortOrder) && !currentPlayerWithHighestChoice.getTopChoice().color.equals(newPlayersTopChoice.color))) {
					currentPlayerWithHighestChoice = player;
				} else if ((currentPlayerWithHighestChoice.getTopChoice().sortOrder == newPlayersTopChoice.sortOrder) &&
					currentPlayerWithHighestChoice.getTopChoice().color.equals(newPlayersTopChoice.color)) {
					ColorChoice currentSecondChoice = player.getSecondChoice();
					ColorChoice savedSecondChoice = currentPlayerWithHighestChoice.getSecondChoice();
					//noinspection StatementWithEmptyBody
					if (currentSecondChoice == null && savedSecondChoice == null) {
						// TODO: random
					} else if (currentSecondChoice == null) {
						currentPlayerWithHighestChoice = player;
					} else //noinspection StatementWithEmptyBody
						if (savedSecondChoice == null) {
							// keep saved player
						} else if (currentSecondChoice.sortOrder > savedSecondChoice.sortOrder) {
							currentPlayerWithHighestChoice = player;
						}
				}
			}
		}
		if (currentPlayerWithHighestChoice != null) {
			assignColorToPlayer(currentPlayerWithHighestChoice.getTopChoice().color, currentPlayerWithHighestChoice, "highest choice");
			return true;
		}
		return false;
	}

	private int populatePlayersNeedingColor() {
		playersNeedingColor = new ArrayList<>();
		List<String> users = new ArrayList<>();
		List<String> nonusers = new ArrayList<>();
		for (Player player : play.getPlayers()) {
			if (TextUtils.isEmpty(player.color)) {
				if (TextUtils.isEmpty(player.username)) {
					if (TextUtils.isEmpty(player.name)) {
						return ERROR_MISSING_PLAYER_NAME;
					}
					if (nonusers.contains(player.name)) {
						return ERROR_DUPLICATE_PLAYER;
					}
					nonusers.add(player.name);
					playersNeedingColor.add(new PlayerColorChoices(player.name, TYPE_PLAYER_NON_USER));
				} else {
					if (users.contains(player.username)) {
						return ERROR_DUPLICATE_PLAYER;
					}
					users.add(player.username);
					playersNeedingColor.add(new PlayerColorChoices(player.username, TYPE_PLAYER_USER));
				}
			}
		}
		return SUCCESS;
	}

	private void populateColorsAvailable() {
		colorsAvailable = ResolverUtils.queryStrings(context.getContentResolver(), Games.buildColorsUri(play.gameId), GameColors.COLOR);
		for (Player player : play.getPlayers()) {
			if (!TextUtils.isEmpty(player.color)) {
				colorsAvailable.remove(player.color);
			}
		}
	}

	/**
	 * Populates the remaining players list with their color choices, limited to the colors remaining in the game.
	 */
	private void populatePlayerColorChoices() {
		for (PlayerColorChoices player : playersNeedingColor) {
			// TODO - support the other player type
			if (player.type == TYPE_PLAYER_USER) {
				Cursor cursor = null;
				try {
					cursor = context.getContentResolver().query(
						PlayerColors.buildUserUri(player.name),
						new String[] { PlayerColors.PLAYER_COLOR, PlayerColors.PLAYER_COLOR_SORT_ORDER },
						null, null, null);
					while (cursor != null && cursor.moveToNext()) {
						String color = cursor.getString(0);
						if (colorsAvailable.contains(color)) {
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
	 * Gets the player from the play based on the player result. Returns null if the player couldn't be found or their color is already set.
	 */
	private Player getPlayerFromResult(@NonNull PlayerResult pr) {
		for (Player player : play.getPlayers()) {
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
	 * from a for each loop without ending the iteration.
	 */
	private void assignColorToPlayer(@NonNull String color, @NonNull PlayerColorChoices player, String reason) {
		PlayerResult playerResult = new PlayerResult();
		playerResult.color = color;
		playerResult.name = player.name;
		playerResult.type = player.type;

		results.results.add(playerResult);
		colorsAvailable.remove(color);
		playersNeedingColor.remove(player);

		for (PlayerColorChoices pc : playersNeedingColor) {
			pc.removeChoice(color);
		}

		Timber.i("Assigned " + playerResult + ": " + reason);
	}

	public class Results {
		int resultCode;
		final List<PlayerResult> results = new ArrayList<>();

		public boolean hasError() {
			return resultCode < 0;
		}
	}

	public class PlayerResult {
		String name;
		int type;
		String color;

		@NonNull
		@Override
		public String toString() {
			return name + " (" + type + ") - " + color;
		}
	}

	private class PlayerColorChoices {
		final String name;
		final int type;
		@NonNull final List<ColorChoice> colors;

		public PlayerColorChoices(String name, int type) {
			this.name = name;
			this.type = type;
			this.colors = new ArrayList<>();
		}

		/**
		 * Gets the player's top remaining color choice, or <code>null</code> if they have no choices left.
		 */
		@Nullable
		public ColorChoice getTopChoice() {
			if (colors.size() > 0) {
				return colors.get(0);
			}
			return null;
		}

		/**
		 * Gets the player's second remaining color choice, or <code>null</code> if they have fewer than 2 choices left.
		 */
		@Nullable
		public ColorChoice getSecondChoice() {
			if (colors.size() > 1) {
				return colors.get(1);
			}
			return null;
		}

		public boolean removeChoice(@NonNull String color) {
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

		@NonNull
		@Override
		public String toString() {
			return "#" + sortOrder + ": " + color;
		}
	}
}
