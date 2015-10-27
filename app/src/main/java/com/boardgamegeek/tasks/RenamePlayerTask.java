package com.boardgamegeek.tasks;

import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.widget.Toast;

import com.boardgamegeek.R;
import com.boardgamegeek.model.Play;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.provider.BggContract.PlayPlayers;
import com.boardgamegeek.provider.BggContract.Plays;
import com.boardgamegeek.service.SyncService;
import com.boardgamegeek.util.ResolverUtils;

import java.util.ArrayList;
import java.util.List;

import de.greenrobot.event.EventBus;

/**
 * Change a player name (either a GeekBuddy or named player), updating and syncing all plays.
 */
public class RenamePlayerTask extends AsyncTask<Void, Void, String> {
	private static final String SELECTION_WITHOUT_USERNAME =
		"play_players." + PlayPlayers.NAME + "=? AND (" + PlayPlayers.USER_NAME + "=? OR " + PlayPlayers.USER_NAME + " IS NULL)";
	private static final String SELECTION_WITH_USERNAME =
		"play_players." + PlayPlayers.NAME + "=? AND " + PlayPlayers.USER_NAME + "=?";

	private final Context mContext;
	private final String mUsername;
	private final String mOldName;
	private final String mNewName;

	public RenamePlayerTask(@NonNull Context context, String username, String oldName, String newName) {
		mContext = context.getApplicationContext();
		mUsername = username;
		mOldName = oldName;
		mNewName = newName;
	}

	@NonNull
	@Override
	protected String doInBackground(Void... params) {
		ArrayList<ContentProviderOperation> batch = new ArrayList<>();
		String selection = TextUtils.isEmpty(mUsername) ? SELECTION_WITHOUT_USERNAME : SELECTION_WITH_USERNAME;

		List<Integer> playIds = ResolverUtils.queryInts(mContext.getContentResolver(),
			Plays.buildPlayersByPlayUri(),
			Plays.PLAY_ID, Plays.SYNC_STATUS + "=? AND (" + selection + ")",
			TextUtils.isEmpty(mUsername) ?
				new String[] { String.valueOf(Play.SYNC_STATUS_SYNCED), mOldName, "" } :
				new String[] { String.valueOf(Play.SYNC_STATUS_SYNCED), mOldName, mUsername });
		if (playIds.size() > 0) {
			ContentValues values = new ContentValues();
			values.put(Plays.SYNC_STATUS, Play.SYNC_STATUS_PENDING_UPDATE);
			for (Integer playId : playIds) {
				if (playId != BggContract.INVALID_ID) {
					Uri uri = Plays.buildPlayUri(playId);
					batch.add(ContentProviderOperation.newUpdate(uri).withValues(values).build());
				}
			}
		}

		batch.add(ContentProviderOperation
			.newUpdate(Plays.buildPlayersByPlayUri())
			.withValue(PlayPlayers.NAME, mNewName)
			.withSelection(selection,
				TextUtils.isEmpty(mUsername) ? new String[] { mOldName, "" } : new String[] { mOldName, mUsername })
			.build());

		ResolverUtils.applyBatch(mContext, batch);

		SyncService.sync(mContext, SyncService.FLAG_SYNC_PLAYS_UPLOAD);

		return mContext.getString(R.string.msg_play_player_change, mOldName, mNewName);
	}

	@Override
	protected void onPostExecute(String result) {
		if (!TextUtils.isEmpty(result)) {
			Toast.makeText(mContext, result, Toast.LENGTH_LONG).show();
		}

		EventBus.getDefault().post(new Event(mNewName));
	}

	public class Event {
		public final String playerName;

		public Event(String playerName) {
			this.playerName = playerName;
		}
	}

}
