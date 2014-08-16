package com.boardgamegeek.provider;

import android.net.Uri;

import com.boardgamegeek.provider.BggContract.PlayItems;
import com.boardgamegeek.provider.BggContract.PlayPlayers;
import com.boardgamegeek.provider.BggContract.Plays;
import com.boardgamegeek.provider.BggDatabase.Tables;
import com.boardgamegeek.util.SelectionBuilder;

public class PlaysPlayersProvider extends BaseProvider {

	@Override
	protected SelectionBuilder buildSimpleSelection(Uri uri) {
		return new SelectionBuilder().table(Tables.PLAY_PLAYERS);
	}

	@Override
	protected SelectionBuilder buildExpandedSelection(Uri uri) {
		SelectionBuilder builder = new SelectionBuilder();
		String groupBy = uri.getQueryParameter(BggContract.QUERY_KEY_GROUP_BY);
		if (BggContract.QUERY_VALUE_NAME_NOT_USER.equals(groupBy)) {
			builder = new SelectionBuilder().table(Tables.PLAY_PLAYERS).groupBy(PlayPlayers.NAME)
				.whereEqualsOrNull(PlayPlayers.USER_NAME, "");
		} else if (BggContract.QUERY_VALUE_UNIQUE_NAME.equals(groupBy)) {
			builder = new SelectionBuilder().table(Tables.PLAY_PLAYERS_JOIN_PLAYS)
				.mapToTable(Plays._ID, Tables.PLAY_PLAYERS).mapToTable(Plays.PLAY_ID, Tables.PLAY_PLAYERS)
				.where(PlayPlayers.NAME + "!= '' OR " + PlayPlayers.USER_NAME + "!=''")
				.groupBy(PlayPlayers.UNIQUE_NAME);
		} else if (BggContract.QUERY_VALUE_UNIQUE_PLAYER.equals(groupBy)) {
			builder = new SelectionBuilder().table(Tables.PLAY_PLAYERS_JOIN_PLAYS)
				.mapToTable(Plays._ID, Tables.PLAY_PLAYERS).mapToTable(Plays.PLAY_ID, Tables.PLAY_PLAYERS)
				.where(PlayPlayers.NAME + "!= '' OR " + PlayPlayers.USER_NAME + "!=''")
				.groupBy(PlayPlayers.NAME + "," + PlayPlayers.USER_NAME);
		} else if (BggContract.QUERY_VALUE_UNIQUE_USER.equals(groupBy)) {
			builder = new SelectionBuilder().table(Tables.PLAY_PLAYERS_JOIN_PLAYS)
				.mapToTable(Plays._ID, Tables.PLAY_PLAYERS).mapToTable(Plays.PLAY_ID, Tables.PLAY_PLAYERS)
				.where(PlayPlayers.USER_NAME + "!=''").groupBy(PlayPlayers.USER_NAME);
		} else {
			builder = new SelectionBuilder().table(Tables.PLAY_PLAYERS_JOIN_PLAYS_JOIN_ITEMS)
				.mapToTable(Plays._ID, Tables.PLAYS).mapToTable(Plays.PLAY_ID, Tables.PLAYS)
				.mapToTable(PlayItems.NAME, Tables.PLAY_ITEMS).groupBy(Plays.PLAY_ID);
		}
		builder.map(PlayPlayers.COUNT, "count(*)").map(PlayPlayers.UNIQUE_NAME, "IFNULL(NULLIF(user_name,''), name)")
			.map(PlayPlayers.DESCRIPTION, "name || IFNULL(NULLIF(' ('||user_name||')', ' ()'), '')");
		return builder;
	}

	@Override
	protected String getDefaultSortOrder() {
		return Plays.DATE + " DESC, " + PlayPlayers.DEFAULT_SORT;
	}

	@Override
	protected String getPath() {
		return "plays/players";
	}
}
