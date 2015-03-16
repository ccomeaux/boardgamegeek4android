package com.boardgamegeek.ui;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Chronometer;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.model.Play;
import com.boardgamegeek.model.Player;
import com.boardgamegeek.model.builder.PlayBuilder;
import com.boardgamegeek.model.persister.PlayPersister;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.provider.BggContract.PlayItems;
import com.boardgamegeek.provider.BggContract.PlayPlayers;
import com.boardgamegeek.provider.BggContract.Plays;
import com.boardgamegeek.service.UpdateService;
import com.boardgamegeek.ui.widget.PlayerRow;
import com.boardgamegeek.util.ActivityUtils;
import com.boardgamegeek.util.DateTimeUtils;
import com.boardgamegeek.util.DetachableResultReceiver;
import com.boardgamegeek.util.DialogUtils;
import com.boardgamegeek.util.ImageUtils;
import com.boardgamegeek.util.NotificationUtils;
import com.boardgamegeek.util.PreferencesUtils;
import com.boardgamegeek.util.UIUtils;

import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;

public class PlayFragment extends ListFragment implements LoaderManager.LoaderCallbacks<Cursor> {
	private static final int AGE_IN_DAYS_TO_REFRESH = 7;
	private static final String KEY_NOTIFIED = "NOTIFIED";

	private int mPlayId = BggContract.INVALID_ID;
	private Play mPlay = new Play();
	private String mThumbnailUrl;
	private String mImageUrl;

	@InjectView(R.id.thumbnail) ImageView mThumbnailView;
	@InjectView(R.id.header) TextView mGameName;
	@InjectView(R.id.play_date) TextView mDate;
	@InjectView(R.id.play_quantity) TextView mQuantity;
	@InjectView(R.id.length_root) View mLengthRoot;
	@InjectView(R.id.play_length) TextView mLength;
	@InjectView(R.id.timer_root) View mTimerRoot;
	@InjectView(R.id.timer) Chronometer mTimer;
	@InjectView(R.id.location_root) View mLocationRoot;
	@InjectView(R.id.play_location) TextView mLocation;
	@InjectView(R.id.play_incomplete) View mIncomplete;
	@InjectView(R.id.play_no_win_stats) View mNoWinStats;
	@InjectView(R.id.play_comments) TextView mComments;
	@InjectView(R.id.play_comments_label) View mCommentsLabel;
	@InjectView(R.id.play_players_label) View mPlayersLabel;
	@InjectView(R.id.updated) TextView mUpdated;
	@InjectView(R.id.play_id) TextView mPlayIdView;
	@InjectView(R.id.play_saved) TextView mSavedTimeStamp;
	@InjectView(R.id.play_unsynced_message) TextView mUnsyncedMessage;
	private PlayerAdapter mAdapter;
	private DetachableResultReceiver mReceiver;
	private boolean mNotified;

	public interface Callbacks {
		public void onNameChanged(String mGameName);

		public void onSent();

		public void onDeleted();
	}

	private static Callbacks sDummyCallbacks = new Callbacks() {
		@Override
		public void onNameChanged(String gameName) {
		}

		@Override
		public void onSent() {
		}

		@Override
		public void onDeleted() {
		}
	};

