package com.boardgamegeek.ui;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
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
import com.boardgamegeek.provider.BggContract.Artists;
import com.boardgamegeek.provider.BggContract.Categories;
import com.boardgamegeek.provider.BggContract.Designers;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.provider.BggContract.GamesExpansions;
import com.boardgamegeek.provider.BggContract.Mechanics;
import com.boardgamegeek.provider.BggContract.Publishers;
import com.boardgamegeek.util.AnimationUtils;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import kotlin.Pair;
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
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.fragment_game_details, container, false);
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		unbinder = ButterKnife.bind(this, view);
		setUpRecyclerView();
	}

	private void readBundle(@Nullable Bundle bundle) {
		if (bundle == null) return;
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

	@NonNull
	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle data) {
		return new CursorLoader(getActivity(), query.getUri(), query.getProjection(), query.getSelection(), query.getSelectionArgs(), null);
	}

	@Override
	public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor cursor) {
		if (getActivity() == null) {
			return;
		}

		int token = loader.getId();
		if (token == queryToken) {
			List<Pair<Integer, String>> items = new ArrayList<>(cursor.getCount());
			if (cursor.moveToFirst()) {
				do {
					int id = cursor.getInt(cursor.getColumnIndex(query.getIdColumnName()));
					String name = cursor.getString(cursor.getColumnIndex(query.getTitleColumnName()));
					items.add(new Pair<>(id, name));
				} while (cursor.moveToNext());
			}
			if (adapter == null) {
				adapter = new GameDetailAdapter(items, query);
				recyclerView.setAdapter(adapter);
			} else {
				adapter.setItems(items);
			}
		} else {
			Timber.d("Query complete, Not Actionable: %s", token);
			cursor.close();
		}

		AnimationUtils.fadeOut(progressView);
		AnimationUtils.fadeIn(getActivity(), recyclerView, isResumed());
	}

	public class GameDetailAdapter extends RecyclerView.Adapter<GameDetailAdapter.DetailViewHolder> {
		private final Query query;
		private List<Pair<Integer, String>> items;

		public GameDetailAdapter(List<Pair<Integer, String>> items, Query query) {
			this.query = query;
			this.items = items;
			setHasStableIds(true);
			notifyDataSetChanged();
		}

		public void setItems(List<Pair<Integer, String>> items) {
			this.items = items;
			notifyDataSetChanged();
		}

		@NonNull
		@Override
		public DetailViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
			return new DetailViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.row_text, parent, false));
		}

		@Override
		public void onBindViewHolder(@NonNull DetailViewHolder holder, int position) {
			holder.bind(items.get(position));
		}

		@Override
		public int getItemCount() {
			return items.size();
		}

		@Override
		public long getItemId(int position) {
			return items.get(position).getFirst();
		}

		public class DetailViewHolder extends RecyclerView.ViewHolder {
			@BindView(android.R.id.title) TextView titleView;

			public DetailViewHolder(View itemView) {
				super(itemView);
				ButterKnife.bind(this, itemView);
			}

			public void bind(final Pair<Integer, String> pair) {
				final String title = pair.getSecond();
				titleView.setText(title);
				if (query.isClickable()) {
					itemView.setOnClickListener(new OnClickListener() {
						@Override
						public void onClick(View v) {
							if (queryToken == 6 || queryToken == 7) {
								GameActivity.start(getContext(), pair.getFirst(), title);
							} else {
								ProducerActivity.start(getContext(), queryToken, pair.getFirst(), pair.getSecond());
							}
						}
					});
				}
			}
		}
	}

	@Override
	public void onLoaderReset(@NonNull Loader<Cursor> arg0) {
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
			return Designers.DESIGNER_ID;
		}

		@Override
		public String getTitleColumnName() {
			return Designers.DESIGNER_NAME;
		}

		@Override
		public Uri getUri() {
			return Games.buildDesignersUri(gameId);
		}
	}

	private class ArtistQuery extends Query {
		@Override
		public String[] getProjection() {
			return new String[] { Artists.ARTIST_ID, Artists.ARTIST_NAME, Artists._ID };
		}

		@Override
		public String getIdColumnName() {
			return Artists.ARTIST_ID;
		}

		@Override
		public String getTitleColumnName() {
			return Artists.ARTIST_NAME;
		}

		@Override
		public Uri getUri() {
			return Games.buildArtistsUri(gameId);
		}
	}

	private class PublisherQuery extends Query {
		@Override
		public String[] getProjection() {
			return new String[] { Publishers.PUBLISHER_ID, Publishers.PUBLISHER_NAME, Publishers._ID };
		}

		@Override
		public String getIdColumnName() {
			return Publishers.PUBLISHER_ID;
		}

		@Override
		public String getTitleColumnName() {
			return Publishers.PUBLISHER_NAME;
		}

		@Override
		public Uri getUri() {
			return Games.buildPublishersUri(gameId);
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
