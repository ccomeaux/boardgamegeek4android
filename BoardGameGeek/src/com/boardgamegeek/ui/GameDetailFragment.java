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
	private String[] mProjection;
	private String[] mFrom;
	private Uri mUri;
	private String mSelection;
	private String[] mSelectionArgs;

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
		return new CursorLoader(getActivity(), mUri, mProjection, mSelection, mSelectionArgs, null);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		if (getActivity() == null) {
			return;
		}

		if (mAdapter == null) {
			mAdapter = new SimpleCursorAdapter(getActivity(), R.layout.row_game_detail, null, mFrom,
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
				mUri = Games.buildDesignersUri(mGameId);
				mSelection = null;
				mSelectionArgs = null;
				mProjection = DesignerQuery.PROJECTION;
				mFrom = DesignerQuery.FROM;
				break;
			case ArtistQuery._TOKEN:
				mUri = Games.buildArtistsUri(mGameId);
				mSelection = null;
				mSelectionArgs = null;
				mProjection = ArtistQuery.PROJECTION;
				mFrom = ArtistQuery.FROM;
				break;
			case PublisherQuery._TOKEN:
				mUri = Games.buildPublishersUri(mGameId);
				mSelection = null;
				mSelectionArgs = null;
				mProjection = PublisherQuery.PROJECTION;
				mFrom = PublisherQuery.FROM;
				break;
			case CategoryQuery._TOKEN:
				mUri = Games.buildCategoriesUri(mGameId);
				mSelection = null;
				mSelectionArgs = null;
				mProjection = CategoryQuery.PROJECTION;
				mFrom = CategoryQuery.FROM;
				break;
			case MechanicQuery._TOKEN:
				mUri = Games.buildMechanicsUri(mGameId);
				mSelection = null;
				mSelectionArgs = null;
				mProjection = MechanicQuery.PROJECTION;
				mFrom = MechanicQuery.FROM;
				break;
			case ExpansionQuery._TOKEN:
				mUri = Games.buildExpansionsUri(mGameId);
				mSelection = GamesExpansions.INBOUND + "=?";
				mSelectionArgs = new String[] { "0" };
				mProjection = ExpansionQuery.PROJECTION;
				mFrom = ExpansionQuery.FROM;
				break;
			case BaseGameQuery._TOKEN:
				mUri = Games.buildExpansionsUri(mGameId);
				mSelection = GamesExpansions.INBOUND + "=?";
				mSelectionArgs = new String[] { "1" };
				mProjection = BaseGameQuery.PROJECTION;
				mFrom = BaseGameQuery.FROM;
				break;
			default:
				mQueryToken = BggContract.INVALID_ID;
				break;
		}
	}

	private interface DesignerQuery {
		int _TOKEN = 1;
		String[] PROJECTION = { Designers.DESIGNER_ID, Designers.DESIGNER_NAME, Designers._ID };
		String[] FROM = { Designers.DESIGNER_NAME };
	}

	private interface ArtistQuery {
		int _TOKEN = 2;
		String[] PROJECTION = { Artists.ARTIST_ID, Artists.ARTIST_NAME, Artists._ID };
		String[] FROM = { Artists.ARTIST_NAME };
	}

	private interface PublisherQuery {
		int _TOKEN = 3;
		String[] PROJECTION = { Publishers.PUBLISHER_ID, Publishers.PUBLISHER_NAME, Publishers._ID };
		String[] FROM = { Publishers.PUBLISHER_NAME };
	}

	private interface CategoryQuery {
		int _TOKEN = 4;
		String[] PROJECTION = { Categories.CATEGORY_ID, Categories.CATEGORY_NAME, Categories._ID };
		String[] FROM = { Categories.CATEGORY_NAME };
	}

	private interface MechanicQuery {
		int _TOKEN = 5;
		String[] PROJECTION = { Mechanics.MECHANIC_ID, Mechanics.MECHANIC_NAME, Mechanics._ID };
		String[] FROM = { Mechanics.MECHANIC_NAME };
	}

	private interface ExpansionQuery {
		int _TOKEN = 6;
		String[] PROJECTION = { GamesExpansions.EXPANSION_ID, GamesExpansions.EXPANSION_NAME, GamesExpansions._ID };
		String[] FROM = { GamesExpansions.EXPANSION_NAME };
	}

	private interface BaseGameQuery {
		int _TOKEN = 7;
		String[] PROJECTION = { GamesExpansions.EXPANSION_ID, GamesExpansions.EXPANSION_NAME, GamesExpansions._ID };
		String[] FROM = { GamesExpansions.EXPANSION_NAME };
	}
}
