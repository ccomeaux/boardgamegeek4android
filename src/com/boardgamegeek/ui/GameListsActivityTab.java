package com.boardgamegeek.ui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.client.HttpClient;

import android.app.Dialog;
import android.app.ExpandableListActivity;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.text.util.Linkify;
import android.util.Log;
import android.view.View;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ScrollView;
import android.widget.SimpleExpandableListAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.boardgamegeek.R;
import com.boardgamegeek.io.RemoteArtistHandler;
import com.boardgamegeek.io.RemoteDesignerHandler;
import com.boardgamegeek.io.RemoteExecutor;
import com.boardgamegeek.io.XmlHandler.HandlerException;
import com.boardgamegeek.provider.BggContract.Artists;
import com.boardgamegeek.provider.BggContract.Designers;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.provider.BggContract.SyncColumns;
import com.boardgamegeek.provider.BggDatabase.GamesArtists;
import com.boardgamegeek.provider.BggDatabase.GamesDesigners;
import com.boardgamegeek.util.DateTimeUtils;
import com.boardgamegeek.util.HttpUtils;
import com.boardgamegeek.util.NotifyingAsyncQueryHandler;
import com.boardgamegeek.util.NotifyingAsyncQueryHandler.AsyncQueryListener;
import com.boardgamegeek.util.StringUtils;

public class GameListsActivityTab extends ExpandableListActivity implements AsyncQueryListener {
	private static final String TAG = "GameListsActivityTab";

	private static final int ID_DIALOG_RESULTS = 1;

	private static final int TOKEN_DESIGNERS = 1;
	private static final int TOKEN_DESIGNERS_UPDATE = 2;
	private static final int TOKEN_ARTISTS = 3;
	// private static final int TOKEN_ARTISTS_UPDATE = 4;

	private static final String NAME = "NAME";
	private static final String COUNT = "COUNT";
	private static final String DESCRIPTION = "DESCRIPTION";

	private Uri mDesignersUri;
	private Uri mArtistsUri;
	private NotifyingAsyncQueryHandler mHandler;

	private List<Map<String, String>> groupData;
	private List<List<Map<String, String>>> childData;
	private ExpandableListAdapter adapter;

	private String mName;
	private String mDescription;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setUiVariables();
		setUris();

		getContentResolver().registerContentObserver(mDesignersUri, true, new DesignerObserver(null));
		getContentResolver().registerContentObserver(mArtistsUri, true, new ArtistObserver(null));

