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
import com.boardgamegeek.io.RemotePublisherHandler;
import com.boardgamegeek.io.XmlHandler.HandlerException;
import com.boardgamegeek.provider.BggContract.Artists;
import com.boardgamegeek.provider.BggContract.Categories;
import com.boardgamegeek.provider.BggContract.Designers;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.provider.BggContract.Mechanics;
import com.boardgamegeek.provider.BggContract.Publishers;
import com.boardgamegeek.provider.BggContract.SyncColumns;
import com.boardgamegeek.provider.BggDatabase.GamesArtists;
import com.boardgamegeek.provider.BggDatabase.GamesDesigners;
import com.boardgamegeek.provider.BggDatabase.GamesPublishers;
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
	private static final int TOKEN_ARTISTS_UPDATE = 4;
	private static final int TOKEN_PUBLISHERS = 5;
	private static final int TOKEN_PUBLISHERS_UPDATE = 6;
	private static final int TOKEN_MECHANICS = 7;
	private static final int TOKEN_CATEGORIES = 8;

	private static final int GROUP_DESIGNERS = 0;
	private static final int GROUP_ARTISTS = 1;
	private static final int GROUP_PUBLISHERS = 2;
	private static final int GROUP_MECHANICS = 3;
	private static final int GROUP_CATEGORIES = 4;

	private static final String KEY_NAME = "NAME";
	private static final String KEY_COUNT = "COUNT";
	private static final String KEY_DESCRIPTION = "DESCRIPTION";

	private int mPadding;
	private Uri mDesignersUri;
	private Uri mArtistsUri;
	private Uri mPublishersUri;
	private Uri mMechanicsUri;
	private Uri mCategoriesUri;
	private NotifyingAsyncQueryHandler mHandler;

	private List<Map<String, String>> mGroupData;
	private List<List<Map<String, String>>> mChildData;
	private ExpandableListAdapter mAdapter;

	private String mName;
	private String mDescription;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mPadding = (int) getResources().getDimension(R.dimen.padding_standard);
		initializeGroupData();
		setAndObserveUris();

		mHandler = new NotifyingAsyncQueryHandler(getContentResolver(), this);
		mHandler.startQuery(TOKEN_DESIGNERS, mDesignersUri, DesignerQuery.PROJECTION);
		mHandler.startQuery(TOKEN_ARTISTS, mArtistsUri, ArtistQuery.PROJECTION);
		mHandler.startQuery(TOKEN_PUBLISHERS, mPublishersUri, PublisherQuery.PROJECTION);
		mHandler.startQuery(TOKEN_MECHANICS, mMechanicsUri, MechanicQuery.PROJECTION);
		mHandler.startQuery(TOKEN_CATEGORIES, mCategoriesUri, CategoryQuery.PROJECTION);
	}

	private void setAndObserveUris() {
		final Uri gameUri = getIntent().getData();
		final int gameId = Games.getGameId(gameUri);
		mDesignersUri = Games.buildDesignersUri(gameId);
		mArtistsUri = Games.buildArtistsUri(gameId);
		mPublishersUri = Games.buildPublishersUri(gameId);
		mMechanicsUri = Games.buildMechanicsUri(gameId);
		mCategoriesUri = Games.buildCategoriesUri(gameId);

		getContentResolver().registerContentObserver(mDesignersUri, true, new DesignersObserver(null));
		getContentResolver().registerContentObserver(mArtistsUri, true, new ArtistsObserver(null));
		getContentResolver().registerContentObserver(mPublishersUri, true, new PublishersObserver(null));
		getContentResolver().registerContentObserver(mMechanicsUri, true, new MechanicsObserver(null));
		getContentResolver().registerContentObserver(mCategoriesUri, true, new CategoriesObserver(null));
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {

		removeDialog(ID_DIALOG_RESULTS);

		Map<String, Object> childItem = (Map<String, Object>) mAdapter.getChild(groupPosition, childPosition);
		mName = (String) childItem.get(KEY_NAME);
		mDescription = (String) childItem.get(KEY_DESCRIPTION);
		if (TextUtils.isEmpty(mDescription)) {
			showToast(R.string.msg_no_description);
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
			TextView textView = new TextView(this);
			textView.setTextColor(Color.WHITE);
			textView.setAutoLinkMask(Linkify.ALL);
			textView.setText(StringUtils.formatDescription(mDescription));
			ScrollView scrollView = new ScrollView(this);
			scrollView.setPadding(mPadding, mPadding, mPadding, mPadding);
			scrollView.addView(textView);
			dialog.setContentView(scrollView);
			dialog.setCancelable(true);
			return dialog;
		}
		return super.onCreateDialog(id);
	}

	public void onQueryComplete(int token, Object cookie, Cursor cursor) {
		try {
			if (token == TOKEN_DESIGNERS || token == TOKEN_DESIGNERS_UPDATE) {
				List<Integer> ids = new ArrayList<Integer>();
				List<ChildItem> designers = new ArrayList<ChildItem>();
				while (cursor.moveToNext()) {
					if (token == TOKEN_DESIGNERS) {
						int id = cursor.getInt(DesignerQuery.DESIGNER_ID);
						getContentResolver().registerContentObserver(Designers.buildDesignerUri(id), true,
								new DesignerObserver(null));
						addId(cursor, ids, id, DesignerQuery.UPDATED);
					}
					addChildItem(cursor, designers, DesignerQuery.DESIGNER_NAME, DesignerQuery.DESIGNER_DESCRIPTION);
				}
				updateGroup(GROUP_DESIGNERS, designers);

				if (ids.size() > 0) {
					Integer[] array = new Integer[ids.size()];
					ids.toArray(array);
					new DesignerTask().execute(array);
				}
			} else if (token == TOKEN_ARTISTS || token == TOKEN_ARTISTS_UPDATE) {
				List<Integer> ids = new ArrayList<Integer>();
				List<ChildItem> artists = new ArrayList<ChildItem>();
				while (cursor.moveToNext()) {
					if (token == TOKEN_ARTISTS) {
						int id = cursor.getInt(ArtistQuery.ARTIST_ID);
						getContentResolver().registerContentObserver(Artists.buildArtistUri(id), true,
								new ArtistObserver(null));
						addId(cursor, ids, id, ArtistQuery.UPDATED);
					}
					addChildItem(cursor, artists, ArtistQuery.ARTIST_NAME, ArtistQuery.ARTIST_DESCRIPTION);
				}
				updateGroup(GROUP_ARTISTS, artists);

				if (ids.size() > 0) {
					Integer[] array = new Integer[ids.size()];
					ids.toArray(array);
					new ArtistTask().execute(array);
				}
			} else if (token == TOKEN_PUBLISHERS || token == TOKEN_PUBLISHERS_UPDATE) {
				List<Integer> ids = new ArrayList<Integer>();
				List<ChildItem> publishers = new ArrayList<ChildItem>();
				while (cursor.moveToNext()) {
					if (token == TOKEN_PUBLISHERS) {
						int id = cursor.getInt(PublisherQuery.PUBLISHER_ID);
						getContentResolver().registerContentObserver(Publishers.buildPublisherUri(id), true,
								new PublisherObserver(null));
						addId(cursor, ids, id, PublisherQuery.UPDATED);
					}
					addChildItem(cursor, publishers, PublisherQuery.PUBLISHER_NAME,
							PublisherQuery.PUBLISHER_DESCRIPTION);
				}
				updateGroup(GROUP_PUBLISHERS, publishers);

				if (ids.size() > 0) {
					Integer[] array = new Integer[ids.size()];
					ids.toArray(array);
					new PublisherTask().execute(array);
				}
			} else if (token == TOKEN_MECHANICS) {
				List<ChildItem> mechanics = new ArrayList<ChildItem>();
				while (cursor.moveToNext()) {
					addChildItem(cursor, mechanics, MechanicQuery.MECHANIC_NAME);
				}
				updateGroup(GROUP_MECHANICS, mechanics);
			} else if (token == TOKEN_CATEGORIES) {
				List<ChildItem> categories = new ArrayList<ChildItem>();
				while (cursor.moveToNext()) {
					addChildItem(cursor, categories, CategoryQuery.CATEGORY_NAME);
				}
				 updateGroup(GROUP_CATEGORIES, categories);
			}

			mAdapter = new SimpleExpandableListAdapter(this, mGroupData, R.layout.grouprow, new String[] { KEY_NAME,
					KEY_COUNT }, new int[] { R.id.name, R.id.count }, mChildData, R.layout.childrow,
					new String[] { KEY_NAME }, new int[] { R.id.name });
			setListAdapter(mAdapter);
		} finally {
			cursor.close();
		}
	}

	private void addId(Cursor cursor, List<Integer> list, int id, int updatedColumnIndex) {
		long lastUpdated = cursor.getLong(updatedColumnIndex);
		if (lastUpdated == 0 || DateTimeUtils.howManyDaysOld(lastUpdated) > 14) {
			list.add(id);
		}
	}

	private void addChildItem(Cursor cursor, List<ChildItem> list, int nameColumnIndex, int descriptionColumnIndex) {
		final ChildItem childItem = new ChildItem();
		childItem.Name = cursor.getString(nameColumnIndex);
		childItem.Description = cursor.getString(descriptionColumnIndex);
		list.add(childItem);
	}

	private void addChildItem(Cursor cursor, List<ChildItem> list, int nameColumnIndex) {
		final ChildItem childItem = new ChildItem();
		childItem.Name = cursor.getString(nameColumnIndex);
		childItem.Description = null;
		list.add(childItem);
	}

	private void initializeGroupData() {
		mGroupData = new ArrayList<Map<String, String>>();
		mChildData = new ArrayList<List<Map<String, String>>>();

		createGroup(R.string.designers);
		createGroup(R.string.artists);
		createGroup(R.string.publishers);
		createGroup(R.string.mechanics);
		createGroup(R.string.categories);
	}

	private void createGroup(int nameResourceId) {
		Map<String, String> groupMap = new HashMap<String, String>();
		groupMap.put(KEY_NAME, getResources().getString(nameResourceId));
		groupMap.put(KEY_COUNT, "0");
		mGroupData.add(groupMap);
		mChildData.add(new ArrayList<Map<String, String>>());
	}

	private void updateGroup(int group, Collection<ChildItem> children) {
		mGroupData.get(group).put(KEY_COUNT, "" + children.size());

		List<Map<String, String>> childList = mChildData.get(group);
		childList.clear();
		for (ChildItem child : children) {
			Map<String, String> childMap = new HashMap<String, String>();
			childList.add(childMap);
			childMap.put(KEY_NAME, child.Name);
			childMap.put(KEY_DESCRIPTION, child.Description);
		}
	}

	class ChildItem {
		String Name;
		String Description;
	}

	class DesignersObserver extends ContentObserver {
		public DesignersObserver(Handler handler) {
			super(handler);
		}

		@Override
		public void onChange(boolean selfChange) {
			mHandler.startQuery(TOKEN_DESIGNERS, mDesignersUri, DesignerQuery.PROJECTION);
		}
	}

	class DesignerObserver extends ContentObserver {
		public DesignerObserver(Handler handler) {
			super(handler);
		}

		@Override
		public void onChange(boolean selfChange) {
			mHandler.startQuery(TOKEN_DESIGNERS_UPDATE, mDesignersUri, DesignerQuery.PROJECTION);
		}
	}

	class ArtistsObserver extends ContentObserver {
		public ArtistsObserver(Handler handler) {
			super(handler);
		}

		@Override
		public void onChange(boolean selfChange) {
			mHandler.startQuery(TOKEN_ARTISTS, mArtistsUri, ArtistQuery.PROJECTION);
		}
	}

	class ArtistObserver extends ContentObserver {
		public ArtistObserver(Handler handler) {
			super(handler);
		}

		@Override
		public void onChange(boolean selfChange) {
			mHandler.startQuery(TOKEN_ARTISTS_UPDATE, mArtistsUri, ArtistQuery.PROJECTION);
		}
	}

	class PublishersObserver extends ContentObserver {
		public PublishersObserver(Handler handler) {
			super(handler);
		}

		@Override
		public void onChange(boolean selfChange) {
			mHandler.startQuery(TOKEN_PUBLISHERS, mPublishersUri, PublisherQuery.PROJECTION);
		}
	}

	class PublisherObserver extends ContentObserver {
		public PublisherObserver(Handler handler) {
			super(handler);
		}

		@Override
		public void onChange(boolean selfChange) {
			mHandler.startQuery(TOKEN_PUBLISHERS_UPDATE, mPublishersUri, PublisherQuery.PROJECTION);
		}
	}

	class MechanicsObserver extends ContentObserver {
		public MechanicsObserver(Handler handler) {
			super(handler);
		}

		@Override
		public void onChange(boolean selfChange) {
			mHandler.startQuery(TOKEN_MECHANICS, mMechanicsUri, MechanicQuery.PROJECTION);
		}
	}

	class CategoriesObserver extends ContentObserver {
		public CategoriesObserver(Handler handler) {
			super(handler);
		}

		@Override
		public void onChange(boolean selfChange) {
			mHandler.startQuery(TOKEN_CATEGORIES, mCategoriesUri, CategoryQuery.PROJECTION);
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

	class PublisherTask extends AsyncTask<Integer, Void, Void> {

		private HttpClient mHttpClient;
		private RemoteExecutor mExecutor;

		@Override
		protected void onPreExecute() {
			mHttpClient = HttpUtils.createHttpClient(GameListsActivityTab.this, true);
			mExecutor = new RemoteExecutor(mHttpClient, getContentResolver());
		}

		@Override
		protected Void doInBackground(Integer... params) {
			for (int publisherId : params) {
				Log.d(TAG, "Fetching publisher ID = " + publisherId);
				final String url = HttpUtils.constructPublisherUrl(publisherId);
				try {
					mExecutor.executeGet(url, new RemotePublisherHandler(publisherId));
				} catch (HandlerException e) {
					Log.e(TAG, "Exception trying to fetch publisher ID = " + publisherId, e);
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

	private interface PublisherQuery {
		String[] PROJECTION = { GamesPublishers.PUBLISHER_ID, Publishers.PUBLISHER_NAME,
				Publishers.PUBLISHER_DESCRIPTION, SyncColumns.UPDATED };

		int PUBLISHER_ID = 0;
		int PUBLISHER_NAME = 1;
		int PUBLISHER_DESCRIPTION = 2;
		int UPDATED = 3;
	}

	private interface MechanicQuery {
		String[] PROJECTION = { Mechanics.MECHANIC_NAME };

		int MECHANIC_NAME = 0;
	}

	private interface CategoryQuery {
		String[] PROJECTION = { Categories.CATEGORY_NAME };

		int CATEGORY_NAME = 0;
	}
}
