package com.boardgamegeek.tasks;

import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.text.TextUtils;

import com.boardgamegeek.R;
import com.boardgamegeek.events.ColorAssignmentCompleteEvent;
import com.boardgamegeek.model.Play;
import com.boardgamegeek.model.Player;
import com.boardgamegeek.provider.BggContract.GameColors;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.provider.BggContract.PlayerColors;
import com.boardgamegeek.tasks.ColorAssignerTask.Results;
import com.boardgamegeek.util.ResolverUtils;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import hugo.weaving.DebugLog;
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

	@NonNull private final Random random;
	private final Context context;
	private final Play play;
	private List<String> colorsAvailable;
	private List<PlayerColorChoices> playersNeedingColor;
	@NonNull private final Results results;
	private int round;

	@DebugLog
	public ColorAssignerTask(Context context, Play play) {
		this.context = context;
		this.play = play;
		results = new Results();
		random = new Random();
	}

	@DebugLog
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

		round = 1;
		boolean shouldContinue = true;
		while (shouldContinue && playersNeedingColor.size() > 0) {
			while (shouldContinue && playersNeedingColor.size() > 0) {
				shouldContinue = assignTopChoice();
			}
			shouldContinue = assignMostPreferredChoice();
			round++;
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

	@DebugLog
	@Override
	protected void onPostExecute(@Nullable Results results) {
		results = ensureResultsAreNotNull(results);
		if (results.resultCode == SUCCESS) {
			setPlayerColorsFromResults(results);
		}
		notifyCompletion(results, getMessageIdFromResults(results));
	}

	@DebugLog
	@NonNull
	private Results ensureResultsAreNotNull(@Nullable Results results) {
		if (results == null) {
			results = new Results();
			results.resultCode = ERROR;
		}
		return results;
	}

	@DebugLog
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

	@DebugLog
	private int getMessageIdFromResults(@NonNull Results results) {
		@StringRes int messageId = R.string.msg_color_success;
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

	@DebugLog
	private void notifyCompletion(@NonNull Results results, @StringRes int messageId) {
		EventBus.getDefault().postSticky(new ColorAssignmentCompleteEvent(results.resultCode == SUCCESS, messageId));
	}

	/**
	 * Assigns a player their top color choice if no one else has that top choice as well.
	 *
	 * @return <code>true</code> if a color was assigned, <code>false</code> if not.
	 */
	@DebugLog
	private boolean assignTopChoice() {
		for (String colorToAssign : colorsAvailable) {
			PlayerColorChoices playerWhoWantsThisColor = getLonePlayerWithTopChoice(colorToAssign);
			if (playerWhoWantsThisColor != null) {
				assignColorToPlayer(colorToAssign, playerWhoWantsThisColor, "top choice");
				return true;
			}
		}
		Timber.i("No more players have a unique top choice in round %d", round);
		return false;
	}

	@DebugLog
	@Nullable
	private PlayerColorChoices getLonePlayerWithTopChoice(String colorToAssign) {
		List<PlayerColorChoices> players = getPlayersWithTopChoice(colorToAssign);
		if (players.size() == 0) {
			Timber.i("No players want %s as their top choice", colorToAssign);
			return null;
		} else if (players.size() > 1) {
			Timber.i("Multiple players want %s as their top choice", colorToAssign);
			return null;
		}
		return players.get(0);
	}

	@DebugLog
	@NonNull
	private List<PlayerColorChoices> getPlayersWithTopChoice(String colorToAssign) {
		List<PlayerColorChoices> players = new ArrayList<>();
		for (PlayerColorChoices player : playersNeedingColor) {
			if (player.isTopChoice(colorToAssign)) {
				players.add(player);
			}
		}
		return players;
	}

	@DebugLog
	private boolean assignMostPreferredChoice() {
		List<PlayerColorChoices> players = new ArrayList<>();
		double maxPreference = 0.0;
		for (String color : colorsAvailable) {
			List<PlayerColorChoices> playersWithTopChoice = getPlayersWithTopChoice(color);
			if (playersWithTopChoice.size() > 1) {
				for (PlayerColorChoices player : playersWithTopChoice) {
					double preference = player.calculateCurrentPreferenceFor(color);
					Timber.i("%s wants %s: %,.2f", player.name, color, preference);
					if (preference > maxPreference) {
						maxPreference = preference;
						players.clear();
						players.add(player);
					} else if (preference == maxPreference) {
						players.add(player);
					}
				}
			} else {
				Timber.i("Not enough players want %s", color);
			}
		}

		if (players.size() == 0) {
			Timber.i("Nobody wants any color");
			return false;
		}
		if (players.size() == 1) {
			PlayerColorChoices player = players.get(0);
			final ColorChoice topChoice = player.getTopChoice();
			if (topChoice != null) {
				assignColorToPlayer(topChoice.color, player, String.format("most preferred (%,.2f)", maxPreference));
				return true;
			}
		} else {
			int i = random.nextInt(players.size());
			PlayerColorChoices player = players.get(i);
			final ColorChoice topChoice = player.getTopChoice();
			if (topChoice != null) {
				assignColorToPlayer(player.getTopChoice().color, player, String.format("most preferred, but randomly chosen in a tie breaker (%,.2f)", maxPreference));
				return true;
			}
		}
		Timber.i("Something went horribly wrong");
		return false;
	}

	@DebugLog
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

	@DebugLog
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
	@DebugLog
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
					Timber.w(e, "Couldn't get the colors for %s", player);
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
	@DebugLog
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
	@DebugLog
	private void assignColorToPlayer(@NonNull String color, @NonNull PlayerColorChoices player, String reason) {
		PlayerResult playerResult = new PlayerResult(player.name, player.type, color, reason);
		results.results.add(playerResult);
		Timber.i("Assigned %s", playerResult);

		colorsAvailable.remove(color);
		playersNeedingColor.remove(player);

		for (PlayerColorChoices playerColorChoices : playersNeedingColor) {
			playerColorChoices.removeChoice(color);
		}
	}

	public class Results {
		int resultCode;
		final List<PlayerResult> results = new ArrayList<>();

		public boolean hasError() {
			return resultCode < 0;
		}

		@Override
		public String toString() {
			return String.format("%1$s - %2$s", resultCode, results.size());
		}
	}

	public class PlayerResult {
		final String name;
		final int type;
		final String color;
		final String reason;

		public PlayerResult(String name, int type, String color, String reason) {
			this.name = name;
			this.type = type;
			this.color = color;
			this.reason = reason;
		}

		@NonNull
		@Override
		public String toString() {
			return String.format("%1$s - %3$s (%4$s in round %5$d)", name, type, color, reason, round);
		}
	}

	private class PlayerColorChoices {
		final String name;
		final int type;
		@NonNull final List<ColorChoice> colors;

		@DebugLog
		public PlayerColorChoices(String name, int type) {
			this.name = name;
			this.type = type;
			this.colors = new ArrayList<>();
		}

		@DebugLog
		public boolean isTopChoice(String color) {
			return colors.size() > 0 && colors.get(0).color.equals(color);
		}

		/**
		 * Gets the player's top remaining color choice, or <code>null</code> if they have no choices left.
		 */
		@DebugLog
		@Nullable
		public ColorChoice getTopChoice() {
			if (colors.size() > 0) {
				return colors.get(0);
			}
			return null;
		}

		@DebugLog
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

		@DebugLog
		public double calculateCurrentPreferenceFor(@NonNull String color) {
			int MAX_PREFERENCE = 100;
			if (colors.size() == 0) {
				return 0;
			} else if (colors.size() == 1) {
				return MAX_PREFERENCE - colors.get(0).sortOrder;
			}

			int total = 0;
			int current = 0;
			for (ColorChoice colorChoice : colors) {
				total += colorChoice.sortOrder;
				if (color.equals(colorChoice.color)) {
					current = colorChoice.sortOrder;
				}
			}
			double expectedValue = ((double) total) / colors.size();
			double expectedValueWithoutColor = ((double) (total - current)) / (colors.size() - 1);
			return expectedValueWithoutColor - expectedValue;
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