		mHandler = new NotifyingAsyncQueryHandler(getContentResolver(), this);
		mHandler.startQuery(TOKEN_DESIGNERS, mDesignersUri, DesignerQuery.PROJECTION);
		mHandler.startQuery(TOKEN_ARTISTS, mArtistsUri, ArtistQuery.PROJECTION);
	}

	private void setUiVariables() {
		groupData = new ArrayList<Map<String, String>>();
		childData = new ArrayList<List<Map<String, String>>>();
	}

	private void setUris() {
		final Uri gameUri = getIntent().getData();
		final int gameId = Games.getGameId(gameUri);
		mDesignersUri = Games.buildDesignersUri(gameId);
		mArtistsUri = Games.buildArtistsUri(gameId);
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {

		removeDialog(ID_DIALOG_RESULTS);

		Map<String, Object> childItem = (Map<String, Object>) adapter.getChild(groupPosition, childPosition);
		mName = (String) childItem.get(NAME);
		mDescription = (String) childItem.get(DESCRIPTION);
		if (TextUtils.isEmpty(mDescription)) {
			Toast.makeText(this, "No extra information", Toast.LENGTH_LONG).show();
		} else {
			showDialog(ID_DIALOG_RESULTS);
		}

		return super.onChildClick(parent, v, groupPosition, childPosition, id);
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		if (id == ID_DIALOG_RESULTS) {
			Dialog dialog = new Dialog(this);
			dialog.setTitle(mName);
			TextView tv = new TextView(this);
			tv.setTextColor(Color.WHITE);
			tv.setAutoLinkMask(Linkify.ALL);
			tv.setText(StringUtils.unescapeHtml(mDescription));
			ScrollView sv = new ScrollView(this);
			sv.setPadding(6, 0, 6, 6);
			sv.addView(tv);
			dialog.setContentView(sv);
			dialog.setCancelable(true);
			return dialog;
		}
		return super.onCreateDialog(id);
	}

	public void onQueryComplete(int token, Object cookie, Cursor cursor) {
		try {
			if (token == TOKEN_DESIGNERS) {
				List<Integer> ids = new ArrayList<Integer>();
				List<ChildItem> designers = new ArrayList<ChildItem>();
				while (cursor.moveToNext()) {
					int id = cursor.getInt(DesignerQuery.DESIGNER_ID);

					Uri uri = Designers.buildDesignerUri(id);
					getContentResolver().registerContentObserver(uri, true, new DesignerObserver(null));

					long lastUpdated = cursor.getLong(DesignerQuery.UPDATED);
					if (lastUpdated == 0 || DateTimeUtils.howManyDaysOld(lastUpdated) > 14) {
						ids.add(id);
					}

					final ChildItem childItem = new ChildItem();
					childItem.Name = cursor.getString(DesignerQuery.DESIGNER_NAME);
					childItem.Description = cursor.getString(DesignerQuery.DESIGNER_DESCRIPTION);
					designers.add(childItem);
				}
				createGroup(R.string.designers, designers);

				if (ids.size() > 0) {
					Integer[] array = new Integer[ids.size()];
					ids.toArray(array);
					new DesignerTask().execute(array);
				}
			} else if (token == TOKEN_ARTISTS) {
				List<Integer> ids = new ArrayList<Integer>();
				List<ChildItem> artists = new ArrayList<ChildItem>();
				while (cursor.moveToNext()) {
					int id = cursor.getInt(ArtistQuery.ARTIST_ID);

					Uri uri = Artists.buildArtistUri(id);
					getContentResolver().registerContentObserver(uri, true, new ArtistObserver(null)); // TODO

					long lastUpdated = cursor.getLong(ArtistQuery.UPDATED);
					if (lastUpdated == 0 || DateTimeUtils.howManyDaysOld(lastUpdated) > 14) {
						ids.add(id);
					}

					final ChildItem childItem = new ChildItem();
					childItem.Name = cursor.getString(ArtistQuery.ARTIST_NAME);
					childItem.Description = cursor.getString(ArtistQuery.ARTIST_DESCRIPTION);
					artists.add(childItem);
				}
				createGroup(R.string.artists, artists);

				if (ids.size() > 0) {
					Integer[] array = new Integer[ids.size()];
					ids.toArray(array);
					new ArtistTask().execute(array);
				}
			}

			adapter = new SimpleExpandableListAdapter(this, groupData, R.layout.grouprow, new String[] { NAME, COUNT },
					new int[] { R.id.name, R.id.count }, childData, R.layout.childrow, new String[] { NAME },
					new int[] { R.id.name });
			setListAdapter(adapter);
		} finally {
			cursor.close();
		}
	}

	private void createGroup(int nameId, Collection<ChildItem> children) {
		Map<String, String> groupMap = new HashMap<String, String>();
		groupData.add(groupMap);
		groupMap.put(NAME, getResources().getString(nameId));
		groupMap.put(COUNT, "" + children.size());

		List<Map<String, String>> childrenMap = new ArrayList<Map<String, String>>();
		for (ChildItem child : children) {
			Map<String, String> childMap = new HashMap<String, String>();
			childrenMap.add(childMap);
			childMap.put(NAME, child.Name);
			childMap.put(DESCRIPTION, child.Description);
		}
		childData.add(childrenMap);
	}

	class ChildItem {
		String Name;
		String Description;
	}

	class DesignerObserver extends ContentObserver {
		public DesignerObserver(Handler handler) {
			super(handler);
		}

		@Override
		public void onChange(boolean selfChange) {
			// Log.d(TAG, "Caught changed URI = " + mDesignersUri);
			mHandler.startQuery(TOKEN_DESIGNERS_UPDATE, mDesignersUri, DesignerQuery.PROJECTION);
		}
	}

	class ArtistObserver extends ContentObserver {
		public ArtistObserver(Handler handler) {
			super(handler);
		}

		@Override
		public void onChange(boolean selfChange) {
			mHandler.startQuery(TOKEN_DESIGNERS_UPDATE, mArtistsUri, ArtistQuery.PROJECTION);
		}
	}

	class DesignerTask extends AsyncTask<Integer, Void, Void> {

		private HttpClient mHttpClient;
		private RemoteExecutor mExecutor;

		@Override
		protected void onPreExecute() {
			mHttpClient = HttpUtils.createHttpClient(GameListsActivityTab.this, true);
			mExecutor = new RemoteExecutor(mHttpClient, getContentResolver());
		}

		@Override
		protected Void doInBackground(Integer... params) {
			for (int designerId : params) {
				Log.d(TAG, "Fetching designer ID = " + designerId);
				final String url = HttpUtils.constructDesignerUrl(designerId);
				try {
					mExecutor.executeGet(url, new RemoteDesignerHandler(designerId));
				} catch (HandlerException e) {
					Log.e(TAG, "Exception trying to fetch designer ID = " + designerId, e);
					runOnUiThread(new Runnable() {
						public void run() {
							showToast(R.string.msg_error_remote);
						}
					});
				}
			}
			return null;
		}
	}

	class ArtistTask extends AsyncTask<Integer, Void, Void> {

		private HttpClient mHttpClient;
		private RemoteExecutor mExecutor;

		@Override
		protected void onPreExecute() {
			mHttpClient = HttpUtils.createHttpClient(GameListsActivityTab.this, true);
			mExecutor = new RemoteExecutor(mHttpClient, getContentResolver());
		}

		@Override
		protected Void doInBackground(Integer... params) {
			for (int artistId : params) {
				Log.d(TAG, "Fetching artist ID = " + artistId);
				final String url = HttpUtils.constructArtistUrl(artistId);
				try {
					mExecutor.executeGet(url, new RemoteArtistHandler(artistId));
				} catch (HandlerException e) {
					Log.e(TAG, "Exception trying to fetch artist ID = " + artistId, e);
					runOnUiThread(new Runnable() {
						public void run() {
							showToast(R.string.msg_error_remote);
						}
					});
				}
			}
			return null;
		}
	}

	private void showToast(int messageId) {
		Toast.makeText(this, messageId, Toast.LENGTH_SHORT).show();
	}

	private interface DesignerQuery {
		String[] PROJECTION = { GamesDesigners.DESIGNER_ID, Designers.DESIGNER_NAME, Designers.DESIGNER_DESCRIPTION,
				SyncColumns.UPDATED };

		int DESIGNER_ID = 0;
		int DESIGNER_NAME = 1;
		int DESIGNER_DESCRIPTION = 2;
		int UPDATED = 3;
	}

	private interface ArtistQuery {
		String[] PROJECTION = { GamesArtists.ARTIST_ID, Artists.ARTIST_NAME, Artists.ARTIST_DESCRIPTION,
				SyncColumns.UPDATED };

		int ARTIST_ID = 0;
		int ARTIST_NAME = 1;
		int ARTIST_DESCRIPTION = 2;
		int UPDATED = 3;
	}
}
