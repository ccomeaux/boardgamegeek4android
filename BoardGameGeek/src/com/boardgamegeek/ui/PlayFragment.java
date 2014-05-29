package com.boardgamegeek.ui;

import java.util.List;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockListFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
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
import com.boardgamegeek.util.NotificationUtils;
import com.boardgamegeek.util.PreferencesUtils;
import com.boardgamegeek.util.UIUtils;
import com.squareup.picasso.Picasso;

public class PlayFragment extends SherlockListFragment implements LoaderManager.LoaderCallbacks<Cursor>,
	DetachableResultReceiver.Receiver {
	private static final int AGE_IN_DAYS_TO_REFRESH = 7;

	private int mPlayId = BggContract.INVALID_ID;
	private Play mPlay = new Play();
	private String mThumbnailUrl;

	private TextView mUpdated;
	private TextView mPlayIdView;
	private ImageView mThumbnailView;
	private TextView mGameName;
	private TextView mDate;
	private View mQuantityRoot;
	private TextView mQuantity;
	private View mLengthRoot;
	private TextView mLength;
	private View mTimerRoot;
	private Chronometer mTimer;
	private View mLocationRoot;
	private TextView mLocation;
	private View mIncomplete;
	private View mNoWinStats;
	private TextView mComments;
	private View mCommentsLabel;
	private View mPlayersLabel;
	private ListView mPlayers;
	private TextView mSavedTimeStamp;
	private TextView mUnsyncedMessage;
	private boolean mPlaysLoaded;
	private boolean mPlayersLoaded;
	private PlayerAdapter mAdapter;
	private DetachableResultReceiver mReceiver;

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
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);

		// TODO: move this to a fragment so it survives orientation changes
		mReceiver = new DetachableResultReceiver(new Handler());
		mReceiver.setReceiver(this);

		final Intent intent = UIUtils.fragmentArgumentsToIntent(getArguments());
		mPlayId = intent.getIntExtra(PlayActivity.KEY_PLAY_ID, BggContract.INVALID_ID);

		if (mPlayId == BggContract.INVALID_ID) {
			return;
		}

		mPlay = new Play(mPlayId, intent.getIntExtra(PlayActivity.KEY_GAME_ID, BggContract.INVALID_ID),
			intent.getStringExtra(PlayActivity.KEY_GAME_NAME));

		mThumbnailUrl = intent.getStringExtra(PlayActivity.KEY_THUMBNAIL_URL);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		ViewGroup rootView = (ViewGroup) super.onCreateView(inflater, container, savedInstanceState);
		int padding = getResources().getDimensionPixelSize(R.dimen.padding_standard);
		rootView.setPadding(padding, padding, padding, padding);

		mPlayers = (ListView) rootView.findViewById(android.R.id.list);
		mPlayers.setHeaderDividersEnabled(false);
		mPlayers.setFooterDividersEnabled(false);

		View header = View.inflate(getActivity(), R.layout.header_play, null);
		mPlayers.addHeaderView(header);

		View footer = View.inflate(getActivity(), R.layout.footer_play, null);
		mPlayers.addFooterView(footer);

		mThumbnailView = (ImageView) rootView.findViewById(R.id.game_info_thumbnail);
		mGameName = (TextView) header.findViewById(R.id.game_name);
		mDate = (TextView) header.findViewById(R.id.play_date);

		mQuantityRoot = header.findViewById(R.id.quantity_root);
		mQuantity = (TextView) header.findViewById(R.id.play_quantity);

		mLengthRoot = header.findViewById(R.id.length_root);
		mLength = (TextView) header.findViewById(R.id.play_length);

		mTimerRoot = header.findViewById(R.id.timer_root);
		mTimer = (Chronometer) header.findViewById(R.id.timer);
		Button b = (Button) header.findViewById(R.id.timer_end);
		b.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				ActivityUtils.endPlay(getActivity(), mPlay.PlayId, mPlay.GameId, mPlay.GameName);
			}
		});

		mLocationRoot = header.findViewById(R.id.location_root);
		mLocation = (TextView) header.findViewById(R.id.play_location);

		mIncomplete = header.findViewById(R.id.play_incomplete);
		mNoWinStats = header.findViewById(R.id.play_no_win_stats);

		mCommentsLabel = header.findViewById(R.id.play_comments_label);
		mComments = (TextView) header.findViewById(R.id.play_comments);

		mPlayersLabel = header.findViewById(R.id.play_players_label);

		mUpdated = (TextView) footer.findViewById(R.id.updated);
		mPlayIdView = (TextView) footer.findViewById(R.id.play_id);
		mSavedTimeStamp = (TextView) footer.findViewById(R.id.play_saved);
		mUnsyncedMessage = (TextView) footer.findViewById(R.id.play_unsynced_message);

		mAdapter = new PlayerAdapter();
		mPlayers.setAdapter(mAdapter);

		mPlaysLoaded = true;
		mPlayersLoaded = false;
		getLoaderManager().restartLoader(PlayQuery._TOKEN, null, this);
		getLoaderManager().restartLoader(PlayerQuery._TOKEN, null, this);

		return rootView;
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);

		if (!(activity instanceof Callbacks)) {
			throw new ClassCastException("Activity must implement fragment's callbacks.");
		}

		mCallbacks = (Callbacks) activity;
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
		menu.findItem(R.id.menu_send).setVisible(mPlay.SyncStatus == Play.SYNC_STATUS_IN_PROGRESS);
		MenuItem refreshMenuItem = menu.findItem(R.id.menu_refresh);
		refreshMenuItem.setEnabled(mPlay.hasBeenSynced());
		if (mPlay.SyncStatus == Play.SYNC_STATUS_IN_PROGRESS) {
			refreshMenuItem.setTitle(R.string.menu_discard_changes);
		}
		menu.findItem(R.id.menu_share).setEnabled(mPlay.SyncStatus == Play.SYNC_STATUS_SYNCED);

		super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_refresh:
				if (mPlay.SyncStatus != Play.SYNC_STATUS_SYNCED) {
					ActivityUtils.createConfirmationDialog(getActivity(), R.string.are_you_sure_refresh_message,
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
				ActivityUtils.editPlay(getActivity(), mPlay.PlayId, mPlay.GameId, mPlay.GameName);
				return true;
			case R.id.menu_send:
				save(Play.SYNC_STATUS_PENDING_UPDATE);
				mCallbacks.onSent();
				return true;
			case R.id.menu_delete: {
				ActivityUtils.createConfirmationDialog(getActivity(), R.string.are_you_sure_delete_play,
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
			case R.id.menu_play_again:
				ActivityUtils.logPlayAgain(getActivity(), mPlay.PlayId, mPlay.GameId, mPlay.GameName);
				return true;
			case R.id.menu_share:
				ActivityUtils.share(getActivity(), mPlay.toShortDescription(getActivity()),
					mPlay.toLongDescription(getActivity()), R.string.share_play_title);
				return true;
			case R.id.menu_view_game:
				ActivityUtils.launchGame(getActivity(), mPlay.GameId, mPlay.GameName);
				return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onReceiveResult(int resultCode, Bundle resultData) {
		switch (resultCode) {
			case UpdateService.STATUS_ERROR:
				Toast.makeText(getActivity(), resultData.getString(Intent.EXTRA_TEXT), Toast.LENGTH_LONG).show();
				break;
		}
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
				onPlayQueryComplete(cursor);
				break;
			case PlayerQuery._TOKEN:
				onPlayerQueryComplete(cursor);
				break;
			default:
				if (cursor != null) {
					cursor.close();
				}
				break;
		}

		if (mPlaysLoaded && mPlayersLoaded) {
			setListShown(true);
		}
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
	}

	public void setNewPlayId(int playId) {
		mPlayId = playId;
		getLoaderManager().restartLoader(PlayQuery._TOKEN, null, this);
		getLoaderManager().restartLoader(PlayerQuery._TOKEN, null, this);
	}

	private void onPlayQueryComplete(Cursor cursor) {
		if (cursor == null || !cursor.moveToFirst()) {
			int newPlayId = PreferencesUtils.getNewPlayId(getActivity(), mPlayId);
			if (newPlayId != BggContract.INVALID_ID) {
				setNewPlayId(newPlayId);
				return;
			}
			setEmptyText(String.format(getResources().getString(R.string.empty_play), mPlayId));
			mPlaysLoaded = true;
			return;
		}

		mPlaysLoaded = true;

		if (TextUtils.isEmpty(mThumbnailUrl)) {
			mThumbnailView.setVisibility(View.GONE);
		} else {
			mThumbnailView.setVisibility(View.VISIBLE);
			Picasso.with(getActivity()).load(mThumbnailUrl).placeholder(R.drawable.thumbnail_image_empty)
				.error(R.drawable.thumbnail_image_empty).resizeDimen(R.dimen.thumbnail_size, R.dimen.thumbnail_size)
				.centerCrop().into(mThumbnailView);
		}

		List<Player> players = mPlay.getPlayers();
		mPlay = PlayBuilder.fromCursor(cursor);
		mPlay.setPlayers(players);

		if (mPlay.hasStarted()) {
			NotificationUtils.launchStartNotification(getActivity(), mPlay);
		}

		mCallbacks.onNameChanged(mPlay.GameName);

		mGameName.setText(mPlay.GameName);

		mDate.setText(mPlay.getDateForDisplay(getActivity()));

		mQuantity.setText(String.valueOf(mPlay.Quantity) + " " + getString(R.string.times));
		mQuantityRoot.setVisibility((mPlay.Quantity == 1) ? View.GONE : View.VISIBLE);

		if (mPlay.Length > 0) {
			mLengthRoot.setVisibility(View.VISIBLE);
			mLength.setText(DateTimeUtils.describeMinutes(getActivity(), mPlay.Length));
			mLength.setVisibility(View.VISIBLE);
			mTimerRoot.setVisibility(View.GONE);
			mTimer.stop();
		} else if (mPlay.hasStarted()) {
			mLengthRoot.setVisibility(View.VISIBLE);
			mLength.setVisibility(View.GONE);
			mTimerRoot.setVisibility(View.VISIBLE);
			UIUtils.startTimerWithSystemTime(mTimer, mPlay.StartTime);
		} else {
			mLengthRoot.setVisibility(View.GONE);
		}

		mLocation.setText(mPlay.Location);
		mLocationRoot.setVisibility(TextUtils.isEmpty(mPlay.Location) ? View.GONE : View.VISIBLE);

		mIncomplete.setVisibility(mPlay.Incomplete ? View.VISIBLE : View.GONE);
		mNoWinStats.setVisibility(mPlay.NoWinStats ? View.VISIBLE : View.GONE);

		mComments.setText(mPlay.Comments);
		mComments.setVisibility(TextUtils.isEmpty(mPlay.Comments) ? View.GONE : View.VISIBLE);
		mCommentsLabel.setVisibility(TextUtils.isEmpty(mPlay.Comments) ? View.GONE : View.VISIBLE);

		mUpdated.setText(getResources().getString(R.string.updated) + " "
			+ DateUtils.getRelativeTimeSpanString(mPlay.Updated));
		mUpdated.setVisibility((mPlay.Updated == 0) ? View.GONE : View.VISIBLE);

		if (mPlay.hasBeenSynced()) {
			mPlayIdView.setText(String.format(getResources().getString(R.string.id_list_text), mPlay.PlayId));
		}

		if (mPlay.SyncStatus != Play.SYNC_STATUS_SYNCED) {
			mUnsyncedMessage.setVisibility(View.VISIBLE);
			mSavedTimeStamp.setVisibility(View.VISIBLE);
			mSavedTimeStamp.setText(getResources().getString(R.string.saved) + " "
				+ DateUtils.getRelativeTimeSpanString(mPlay.Saved));
			if (mPlay.SyncStatus == Play.SYNC_STATUS_IN_PROGRESS) {
				if (mPlay.hasBeenSynced()) {
					mUnsyncedMessage.setText(R.string.sync_editing);
				} else {
					mUnsyncedMessage.setText(R.string.sync_draft);
				}
			} else if (mPlay.SyncStatus == Play.SYNC_STATUS_PENDING_UPDATE) {
				mUnsyncedMessage.setText(R.string.sync_pending_update);
			} else if (mPlay.SyncStatus == Play.SYNC_STATUS_PENDING_DELETE) {
				mUnsyncedMessage.setText(R.string.sync_pending_delete);
			}
		} else {
			mUnsyncedMessage.setVisibility(View.GONE);
			mSavedTimeStamp.setVisibility(View.GONE);
		}

		getActivity().supportInvalidateOptionsMenu();

		if (mPlay.hasBeenSynced()
			&& (mPlay.Updated == 0 || DateTimeUtils.howManyDaysOld(mPlay.Updated) > AGE_IN_DAYS_TO_REFRESH)) {
			triggerRefresh();
		}
	}

	private void onPlayerQueryComplete(Cursor cursor) {
		mPlay.setPlayers(cursor);
		mPlayersLabel.setVisibility(mPlay.getPlayers().size() == 0 ? View.GONE : View.VISIBLE);
		mAdapter.notifyDataSetChanged();
		mPlayersLoaded = true;
	}

	private void triggerRefresh() {
		UpdateService.start(getActivity(), UpdateService.SYNC_TYPE_GAME_PLAYS, mPlay.GameId, mReceiver);
	}

	private void save(int status) {
		mPlay.SyncStatus = status;
		PlayPersister.save(getActivity().getContentResolver(), mPlay);
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
