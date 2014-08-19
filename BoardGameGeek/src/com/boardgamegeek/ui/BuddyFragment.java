package com.boardgamegeek.ui;

import java.util.List;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.boardgamegeek.R;
import com.boardgamegeek.model.Play;
import com.boardgamegeek.provider.BggContract.Buddies;
import com.boardgamegeek.provider.BggContract.PlayPlayers;
import com.boardgamegeek.provider.BggContract.Plays;
import com.boardgamegeek.service.SyncService;
import com.boardgamegeek.util.BuddyUtils;
import com.boardgamegeek.util.DetachableResultReceiver;
import com.boardgamegeek.util.ResolverUtils;
import com.boardgamegeek.util.UIUtils;
import com.squareup.picasso.Picasso;

public class BuddyFragment extends SherlockFragment implements LoaderManager.LoaderCallbacks<Cursor> {
	private Uri mBuddyUri;
	private int mDefaultTextColor;
	private int mLightTextColor;

	private ViewGroup mRootView;
	private TextView mFullName;
	private TextView mName;
	private TextView mId;
	private ImageView mAvatar;
	private TextView mNickname;
	private TextView mUpdated;

	public interface Callbacks {
		public DetachableResultReceiver getReceiver();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);

		final Intent intent = UIUtils.fragmentArgumentsToIntent(getArguments());
		String buddyName = intent.getStringExtra(BuddyUtils.KEY_BUDDY_NAME);
		if (TextUtils.isEmpty(buddyName)) {
			return;
		}

		mBuddyUri = Buddies.buildBuddyUri(buddyName);
		if (mBuddyUri == null) {
			return;
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		mRootView = (ViewGroup) inflater.inflate(R.layout.fragment_buddy, container, false);

		mFullName = (TextView) mRootView.findViewById(R.id.buddy_full_name);
		mName = (TextView) mRootView.findViewById(R.id.buddy_name);
		mId = (TextView) mRootView.findViewById(R.id.buddy_id);
		mAvatar = (ImageView) mRootView.findViewById(R.id.buddy_avatar);
		mNickname = (TextView) mRootView.findViewById(R.id.nickname);
		mUpdated = (TextView) mRootView.findViewById(R.id.updated);

		mDefaultTextColor = mNickname.getTextColors().getDefaultColor();
		mLightTextColor = getResources().getColor(R.color.light_text);

		getLoaderManager().restartLoader(BuddyQuery._TOKEN, null, this);

		return mRootView;
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.buddy, menu);
		super.onCreateOptionsMenu(menu, inflater);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		switch (id) {
			case R.id.menu_edit:
				showDialog(getActivity(), mBuddyUri, mNickname.getText().toString(), mName.getText().toString());
				return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle data) {
		CursorLoader loader = null;
		if (id == BuddyQuery._TOKEN) {
			loader = new CursorLoader(getActivity(), mBuddyUri, BuddyQuery.PROJECTION, null, null, null);
		}
		return loader;
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		if (getActivity() == null) {
			return;
		}

		if (loader.getId() == BuddyQuery._TOKEN) {
			onBuddyQueryComplete(cursor);
		} else {
			cursor.close();
		}
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
	}

	private void onBuddyQueryComplete(Cursor cursor) {
		if (cursor == null || !cursor.moveToFirst()) {
			return;
		}

		int id = cursor.getInt(BuddyQuery.BUDDY_ID);
		String name = cursor.getString(BuddyQuery.NAME);
		String nickname = cursor.getString(BuddyQuery.PLAY_NICKNAME);
		final String avatarUrl = cursor.getString(BuddyQuery.AVATAR_URL);
		long updated = cursor.getLong(BuddyQuery.UPDATED);
		String fullName = BuddyUtils.buildFullName(cursor, BuddyQuery.FIRSTNAME, BuddyQuery.LASTNAME);

		Picasso.with(getActivity()).load(avatarUrl).placeholder(R.drawable.person_image_empty)
			.error(R.drawable.person_image_empty).fit().into(mAvatar);
		mFullName.setText(fullName);
		mName.setText(name);
		mId.setText(String.valueOf(id));
		if (TextUtils.isEmpty(nickname)) {
			mNickname.setTextColor(mLightTextColor);
			mNickname.setText(fullName);
		} else {
			mNickname.setTextColor(mDefaultTextColor);
			mNickname.setText(nickname);
		}
		mUpdated.setText(getResources().getString(R.string.updated)
			+ ": "
			+ (updated == 0 ? getResources().getString(R.string.needs_updating) : DateUtils
				.getRelativeTimeSpanString(updated)));
	}