	private Callbacks mCallbacks = sDummyCallbacks;

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);

		if (!(activity instanceof Callbacks)) {
			throw new ClassCastException("Activity must implement fragment's callbacks.");
		}

		mCallbacks = (Callbacks) activity;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (savedInstanceState != null) {
			mNotified = savedInstanceState.getBoolean(KEY_NOTIFIED);
		}

		setHasOptionsMenu(true);

		final Intent intent = UIUtils.fragmentArgumentsToIntent(getArguments());
		mPlayId = intent.getIntExtra(PlayActivity.KEY_PLAY_ID, BggContract.INVALID_ID);

		if (mPlayId == BggContract.INVALID_ID) {
			return;
		}

		mPlay = new Play(mPlayId, intent.getIntExtra(PlayActivity.KEY_GAME_ID, BggContract.INVALID_ID),
			intent.getStringExtra(PlayActivity.KEY_GAME_NAME));

		mThumbnailUrl = intent.getStringExtra(PlayActivity.KEY_THUMBNAIL_URL);
		mImageUrl = intent.getStringExtra(PlayActivity.KEY_IMAGE_URL);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		ViewGroup rootView = (ViewGroup) super.onCreateView(inflater, container, savedInstanceState);

		ListView playersView = (ListView) rootView.findViewById(android.R.id.list);
		playersView.setHeaderDividersEnabled(false);
		playersView.setFooterDividersEnabled(false);

		playersView.addHeaderView(View.inflate(getActivity(), R.layout.header_play, null), null, false);
		playersView.addFooterView(View.inflate(getActivity(), R.layout.footer_play, null), null, false);

		ButterKnife.inject(this, rootView);

		mAdapter = new PlayerAdapter();
		playersView.setAdapter(mAdapter);

		getLoaderManager().restartLoader(PlayQuery._TOKEN, null, this);

		return rootView;
	}

	@Override
	public void onResume() {
		super.onResume();
		if (mPlay != null && mPlay.hasStarted()) {
			NotificationUtils.launchPlayingNotification(getActivity(), mPlay, mThumbnailUrl, mImageUrl);
			mNotified = true;
		}
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putBoolean(KEY_NOTIFIED, mNotified);
	}

	@Override
	public void onDetach() {
		super.onDetach();
		mCallbacks = sDummyCallbacks;
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.play, menu);
	}

	@Override
	public void onPrepareOptionsMenu(Menu menu) {
		menu.findItem(R.id.menu_send).setVisible(mPlay.syncStatus == Play.SYNC_STATUS_IN_PROGRESS);
		MenuItem refreshMenuItem = menu.findItem(R.id.menu_refresh);
		refreshMenuItem.setEnabled(mPlay.hasBeenSynced());
		if (mPlay.syncStatus == Play.SYNC_STATUS_IN_PROGRESS) {
			refreshMenuItem.setTitle(R.string.menu_discard_changes);
		}
		menu.findItem(R.id.menu_share).setEnabled(mPlay.syncStatus == Play.SYNC_STATUS_SYNCED);

		super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_refresh:
				if (mPlay.syncStatus != Play.SYNC_STATUS_SYNCED) {
					DialogUtils.createConfirmationDialog(getActivity(), R.string.are_you_sure_refresh_message,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								save(Play.SYNC_STATUS_SYNCED);
							}
						}).show();
				} else {
					triggerRefresh();
				}
				return true;
			case R.id.menu_edit:
				ActivityUtils.editPlay(getActivity(), mPlay.playId, mPlay.gameId, mPlay.gameName, mThumbnailUrl,
					mImageUrl);
				return true;
			case R.id.menu_send:
				save(Play.SYNC_STATUS_PENDING_UPDATE);
				mCallbacks.onSent();
				return true;
			case R.id.menu_delete: {
				DialogUtils.createConfirmationDialog(getActivity(), R.string.are_you_sure_delete_play,
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							if (mPlay.hasStarted()) {
								NotificationUtils.cancel(getActivity(), NotificationUtils.ID_PLAY_TIMER);
							}
							mPlay.end(); // this prevents the timer from reappearing
							save(Play.SYNC_STATUS_PENDING_DELETE);
							mCallbacks.onDeleted();
						}
					}).show();
				return true;
			}
			case R.id.menu_rematch:
				ActivityUtils.rematch(getActivity(), mPlay.playId, mPlay.gameId, mPlay.gameName, mThumbnailUrl,
					mImageUrl);
				getActivity().finish(); // don't want to show the "old" play upon return
				return true;
			case R.id.menu_share:
				ActivityUtils.share(getActivity(), mPlay.toShortDescription(getActivity()),
					mPlay.toLongDescription(getActivity()), R.string.share_play_title);
				return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@OnClick(R.id.header_container)
	void viewGame(View v) {
		ActivityUtils.launchGame(getActivity(), mPlay.gameId, mPlay.gameName);
	}

	@OnClick(R.id.timer_end)
	void onClick(View v) {
		ActivityUtils.endPlay(getActivity(), mPlay.playId, mPlay.gameId, mPlay.gameName, mThumbnailUrl, mImageUrl);
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle data) {
		CursorLoader loader = null;
		switch (id) {
			case PlayQuery._TOKEN:
				loader = new CursorLoader(getActivity(), Plays.buildPlayUri(mPlayId), PlayQuery.PROJECTION, null, null,
					null);
				break;
			case PlayerQuery._TOKEN:
				loader = new CursorLoader(getActivity(), Plays.buildPlayerUri(mPlayId), PlayerQuery.PROJECTION, null,
					null, null);
				break;
		}
		return loader;
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		if (getActivity() == null) {
			return;
		}

		switch (loader.getId()) {
			case PlayQuery._TOKEN:
				if (onPlayQueryComplete(cursor)) {
					setListShown(true);
				}
				break;
			case PlayerQuery._TOKEN:
				// HACK ensures the list header shows, not the empty text
				setEmptyText(null);
				getListView().setEmptyView(null);

				mPlay.setPlayers(cursor);
				mPlayersLabel.setVisibility(mPlay.getPlayers().size() == 0 ? View.GONE : View.VISIBLE);
				mAdapter.notifyDataSetChanged();
				maybeShowNotification();
				setListShown(true);
				break;
			default:
				if (cursor != null) {
					cursor.close();
				}
				break;
		}
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
	}

	public void setNewPlayId(int playId) {
		mPlayId = playId;
		getLoaderManager().restartLoader(PlayQuery._TOKEN, null, this);
	}

	/**
	 * @return true if the we're done loading
	 */
	private boolean onPlayQueryComplete(Cursor cursor) {
		if (cursor == null || !cursor.moveToFirst()) {
			int newPlayId = PreferencesUtils.getNewPlayId(getActivity(), mPlayId);
			if (newPlayId != BggContract.INVALID_ID) {
				setNewPlayId(newPlayId);
				return false;
			}
			setEmptyText(String.format(getResources().getString(R.string.empty_play), mPlayId));
			return true;
		}

		ImageUtils.safelyLoadImage(mThumbnailView, mImageUrl);

		List<Player> players = mPlay.getPlayers();
		mPlay = PlayBuilder.fromCursor(cursor);
		mPlay.setPlayers(players);

		mCallbacks.onNameChanged(mPlay.gameName);

		mGameName.setText(mPlay.gameName);

		mDate.setText(mPlay.getDateForDisplay(getActivity()));

		mQuantity.setText(String.valueOf(mPlay.quantity) + " " + getString(R.string.times));
		mQuantity.setVisibility((mPlay.quantity == 1) ? View.GONE : View.VISIBLE);

		if (mPlay.length > 0) {
			mLengthRoot.setVisibility(View.VISIBLE);
			mLength.setText(DateTimeUtils.describeMinutes(getActivity(), mPlay.length));
			mLength.setVisibility(View.VISIBLE);
			mTimerRoot.setVisibility(View.GONE);
			mTimer.stop();
		} else if (mPlay.hasStarted()) {
			mLengthRoot.setVisibility(View.VISIBLE);
			mLength.setVisibility(View.GONE);
			mTimerRoot.setVisibility(View.VISIBLE);
			UIUtils.startTimerWithSystemTime(mTimer, mPlay.startTime);
		} else {
			mLengthRoot.setVisibility(View.GONE);
		}

		mLocation.setText(mPlay.location);
		mLocationRoot.setVisibility(TextUtils.isEmpty(mPlay.location) ? View.GONE : View.VISIBLE);

		mIncomplete.setVisibility(mPlay.Incomplete() ? View.VISIBLE : View.GONE);
		mNoWinStats.setVisibility(mPlay.NoWinStats() ? View.VISIBLE : View.GONE);

		mComments.setText(mPlay.comments);
		mComments.setVisibility(TextUtils.isEmpty(mPlay.comments) ? View.GONE : View.VISIBLE);
		mCommentsLabel.setVisibility(TextUtils.isEmpty(mPlay.comments) ? View.GONE : View.VISIBLE);

		mUpdated.setText(getResources().getString(R.string.updated) + " "
			+ DateUtils.getRelativeTimeSpanString(mPlay.updated));
		mUpdated.setVisibility((mPlay.updated == 0) ? View.GONE : View.VISIBLE);

		if (mPlay.hasBeenSynced()) {
			mPlayIdView.setText(String.format(getResources().getString(R.string.id_list_text), mPlay.playId));
		}

		if (mPlay.syncStatus != Play.SYNC_STATUS_SYNCED) {
			mUnsyncedMessage.setVisibility(View.VISIBLE);
			mSavedTimeStamp.setVisibility(View.VISIBLE);
			mSavedTimeStamp.setText(getResources().getString(R.string.saved) + " "
				+ DateUtils.getRelativeTimeSpanString(mPlay.saved));
			if (mPlay.syncStatus == Play.SYNC_STATUS_IN_PROGRESS) {
				if (mPlay.hasBeenSynced()) {
					mUnsyncedMessage.setText(R.string.sync_editing);
				} else {
					mUnsyncedMessage.setText(R.string.sync_draft);
				}
			} else if (mPlay.syncStatus == Play.SYNC_STATUS_PENDING_UPDATE) {
				mUnsyncedMessage.setText(R.string.sync_pending_update);
			} else if (mPlay.syncStatus == Play.SYNC_STATUS_PENDING_DELETE) {
				mUnsyncedMessage.setText(R.string.sync_pending_delete);
			}
		} else {
			mUnsyncedMessage.setVisibility(View.GONE);
			mSavedTimeStamp.setVisibility(View.GONE);
		}

		getActivity().supportInvalidateOptionsMenu();
		getLoaderManager().restartLoader(PlayerQuery._TOKEN, null, this);

		if (mPlay.hasBeenSynced()
			&& (mPlay.updated == 0 || DateTimeUtils.howManyDaysOld(mPlay.updated) > AGE_IN_DAYS_TO_REFRESH)) {
			triggerRefresh();
		}

		return false;
	}

	private void maybeShowNotification() {
		if (mPlay.hasStarted()) {
			NotificationUtils.launchPlayingNotification(getActivity(), mPlay, mThumbnailUrl, mImageUrl);
		} else if (mNotified) {
			NotificationUtils.cancel(getActivity(), NotificationUtils.ID_PLAY_TIMER);
		}
	}

	private void triggerRefresh() {
		UpdateService.start(getActivity(), UpdateService.SYNC_TYPE_GAME_PLAYS, mPlay.gameId, mReceiver);
	}

	private void save(int status) {
		mPlay.syncStatus = status;
		new PlayPersister(getActivity()).save(null, mPlay);
		triggerRefresh();
	}

	private class PlayerAdapter extends BaseAdapter {
		@Override
		public boolean isEnabled(int position) {
			return false;
		}

		@Override
		public int getCount() {
			return mPlay.getPlayerCount();
		}

		@Override
		public Object getItem(int position) {
			return mPlay.getPlayers().get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			PlayerRow row = new PlayerRow(getActivity());
			row.setPlayer((Player) getItem(position));
			return row;
		}
	}

	private interface PlayQuery {
		int _TOKEN = 0x01;
		String[] PROJECTION = { Plays.PLAY_ID, PlayItems.NAME, PlayItems.OBJECT_ID, Plays.DATE, Plays.LOCATION,
			Plays.LENGTH, Plays.QUANTITY, Plays.INCOMPLETE, Plays.NO_WIN_STATS, Plays.COMMENTS, Plays.UPDATED_LIST,
			Plays.SYNC_STATUS, Plays.UPDATED, Plays.START_TIME };
	}

	private interface PlayerQuery {
		int _TOKEN = 0x02;
		String[] PROJECTION = { PlayPlayers.USER_NAME, PlayPlayers.NAME, PlayPlayers.START_POSITION, PlayPlayers.COLOR,
			PlayPlayers.SCORE, PlayPlayers.RATING, PlayPlayers.NEW, PlayPlayers.WIN, };
	}
}
