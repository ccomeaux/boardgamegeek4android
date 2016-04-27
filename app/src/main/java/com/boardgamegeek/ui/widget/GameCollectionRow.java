package com.boardgamegeek.ui.widget;

import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.model.Constants;
import com.boardgamegeek.ui.GameCollectionActivity;
import com.boardgamegeek.util.ActivityUtils;
import com.boardgamegeek.util.ColorUtils;
import com.boardgamegeek.util.HttpUtils;
import com.boardgamegeek.util.PresentationUtils;
import com.boardgamegeek.util.StringUtils;
import com.squareup.picasso.Picasso;

import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;

public class GameCollectionRow extends LinearLayout {
	@SuppressWarnings("unused") @InjectView(R.id.thumbnail) ImageView thumbnailView;
	@SuppressWarnings("unused") @InjectView(R.id.status) TextView statusView;
	@SuppressWarnings("unused") @InjectView(R.id.description) TextView descriptionView;
	@SuppressWarnings("unused") @InjectView(R.id.comment) TextView commentView;
	@SuppressWarnings("unused") @InjectView(R.id.rating) TextView ratingView;

	private int gameId;
	private String gameName;
	private String collectionName;
	private int collectionId;
	private int yearPublished;
	private String imageUrl;

	public GameCollectionRow(Context context) {
		super(context);

		setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
		setBackgroundResource(obtainBackgroundResId(context));
		setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
		setGravity(Gravity.CENTER_VERTICAL);
		setMinimumHeight(getResources().getDimensionPixelSize(R.dimen.edit_row_height));
		setOrientation(HORIZONTAL);
		int padding = getResources().getDimensionPixelSize(R.dimen.padding_half);
		setPadding(0, padding, 0, padding);

		LayoutInflater.from(context).inflate(R.layout.widget_collection_row, this, true);
		ButterKnife.inject(this);

		setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(getContext(), GameCollectionActivity.class);
				intent.putExtra(ActivityUtils.KEY_GAME_ID, gameId);
				intent.putExtra(ActivityUtils.KEY_GAME_NAME, gameName);
				intent.putExtra(ActivityUtils.KEY_COLLECTION_ID, collectionId);
				intent.putExtra(ActivityUtils.KEY_COLLECTION_NAME, collectionName);
				intent.putExtra(ActivityUtils.KEY_IMAGE_URL, imageUrl);
				getContext().startActivity(intent);
			}
		});
	}

	private int obtainBackgroundResId(Context context) {
		int backgroundResId = 0;
		TypedArray a = null;
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB) {
			a = context.obtainStyledAttributes(new int[] { android.R.attr.selectableItemBackground });
		}
		try {
			backgroundResId = a != null ? a.getResourceId(0, backgroundResId) : 0;
		} finally {
			if (a != null) {
				a.recycle();
			}
		}
		return backgroundResId;
	}

	public void bind(int gameId, String gameName, int collectionId, int yearPublished, String imageUrl) {
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
		String description = StringUtils.formatList(statuses);
		if (TextUtils.isEmpty(description)) {
			statusView.setVisibility(View.GONE);
		} else {
			statusView.setText(description);
			statusView.setVisibility(View.VISIBLE);
		}
	}

	public void setDescription(String name, int yearPublished) {
		collectionName = name;
		if ((!TextUtils.isEmpty(name) && !name.equals(gameName)) ||
			(yearPublished != Constants.YEAR_UNKNOWN && yearPublished != this.yearPublished)) {
			String description;
			if (yearPublished == Constants.YEAR_UNKNOWN) {
				description = name;
			} else {
				description = name + " (" + PresentationUtils.describeYear(getContext(), yearPublished) + ")";
			}
			descriptionView.setText(description);
			descriptionView.setVisibility(View.VISIBLE);
		} else {
			descriptionView.setVisibility(View.GONE);
		}
	}

	public void setComment(String comment) {
		if (TextUtils.isEmpty(comment)) {
			commentView.setVisibility(View.GONE);
		} else {
			commentView.setText(comment);
			commentView.setVisibility(View.VISIBLE);
		}
	}

	public void setRating(double rating) {
		if (rating == 0.0) {
			ratingView.setVisibility(View.GONE);
		} else {
			ratingView.setText(PresentationUtils.describePersonalRating(getContext(), rating));
			ColorUtils.setViewBackground(ratingView, ColorUtils.getRatingColor(rating));
			ratingView.setVisibility(View.VISIBLE);
		}
	}

	public void setThumbnail(String thumbnailUrl) {
		Picasso.with(getContext())
			.load(HttpUtils.ensureScheme(thumbnailUrl))
			.placeholder(R.drawable.thumbnail_image_empty)
			.error(R.drawable.thumbnail_image_empty)
			.resizeDimen(R.dimen.thumbnail_list_size_small, R.dimen.thumbnail_list_size)
			.centerCrop()
			.into(thumbnailView);
	}
}