	private interface BuddyQuery {
		int _TOKEN = 0x1;

		String[] PROJECTION = { Buddies.BUDDY_ID, Buddies.BUDDY_NAME, Buddies.BUDDY_FIRSTNAME, Buddies.BUDDY_LASTNAME,
			Buddies.AVATAR_URL, Buddies.PLAY_NICKNAME, Buddies.UPDATED };

		int BUDDY_ID = 0;
		int NAME = 1;
		int FIRSTNAME = 2;
		int LASTNAME = 3;
		int AVATAR_URL = 4;
		int PLAY_NICKNAME = 5;
		int UPDATED = 6;
	}

	public void showDialog(final Context context, final Uri uri, final String nickname, final String username) {
		final LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View view = inflater.inflate(R.layout.dialog_edit_nickname, mRootView, false);

		final EditText editText = (EditText) view.findViewById(R.id.edit_nickname);
		final CheckBox checkBox = (CheckBox) view.findViewById(R.id.change_plays);
		if (!TextUtils.isEmpty(nickname)) {
			editText.setText(nickname);
			editText.setSelection(0, nickname.length());
		}

		AlertDialog dialog = new AlertDialog.Builder(context).setView(view).setTitle(R.string.title_edit_nickname)
			.setNegativeButton(R.string.cancel, null)
			.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					String newNickname = editText.getText().toString();
					new Task(context, uri, username, checkBox.isChecked()).execute(newNickname);
				}
			}).create();
		dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
		dialog.show();
	}

	private class Task extends AsyncTask<String, Void, Void> {
		private static final String SELECTION = PlayPlayers.USER_NAME + "=? AND play_players." + PlayPlayers.NAME
			+ "!=?";
		Context mContext;
		Uri mUri;
		String mUsername;
		boolean mUpdatePlays;

		public Task(Context context, Uri uri, String username, boolean updatePlays) {
			mContext = context;
			mUri = uri;
			mUsername = username;
			mUpdatePlays = updatePlays;
		}

		@Override
		protected Void doInBackground(String... params) {
			String newNickname = params[0];
			updateNickname(mContext, mUri, newNickname);
			if (mUpdatePlays) {
				if (TextUtils.isEmpty(newNickname)) {
					showToast(getString(R.string.msg_missing_nickname));
				} else {
					int count = updatePlays(mContext, mUsername, newNickname);
					if (count > 0) {
						updatePlayers(mContext, mUsername, newNickname);
						SyncService.sync(mContext, SyncService.FLAG_SYNC_PLAYS_UPLOAD);
					}
					showToast(getResources().getQuantityString(R.plurals.msg_updated_plays, count, count));
				}
			}
			return null;
		}

		private void showToast(final String text) {
			getActivity().runOnUiThread(new Runnable() {
				@Override
				public void run() {
					Toast.makeText(mContext, text, Toast.LENGTH_LONG).show();
				}
			});
		}

		private void updateNickname(final Context context, final Uri uri, String nickname) {
			ContentValues values = new ContentValues(1);
			values.put(Buddies.PLAY_NICKNAME, nickname);
			context.getContentResolver().update(uri, values, null, null);
		}

		private int updatePlays(final Context context, final String username, final String newNickname) {
			ContentValues values = new ContentValues(1);
			values.put(Plays.SYNC_STATUS, Play.SYNC_STATUS_PENDING_UPDATE);
			List<Integer> playIds = ResolverUtils.queryInts(context.getContentResolver(), Plays.buildPlayersUri(),
				Plays.PLAY_ID, SELECTION, new String[] { username, newNickname });
			for (Integer playId : playIds) {
				context.getContentResolver().update(Plays.buildPlayUri(playId), values, null, null);
			}
			return playIds.size();
		}

		private int updatePlayers(final Context context, final String username, String newNickname) {
			ContentValues values = new ContentValues(1);
			values.put(PlayPlayers.NAME, newNickname);
			return context.getContentResolver().update(Plays.buildPlayersUri(), values, SELECTION,
				new String[] { username, newNickname });
		}
	}
}
