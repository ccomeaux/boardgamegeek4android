package com.boardgamegeek.ui.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.model.TopGame;
import com.boardgamegeek.ui.GameActivity;
import com.boardgamegeek.util.ImageUtils;
import com.boardgamegeek.util.PresentationUtils;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class TopGamesAdapter extends RecyclerView.Adapter<TopGamesAdapter.ViewHolder> {

	private final List<TopGame> topGames;

	public TopGamesAdapter(List<TopGame> topGames) {
		this.topGames = topGames;
		setHasStableIds(true);
	}

	@Override
	public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
		View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_top_game, parent, false);
		return new ViewHolder(view);
	}

	@Override
	public void onBindViewHolder(ViewHolder holder, int position) {
		holder.bind(topGames.get(position));
	}

	@Override
	public int getItemCount() {
		return topGames.size();
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	public class ViewHolder extends RecyclerView.ViewHolder {
		@BindView(R.id.name) TextView name;
		@BindView(R.id.year) TextView year;
		@BindView(R.id.rank) TextView rank;
		@BindView(R.id.thumbnail) ImageView thumbnail;

		private TopGame game;

		public ViewHolder(View itemView) {
			super(itemView);
			ButterKnife.bind(this, itemView);
		}

		public void bind(TopGame game) {
			this.game = game;
			name.setText(game.name);
			year.setText(PresentationUtils.describeYear(name.getContext(), game.yearPublished));
			rank.setText(String.valueOf(game.rank));
			ImageUtils.loadThumbnail(game.thumbnailUrl, thumbnail);

			itemView.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					GameActivity.start(itemView.getContext(), getGame().id, getGame().name);
				}
			});
		}

		public TopGame getGame() {
			return game;
		}
	}
}
