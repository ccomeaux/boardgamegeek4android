package com.boardgamegeek.ui;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v4.widget.SwipeRefreshLayout.OnRefreshListener;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.events.UpdateCompleteEvent;
import com.boardgamegeek.events.UpdateEvent;
import com.boardgamegeek.provider.BggContract.Artists;
import com.boardgamegeek.provider.BggContract.Designers;
import com.boardgamegeek.provider.BggContract.Publishers;
import com.boardgamegeek.service.UpdateService;
import com.boardgamegeek.util.DateTimeUtils;
import com.boardgamegeek.util.PresentationUtils;
import com.boardgamegeek.util.UIUtils;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import butterknife.Bind;
import butterknife.ButterKnife;
import hugo.weaving.DebugLog;

public class ProducerFragment extends Fragment implements LoaderCallbacks<Cursor>, OnRefreshListener {
	private static final int TIME_HINT_UPDATE_INTERVAL = 30000; // 30 sec

	private Handler mHandler = new Handler();
	private Runnable mUpdaterRunnable = null;
	private static final int AGE_IN_DAYS_TO_REFRESH = 30;
	private Uri mUri;
	private int mToken;
	private int mId;
	private boolean mSyncing;

	@SuppressWarnings("unused") @Bind(R.id.swipe_refresh) SwipeRefreshLayout mSwipeRefreshLayout;
	@SuppressWarnings("unused") @Bind(R.id.id) TextView mIdView;
	@SuppressWarnings("unused") @Bind(R.id.name) TextView mName;
	@SuppressWarnings("unused") @Bind(R.id.description) TextView mDescription;
	@SuppressWarnings("unused") @Bind(R.id.updated) TextView mUpdated;

	@Override
	@DebugLog
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mHandler = new Handler();
		final Intent intent = UIUtils.fragmentArgumentsToIntent(getArguments());
		mUri = intent.getData();

		if (Designers.isDesignerUri(mUri)) {
			mToken = DesignerQuery._TOKEN;
		} else if (Artists.isArtistUri(mUri)) {
			mToken = ArtistQuery._TOKEN;
		} else if (Publishers.isPublisherUri(mUri)) {
			mToken = PublisherQuery._TOKEN;
		}
	}

	@Override
	@DebugLog
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_producer, container, false);
		ButterKnife.bind(this, rootView);

		mSwipeRefreshLayout.setOnRefreshListener(this);
		mSwipeRefreshLayout.setColorSchemeResources(R.color.primary_dark, R.color.primary);

		getLoaderManager().restartLoader(mToken, null, this);
		return rootView;
	}

	@Override
	public void onStart() {
		super.onStart();
		EventBus.getDefault().register(this);
	}

	@Override
	public void onResume() {
		super.onResume();
		if (mUpdaterRunnable != null) {
			mHandler.postDelayed(mUpdaterRunnable, TIME_HINT_UPDATE_INTERVAL);
		}
	}


	@Override
	public void onPause() {
		super.onPause();
		if (mUpdaterRunnable != null) {
			mHandler.removeCallbacks(mUpdaterRunnable);
		}
	}

	@Override
	public void onStop() {
		EventBus.getDefault().unregister(this);
		super.onStop();
	}

	@Override
	@DebugLog
	public Loader<Cursor> onCreateLoader(int id, Bundle data) {
		CursorLoader loader = null;
		switch (id) {
			case DesignerQuery._TOKEN:
				loader = new CursorLoader(getActivity(), mUri, DesignerQuery.PROJECTION, null, null, null);
				break;
			case ArtistQuery._TOKEN:
				loader = new CursorLoader(getActivity(), mUri, ArtistQuery.PROJECTION, null, null, null);
				break;
			case PublisherQuery._TOKEN:
				loader = new CursorLoader(getActivity(), mUri, PublisherQuery.PROJECTION, null, null, null);
				break;
		}
		return loader;
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		if (getActivity() == null) {
			return;
		}

		if (loader.getId() == mToken) {
			if (cursor == null || !cursor.moveToFirst()) {
				return;
			}

			mId = cursor.getInt(Query.ID);
			String name = cursor.getString(Query.NAME);
			String description = cursor.getString(Query.DESCRIPTION);
			long updated = cursor.getLong(Query.UPDATED);

			mIdView.setText(String.format(getString(R.string.id_list_text), mId));
			mName.setText(name);
			UIUtils.setTextMaybeHtml(mDescription, description);
			mUpdated.setTag(updated);

			updateTimeBasedUi();
			if (mUpdaterRunnable != null) {
				mHandler.removeCallbacks(mUpdaterRunnable);
			}
			mUpdaterRunnable = new Runnable() {
				@Override
				public void run() {
					updateTimeBasedUi();
					mHandler.postDelayed(mUpdaterRunnable, TIME_HINT_UPDATE_INTERVAL);
				}
			};
			mHandler.postDelayed(mUpdaterRunnable, TIME_HINT_UPDATE_INTERVAL);

			if (updated == 0 || DateTimeUtils.howManyDaysOld(updated) > AGE_IN_DAYS_TO_REFRESH) {
				triggerRefresh();
			}
		} else {
			if (cursor != null) {
				cursor.close();
			}
		}
	}

	private void triggerRefresh() {
		UpdateService.start(getActivity(), mToken, mId);
	}

	private void updateTimeBasedUi() {
		if (!isAdded()) {
			return;
		}
		if (mUpdated != null) {
			long updated = (long) mUpdated.getTag();
			mUpdated.setText(PresentationUtils.describePastTimeSpan(updated, getResources().getString(R.string.text_unknown)));
		}
	}

	@Override
	@DebugLog
	public void onLoaderReset(Loader<Cursor> loader) {
	}

	@Override
	public void onRefresh() {
		triggerRefresh();
	}

	@SuppressWarnings("unused")
	@Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
	public void onEvent(UpdateEvent event) {
		mSyncing = event.getType() == mToken;
		updateRefreshStatus();
	}

	@SuppressWarnings({ "unused", "UnusedParameters" })
	@Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
	public void onEvent(UpdateCompleteEvent event) {
		mSyncing = false;
		updateRefreshStatus();
	}

	private void updateRefreshStatus() {
		if (mSwipeRefreshLayout != null) {
			mSwipeRefreshLayout.post(new Runnable() {
				@Override
				public void run() {
					mSwipeRefreshLayout.setRefreshing(mSyncing);
				}
			});
		}
	}

	private interface Query {
		int ID = 0;
		int NAME = 1;
		int DESCRIPTION = 2;
		int UPDATED = 3;
	}

	private interface DesignerQuery extends Query {
		int _TOKEN = UpdateService.SYNC_TYPE_DESIGNER;
		String[] PROJECTION = { Designers.DESIGNER_ID, Designers.DESIGNER_NAME, Designers.DESIGNER_DESCRIPTION,
			Designers.UPDATED };
	}

	private interface ArtistQuery extends Query {
		int _TOKEN = UpdateService.SYNC_TYPE_ARTIST;
		String[] PROJECTION = { Artists.ARTIST_ID, Artists.ARTIST_NAME, Artists.ARTIST_DESCRIPTION, Artists.UPDATED };
	}

	private interface PublisherQuery extends Query {
		int _TOKEN = UpdateService.SYNC_TYPE_PUBLISHER;
		String[] PROJECTION = { Publishers.PUBLISHER_ID, Publishers.PUBLISHER_NAME, Publishers.PUBLISHER_DESCRIPTION,
			Publishers.UPDATED };
	}
}
