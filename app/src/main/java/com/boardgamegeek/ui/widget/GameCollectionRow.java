package com.boardgamegeek.ui.widget;

import android.content.Context;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.model.Constants;
import com.boardgamegeek.ui.GameCollectionItemActivity;
import com.boardgamegeek.util.ColorUtils;
import com.boardgamegeek.util.HttpUtils;
import com.boardgamegeek.util.PresentationUtils;
import com.boardgamegeek.util.StringUtils;
import com.squareup.picasso.Picasso;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class GameCollectionRow extends LinearLayout {
	@BindView(R.id.thumbnail) ImageView thumbnailView;
	@BindView(R.id.status) TextView statusView;
	@BindView(R.id.description) TextView descriptionView;
	@BindView(R.id.comment) TextView commentView;
	@BindView(R.id.rating) TextView ratingView;

	private long internalId;
	private int gameId;
	private String gameName;
	private String collectionName;
	private int collectionId;
	private int yearPublished;
	private int collectionYearPublished;
	private String imageUrl;

	public GameCollectionRow(Context context) {
		super(context);

		LayoutInflater.from(context).inflate(R.layout.widget_collection_row, this, true);
		ButterKnife.bind(this);
	}

	@OnClick
	public void onClick() {
		GameCollectionItemActivity.start(getContext(), internalId, gameId, gameName, collectionId, collectionName, imageUrl,
			yearPublished, collectionYearPublished);
	}

	public void bind(long internalId, int gameId, String gameName, int collectionId, int yearPublished, String imageUrl) {
		this.internalId = internalId;
		this.gameId = gameId;
		this.gameName = gameName;
		this.collectionId = collectionId;
		this.yearPublished = yearPublished;
		this.imageUrl = imageUrl;
	}

	public void setStatus(@NonNull List<String> statuses, int playCount, double rating, String comment) {
		if (statuses.size() == 0) {
			if (playCount > 0) {
				statuses.add(getContext().getString(R.string.played));
			} else {
				if (rating > 0.0) {
					statuses.add(getContext().getString(R.string.rated));
				}
				if (!TextUtils.isEmpty(comment)) {
					statuses.add(getContext().getString(R.string.commented));
				}
			}
		}
		PresentationUtils.setTextOrHide(statusView, StringUtils.formatList(statuses));
	}

	public void setDescription(String name, int yearPublished) {
		collectionName = name;
		collectionYearPublished = yearPublished;
		String description = "";
		if ((!TextUtils.isEmpty(name) && !name.equals(gameName)) ||
			(yearPublished != Constants.YEAR_UNKNOWN && yearPublished != this.yearPublished)) {
			if (yearPublished == Constants.YEAR_UNKNOWN) {
				description = name;
			} else {
				description = String.format("%s (%s)", name, PresentationUtils.describeYear(getContext(), yearPublished));
			}

		}
		PresentationUtils.setTextOrHide(descriptionView, description);
	}

	public void setComment(String comment) {
		PresentationUtils.setTextOrHide(commentView, comment);
	}

	public void setRating(double rating) {
		if (rating == 0.0) {
			ratingView.setVisibility(View.GONE);
		} else {
			ratingView.setText(PresentationUtils.describePersonalRating(getContext(), rating));
			ColorUtils.setTextViewBackground(ratingView, ColorUtils.getRatingColor(rating));
			ratingView.setVisibility(View.VISIBLE);
		}
	}

	public void setThumbnail(String thumbnailUrl) {
		if (TextUtils.isEmpty(thumbnailUrl)) return;
		Picasso.with(getContext())
			.load(HttpUtils.ensureScheme(thumbnailUrl))
			.placeholder(R.drawable.thumbnail_image_empty)
			.error(R.drawable.thumbnail_image_empty)
			.resizeDimen(R.dimen.thumbnail_list_size, R.dimen.thumbnail_list_size)
			.centerCrop()
			.into(thumbnailView);
	}
}
