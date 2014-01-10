package com.boardgamegeek.ui;

import static com.boardgamegeek.util.LogUtils.LOGD;
import static com.boardgamegeek.util.LogUtils.makeLogTag;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.support.v4.widget.SimpleCursorAdapter;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.provider.BggContract.Artists;
import com.boardgamegeek.provider.BggContract.Categories;
import com.boardgamegeek.provider.BggContract.Designers;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.provider.BggContract.GamesExpansions;
import com.boardgamegeek.provider.BggContract.Mechanics;
import com.boardgamegeek.provider.BggContract.Publishers;
import com.boardgamegeek.util.ActivityUtils;
import com.boardgamegeek.util.UIUtils;

public class GameDetailFragment extends BggListFragment implements LoaderManager.LoaderCallbacks<Cursor> {
	private static final String TAG = makeLogTag(GameDetailFragment.class);
	private CursorAdapter mAdapter;
	private int mGameId;
	private int mQueryToken;
	private Query mQuery;

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		view.setBackgroundColor(Color.WHITE);
		final ListView listView = getListView();
		listView.setSelector(android.R.color.transparent);
		listView.setCacheColorHint(Color.WHITE);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		setEmptyText(getString(R.string.empty_colors));

		final Intent intent = UIUtils.fragmentArgumentsToIntent(getArguments());
		mGameId = intent.getIntExtra(ActivityUtils.KEY_GAME_ID, BggContract.INVALID_ID);
		mQueryToken = intent.getIntExtra(ActivityUtils.KEY_QUERY_TOKEN, BggContract.INVALID_ID);

		determineQuery();
		if (mQueryToken != BggContract.INVALID_ID) {
			getLoaderManager().restartLoader(mQueryToken, getArguments(), this);
		} else {
			Toast.makeText(getActivity(), "Oops! " + mQueryToken, Toast.LENGTH_SHORT).show();
		}
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle data) {
		return new CursorLoader(getActivity(), mQuery.getUri(), mQuery.getProjection(), mQuery.getSelection(),
			mQuery.getSelectionArgs(), null);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		if (getActivity() == null) {
			return;
		}

		if (mAdapter == null) {
			mAdapter = new SimpleCursorAdapter(getActivity(), R.layout.row_game_detail, null, mQuery.getFrom(),
				new int[] { R.id.name }, 0);
			setListAdapter(mAdapter);
		}

		int token = loader.getId();
		if (token == mQueryToken) {
			mAdapter.changeCursor(cursor);
		} else {
			LOGD(TAG, "Query complete, Not Actionable: " + token);
			cursor.close();
		}

		if (isResumed()) {
			setListShown(true);
		} else {
			setListShownNoAnimation(true);
		}
	}

	@Override
	public void onLoaderReset(Loader<Cursor> arg0) {
		mAdapter.changeCursor(null);
	}

	private void determineQuery() {
		switch (mQueryToken) {
			case DesignerQuery._TOKEN:
				mQuery = new DesignerQuery();
				break;
			case ArtistQuery._TOKEN:
				mQuery = new ArtistQuery();
				break;
			case PublisherQuery._TOKEN:
				mQuery = new PublisherQuery();
				break;
			case CategoryQuery._TOKEN:
				mQuery = new CategoryQuery();
				break;
			case MechanicQuery._TOKEN:
				mQuery = new MechanicQuery();
				break;
			case ExpansionQuery._TOKEN:
				mQuery = new ExpansionQuery();
				break;
			case BaseGameQuery._TOKEN:
				mQuery = new BaseGameQuery();
				break;
			default:
				mQueryToken = BggContract.INVALID_ID;
				break;
		}
	}

	interface Query {
		String[] getProjection();

		String[] getFrom();

		Uri getUri();

		String getSelection();

		String[] getSelectionArgs();
	}

	abstract class BaseQuery implements Query {
		@Override
		public String getSelection() {
			return null;
		}

		@Override
		public String[] getSelectionArgs() {
			return null;
		}
	}

	private class DesignerQuery extends BaseQuery {
		static final int _TOKEN = 1;

		@Override
		public String[] getProjection() {
			return new String[] { Designers.DESIGNER_ID, Designers.DESIGNER_NAME, Designers._ID };
		}

		@Override
		public String[] getFrom() {
			return new String[] { Designers.DESIGNER_NAME };
		}

		@Override
		public Uri getUri() {
			return Games.buildDesignersUri(mGameId);
		}
	}

	private class ArtistQuery extends BaseQuery {
		static final int _TOKEN = 2;

		@Override
		public String[] getProjection() {
			return new String[] { Artists.ARTIST_ID, Artists.ARTIST_NAME, Artists._ID };
		}

		@Override
		public String[] getFrom() {
			return new String[] { Artists.ARTIST_NAME };
		}

		@Override
		public Uri getUri() {
			return Games.buildArtistsUri(mGameId);
		}
	}

	private class PublisherQuery extends BaseQuery {
		static final int _TOKEN = 3;

		@Override
		public String[] getProjection() {
			return new String[] { Publishers.PUBLISHER_ID, Publishers.PUBLISHER_NAME, Publishers._ID };
		}

		@Override
		public String[] getFrom() {
			return new String[] { Publishers.PUBLISHER_NAME };
		}

		@Override
		public Uri getUri() {
			return Games.buildPublishersUri(mGameId);
		}
	}

	private class CategoryQuery extends BaseQuery {
		static final int _TOKEN = 4;

		@Override
		public String[] getProjection() {
			return new String[] { Categories.CATEGORY_ID, Categories.CATEGORY_NAME, Categories._ID };
		}

		@Override
		public String[] getFrom() {
			return new String[] { Categories.CATEGORY_NAME };
		}

		@Override
		public Uri getUri() {
			return Games.buildCategoriesUri(mGameId);
		}
	}

	private class MechanicQuery extends BaseQuery {
		static final int _TOKEN = 5;

		@Override
		public String[] getProjection() {
			return new String[] { Mechanics.MECHANIC_ID, Mechanics.MECHANIC_NAME, Mechanics._ID };
		}

		@Override
		public String[] getFrom() {
			return new String[] { Mechanics.MECHANIC_NAME };
		}

		@Override
		public Uri getUri() {
			return Games.buildMechanicsUri(mGameId);
		}
	}

	private class ExpansionBaseQuery extends BaseQuery {
		@Override
		public String[] getProjection() {
			return new String[] { GamesExpansions.EXPANSION_ID, GamesExpansions.EXPANSION_NAME, GamesExpansions._ID };
		}

		@Override
		public String[] getFrom() {
			return new String[] { GamesExpansions.EXPANSION_NAME };
		}

		@Override
		public Uri getUri() {
			return Games.buildExpansionsUri(mGameId);
		}

		public String getSelection() {
			return GamesExpansions.INBOUND + "=?";
		}
	}

	private class ExpansionQuery extends ExpansionBaseQuery {
		static final int _TOKEN = 6;

		public String[] getSelectionArgs() {
			return new String[] { "0" };
		}
	}

	private class BaseGameQuery extends ExpansionBaseQuery {
		static final int _TOKEN = 7;

		public String[] getSelectionArgs() {
			return new String[] { "1" };
		}
	}
}
