package com.boardgamegeek.ui;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
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
import com.boardgamegeek.provider.BggContract.Artists;
import com.boardgamegeek.provider.BggContract.Designers;
import com.boardgamegeek.provider.BggContract.Publishers;
import com.boardgamegeek.tasks.SyncArtistTask;
import com.boardgamegeek.tasks.SyncDesignerTask;
import com.boardgamegeek.tasks.SyncPublisherTask;
import com.boardgamegeek.ui.widget.TimestampView;
import com.boardgamegeek.util.DateTimeUtils;
import com.boardgamegeek.util.PresentationUtils;
import com.boardgamegeek.util.TaskUtils;
import com.boardgamegeek.util.UIUtils;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import hugo.weaving.DebugLog;

public class ProducerFragment extends Fragment implements LoaderCallbacks<Cursor>, OnRefreshListener {
	private static final int AGE_IN_DAYS_TO_REFRESH = 30;
	private Uri uri;
	private int token;
	private int id;

	private Unbinder unbinder;
	@BindView(R.id.swipe_refresh) SwipeRefreshLayout swipeRefreshLayout;
	@BindView(R.id.id) TextView idView;
	@BindView(R.id.name) TextView nameView;
	@BindView(R.id.description) TextView descriptionView;
	@BindView(R.id.updated) TimestampView updatedView;

	@Override
	@DebugLog
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		final Intent intent = UIUtils.fragmentArgumentsToIntent(getArguments());
		uri = intent.getData();

		if (Designers.isDesignerUri(uri)) {
			token = DesignerQuery._TOKEN;
		} else if (Artists.isArtistUri(uri)) {
			token = ArtistQuery._TOKEN;
		} else if (Publishers.isPublisherUri(uri)) {
			token = PublisherQuery._TOKEN;
		}
	}

	@Override
	@DebugLog
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_producer, container, false);
		unbinder = ButterKnife.bind(this, rootView);

		swipeRefreshLayout.setOnRefreshListener(this);
		swipeRefreshLayout.setColorSchemeResources(PresentationUtils.getColorSchemeResources());

		getLoaderManager().restartLoader(token, null, this);
		return rootView;
	}

	@DebugLog
	@Override
	public void onStart() {
		super.onStart();
		EventBus.getDefault().register(this);
	}

	@DebugLog
	@Override
	public void onStop() {
		EventBus.getDefault().unregister(this);
		super.onStop();
	}

	@DebugLog
	@Override
	public void onDestroyView() {
		super.onDestroyView();
		if (unbinder != null) unbinder.unbind();
	}

	@Override
	@DebugLog
	public Loader<Cursor> onCreateLoader(int id, Bundle data) {
		CursorLoader loader = null;
		switch (id) {
			case DesignerQuery._TOKEN:
				loader = new CursorLoader(getActivity(), uri, DesignerQuery.PROJECTION, null, null, null);
				break;
			case ArtistQuery._TOKEN:
				loader = new CursorLoader(getActivity(), uri, ArtistQuery.PROJECTION, null, null, null);
				break;
			case PublisherQuery._TOKEN:
				loader = new CursorLoader(getActivity(), uri, PublisherQuery.PROJECTION, null, null, null);
				break;
		}
		return loader;
	}

	@DebugLog
	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		if (getActivity() == null) {
			return;
		}

		if (loader.getId() == token) {
			if (cursor == null || !cursor.moveToFirst()) {
				return;
			}

			id = cursor.getInt(Query.ID);
			String name = cursor.getString(Query.NAME);
			String description = cursor.getString(Query.DESCRIPTION);
			long updated = cursor.getLong(Query.UPDATED);

			idView.setText(String.format(getString(R.string.id_list_text), String.valueOf(id)));
			nameView.setText(name);
			UIUtils.setTextMaybeHtml(descriptionView, description);
			updatedView.setTimestamp(updated);

			if (updated == 0 || DateTimeUtils.howManyDaysOld(updated) > AGE_IN_DAYS_TO_REFRESH) {
				triggerRefresh();
			}
		} else {
			if (cursor != null) cursor.close();
		}
	}

	@DebugLog
	private void triggerRefresh() {
		if (token == DesignerQuery._TOKEN) {
			TaskUtils.executeAsyncTask(new SyncDesignerTask(getContext(), id));
			updateRefreshStatus(true);
		} else if (token == ArtistQuery._TOKEN) {
			TaskUtils.executeAsyncTask(new SyncArtistTask(getContext(), id));
			updateRefreshStatus(true);
		} else if (token == PublisherQuery._TOKEN) {
			TaskUtils.executeAsyncTask(new SyncPublisherTask(getContext(), id));
			updateRefreshStatus(true);
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

	@DebugLog
	private void updateRefreshStatus(final boolean isSyncing) {
		if (swipeRefreshLayout != null) {
			swipeRefreshLayout.post(new Runnable() {
				@Override
				public void run() {
					if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(isSyncing);
				}
			});
		}
	}

	@SuppressWarnings("unused")
	@DebugLog
	@Subscribe(threadMode = ThreadMode.MAIN)
	public void onEvent(SyncDesignerTask.Event event) {
		if (event.getDesignerId() == id) {
			updateRefreshStatus(false);
		}
	}

	@SuppressWarnings("unused")
	@DebugLog
	@Subscribe(threadMode = ThreadMode.MAIN)
	public void onEvent(SyncArtistTask.Event event) {
		if (event.getArtistId() == id) {
			updateRefreshStatus(false);
		}
	}

	@SuppressWarnings("unused")
	@DebugLog
	@Subscribe(threadMode = ThreadMode.MAIN)
	public void onEvent(SyncPublisherTask.Event event) {
		if (event.getPublisherId() == id) {
			updateRefreshStatus(false);
		}
	}

	private interface Query {
		int ID = 0;
		int NAME = 1;
		int DESCRIPTION = 2;
		int UPDATED = 3;
	}

	private interface DesignerQuery extends Query {
		int _TOKEN = 1;
		String[] PROJECTION = { Designers.DESIGNER_ID, Designers.DESIGNER_NAME, Designers.DESIGNER_DESCRIPTION, Designers.UPDATED };
	}

	private interface ArtistQuery extends Query {
		int _TOKEN = 2;
		String[] PROJECTION = { Artists.ARTIST_ID, Artists.ARTIST_NAME, Artists.ARTIST_DESCRIPTION, Artists.UPDATED };
	}

	private interface PublisherQuery extends Query {
		int _TOKEN = 3;
		String[] PROJECTION = { Publishers.PUBLISHER_ID, Publishers.PUBLISHER_NAME, Publishers.PUBLISHER_DESCRIPTION, Publishers.UPDATED };
	}
}
