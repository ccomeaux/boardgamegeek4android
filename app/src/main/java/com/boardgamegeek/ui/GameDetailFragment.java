package com.boardgamegeek.ui;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TextView;

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
import com.boardgamegeek.util.AnimationUtils;
import com.boardgamegeek.util.UIUtils;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import timber.log.Timber;

public class GameDetailFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
	private static final String KEY_GAME_ID = "GAME_ID";
	private static final String KEY_QUERY_TOKEN = "QUERY_TOKEN";
	private GameDetailAdapter adapter;
	private int gameId;
	private int queryToken;
	private Query query;

	Unbinder unbinder;
	@BindView(R.id.root_container) CoordinatorLayout containerView;
	@BindView(android.R.id.progress) View progressView;
	@BindView(android.R.id.empty) TextView emptyView;
	@BindView(android.R.id.list) RecyclerView recyclerView;

	public static GameDetailFragment newInstance(int gameId, int queryToken) {
		Bundle args = new Bundle();
		args.putInt(KEY_GAME_ID, gameId);
		args.putInt(KEY_QUERY_TOKEN, queryToken);
		GameDetailFragment fragment = new GameDetailFragment();
		fragment.setArguments(args);
		return fragment;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_game_details, container, false);
		unbinder = ButterKnife.bind(this, rootView);
		setUpRecyclerView();
		return rootView;
	}

	private void readBundle(Bundle bundle) {
		gameId = bundle.getInt(KEY_GAME_ID);
		queryToken = bundle.getInt(KEY_QUERY_TOKEN);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		readBundle(getArguments());

		makeQuery();

		if (query != null) {
			getLoaderManager().restartLoader(queryToken, getArguments(), this);
		} else {
			final String message = getString(R.string.msg_invalid_token, String.valueOf(queryToken));
			Timber.w(message);
			emptyView.setText(message);
			AnimationUtils.fadeIn(emptyView);
			AnimationUtils.fadeOut(recyclerView);
			AnimationUtils.fadeOut(progressView);
		}
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		if (unbinder != null) unbinder.unbind();
	}

	private void setUpRecyclerView() {
		recyclerView.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false));
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle data) {
		return new CursorLoader(getActivity(), query.getUri(), query.getProjection(), query.getSelection(), query.getSelectionArgs(), null);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		if (getActivity() == null) {
			return;
		}

		if (adapter == null) {
			adapter = new GameDetailAdapter(getActivity(), cursor, query);
			recyclerView.setAdapter(adapter);
		}

		int token = loader.getId();
		if (token == queryToken) {
			adapter.changeCursor(cursor);
		} else {
			Timber.d("Query complete, Not Actionable: %s", token);
			cursor.close();
		}

		AnimationUtils.fadeOut(progressView);
		AnimationUtils.fadeIn(getActivity(), recyclerView, isResumed());
	}

	public class GameDetailAdapter extends RecyclerView.Adapter<GameDetailAdapter.DetailViewHolder> {
		private final LayoutInflater inflater;
		private Cursor cursor;
		private final Query query;

		public GameDetailAdapter(Context context, Cursor cursor, Query query) {
			this.cursor = cursor;
			this.query = query;
			inflater = LayoutInflater.from(context);
			setHasStableIds(true);
		}

		@Override
		public DetailViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			View view = inflater.inflate(R.layout.row_text, parent, false);
			return new DetailViewHolder(view);
		}

		@Override
		public void onBindViewHolder(DetailViewHolder holder, int position) {
			if (cursor.moveToPosition(position)) {
				holder.bind(cursor);
			}
		}

		@Override
		public int getItemCount() {
			return cursor.getCount();
		}

		@Override
		public long getItemId(int position) {
			if (cursor.moveToPosition(position)) {
				return cursor.getInt(cursor.getColumnIndex(query.getIdColumnName()));
			}
			return RecyclerView.NO_ID;
		}

		public class DetailViewHolder extends RecyclerView.ViewHolder {
			@BindView(android.R.id.title) TextView titleView;
			private Uri uri;

			public DetailViewHolder(View itemView) {
				super(itemView);
				ButterKnife.bind(this, itemView);
			}

			public void bind(final Cursor cursor) {
				titleView.setText(cursor.getString(cursor.getColumnIndex(query.getTitleColumnName())));
				uri = query.getUri(cursor);
				if (query.isClickable()) {
					itemView.setOnClickListener(new OnClickListener() {
						@Override
						public void onClick(View v) {
							if (uri != null) {
								getActivity().startActivity(new Intent(Intent.ACTION_VIEW, uri));
							}
						}
					});
				}
			}
		}

		public void changeCursor(Cursor cursor) {
			Cursor old = swapCursor(cursor);
			if (old != null) {
				old.close();
			}
		}

		public Cursor swapCursor(Cursor newCursor) {
			if (newCursor == cursor) {
				return null;
			}
			Cursor oldCursor = cursor;
			cursor = newCursor;
			if (newCursor != null) {
				notifyDataSetChanged();
			} else {
				notifyItemRangeRemoved(0, oldCursor.getCount());
			}
			return oldCursor;
		}
	}

	@Override
	public void onLoaderReset(Loader<Cursor> arg0) {
		adapter.changeCursor(null);
	}

	private void makeQuery() {
		if (queryToken == getResources().getInteger(R.integer.query_token_designers)) {
			query = new DesignerQuery();
		} else if (queryToken == getResources().getInteger(R.integer.query_token_artists)) {
			query = new ArtistQuery();
		} else if (queryToken == getResources().getInteger(R.integer.query_token_publishers)) {
			query = new PublisherQuery();
		} else if (queryToken == getResources().getInteger(R.integer.query_token_categories)) {
			query = new CategoryQuery();
		} else if (queryToken == getResources().getInteger(R.integer.query_token_mechanics)) {
			query = new MechanicQuery();
		} else if (queryToken == getResources().getInteger(R.integer.query_token_expansions)) {
			query = new ExpansionQuery();
		} else if (queryToken == getResources().getInteger(R.integer.query_token_base_games)) {
			query = new BaseGameQuery();
		}
	}

	abstract class Query {
		public abstract String[] getProjection();

		public abstract String getIdColumnName();

		public abstract String getTitleColumnName();

		public abstract Uri getUri();

		public String getSelection() {
			return null;
		}

		public String[] getSelectionArgs() {
			return null;
		}

		public Uri getUri(Cursor cursor) {
			return null;
		}

		public boolean isClickable() {
			return true;
		}
	}

	private class DesignerQuery extends Query {
		@Override
		public String[] getProjection() {
			return new String[] { Designers.DESIGNER_ID, Designers.DESIGNER_NAME, Designers._ID };
		}

		@Override
		public String getIdColumnName() {
			return Designers._ID;
		}

		@Override
		public String getTitleColumnName() {
			return Designers.DESIGNER_NAME;
		}

		@Override
		public Uri getUri() {
			return Games.buildDesignersUri(gameId);
		}

		@Override
		public Uri getUri(Cursor cursor) {
			return Designers.buildDesignerUri(cursor.getInt(0));
		}
	}

	private class ArtistQuery extends Query {
		@Override
		public String[] getProjection() {
			return new String[] { Artists.ARTIST_ID, Artists.ARTIST_NAME, Artists._ID };
		}

		@Override
		public String getIdColumnName() {
			return Artists._ID;
		}

		@Override
		public String getTitleColumnName() {
			return Artists.ARTIST_NAME;
		}

		@Override
		public Uri getUri() {
			return Games.buildArtistsUri(gameId);
		}

		@Override
		public Uri getUri(Cursor cursor) {
			return Artists.buildArtistUri(cursor.getInt(0));
		}
	}

	private class PublisherQuery extends Query {
		@Override
		public String[] getProjection() {
			return new String[] { Publishers.PUBLISHER_ID, Publishers.PUBLISHER_NAME, Publishers._ID };
		}

		@Override
		public String getIdColumnName() {
			return Publishers._ID;
		}

		@Override
		public String getTitleColumnName() {
			return Publishers.PUBLISHER_NAME;
		}

		@Override
		public Uri getUri() {
			return Games.buildPublishersUri(gameId);
		}

		@Override
		public Uri getUri(Cursor cursor) {
			return Publishers.buildPublisherUri(cursor.getInt(0));
		}
	}

	private class CategoryQuery extends Query {
		@Override
		public String[] getProjection() {
			return new String[] { Categories.CATEGORY_ID, Categories.CATEGORY_NAME, Categories._ID };
		}

		@Override
		public String getIdColumnName() {
			return Categories._ID;
		}

		@Override
		public String getTitleColumnName() {
			return Categories.CATEGORY_NAME;
		}

		@Override
		public Uri getUri() {
			return Games.buildCategoriesUri(gameId);
		}

		@Override
		public boolean isClickable() {
			return false;
		}
	}

	private class MechanicQuery extends Query {
		@Override
		public String[] getProjection() {
			return new String[] { Mechanics.MECHANIC_ID, Mechanics.MECHANIC_NAME, Mechanics._ID };
		}

		@Override
		public String getIdColumnName() {
			return Mechanics._ID;
		}

		@Override
		public String getTitleColumnName() {
			return Mechanics.MECHANIC_NAME;
		}

		@Override
		public Uri getUri() {
			return Games.buildMechanicsUri(gameId);
		}

		@Override
		public boolean isClickable() {
			return false;
		}
	}

	private class ExpansionBaseQuery extends Query {
		@Override
		public String[] getProjection() {
			return new String[] { GamesExpansions.EXPANSION_ID, GamesExpansions.EXPANSION_NAME, GamesExpansions._ID };
		}

		@Override
		public String getIdColumnName() {
			return GamesExpansions._ID;
		}

		@Override
		public String getTitleColumnName() {
			return GamesExpansions.EXPANSION_NAME;
		}

		@Override
		public Uri getUri() {
			return Games.buildExpansionsUri(gameId);
		}

		public String getSelection() {
			return GamesExpansions.INBOUND + "=?";
		}

		@Override
		public Uri getUri(Cursor cursor) {
			return Games.buildGameUri(cursor.getInt(0));
		}
	}

	private class ExpansionQuery extends ExpansionBaseQuery {
		public String[] getSelectionArgs() {
			return new String[] { "0" };
		}
	}

	private class BaseGameQuery extends ExpansionBaseQuery {
		public String[] getSelectionArgs() {
			return new String[] { "1" };
		}
	}
}
