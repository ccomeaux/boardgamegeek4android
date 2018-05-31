package com.boardgamegeek.ui.widget;

import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.provider.BggContract.Artists;
import com.boardgamegeek.provider.BggContract.Designers;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.provider.BggContract.Publishers;
import com.boardgamegeek.ui.GameDetailActivity;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import kotlin.Pair;

public class GameDetailRow extends LinearLayout {
	@BindView(android.R.id.icon) ImageView iconView;
	@BindView(R.id.data) TextView dataView;
	private int queryToken;
	private String oneMore;
	private String someMore;
	private int gameId;
	private String gameName;
	private String label;
	private Drawable icon;

	public GameDetailRow(Context context) {
		super(context);
		init(context, null);
	}

	public GameDetailRow(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context, attrs);
	}

	private void init(Context context, AttributeSet attrs) {
		oneMore = context.getString(R.string.one_more);
		someMore = context.getString(R.string.some_more);

		setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
		int backgroundResId = 0;
		TypedArray a = context.obtainStyledAttributes(new int[] { android.R.attr.selectableItemBackground });
		try {
			backgroundResId = a.getResourceId(0, backgroundResId);
		} finally {
			a.recycle();
		}
		setBackgroundResource(backgroundResId);
		setDescendantFocusability(FOCUS_BLOCK_DESCENDANTS);
		setGravity(Gravity.CENTER_VERTICAL);
		setMinimumHeight(getResources().getDimensionPixelSize(R.dimen.game_detail_row_height));
		setOrientation(HORIZONTAL);

		LayoutInflater.from(context).inflate(R.layout.widget_game_detail_row, this, true);
		ButterKnife.bind(this);

		if (attrs != null) {
			a = context.obtainStyledAttributes(attrs, R.styleable.GameDetailRow);
			try {
				label = a.getString(R.styleable.GameDetailRow_label);
				icon = a.getDrawable(R.styleable.GameDetailRow_icon_res);
				queryToken = a.getInt(R.styleable.GameDetailRow_query_token, BggContract.INVALID_ID);
			} finally {
				a.recycle();
			}
		}

		if (icon == null) {
			iconView.setVisibility(View.GONE);
		} else {
			iconView.setVisibility(View.VISIBLE);
			iconView.setImageDrawable(icon);
		}

		setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Uri uri = (Uri) getTag();
				if (uri != null) {
					getContext().startActivity(new Intent(Intent.ACTION_VIEW, uri));
				} else {
					GameDetailActivity.start(getContext(), label, gameId, gameName, queryToken);
				}
			}
		});
	}

	public void clear() {
		dataView.setText("");
	}

	public void colorIcon(int rgb) {
		iconView.setColorFilter(rgb);
	}

	public static final ButterKnife.Setter<GameDetailRow, Integer> rgbIconSetter =
		new ButterKnife.Setter<GameDetailRow, Integer>() {
			@Override
			public void set(@NonNull GameDetailRow view, Integer value, int index) {
				if (value != null) view.colorIcon(value);
			}
		};

	public void bindData(int gameId, String gameName, List<Pair<Integer, String>> list) {
		this.gameId = gameId;
		this.gameName = gameName;
		CharSequence summary = null;
		final CharSequence text = joinNames(list);
		if (!TextUtils.isEmpty(text)) {
			TextPaint paint = new TextPaint();
			paint.setTextSize(dataView.getTextSize());
			summary = TextUtils.commaEllipsize(text, paint, dataView.getWidth() * 2, oneMore, someMore);
			if (TextUtils.isEmpty(summary)) {
				summary = String.format(someMore, list.size());
			}
		}
		dataView.setText(summary);

		if (list.size() == 1) {
			int id = list.get(0).getFirst();
			Uri uri = null;
			if (queryToken == getResources().getInteger(R.integer.query_token_designers)) {
				uri = Designers.buildDesignerUri(id);
			} else if (queryToken == getResources().getInteger(R.integer.query_token_artists)) {
				uri = Artists.buildArtistUri(id);
			} else if (queryToken == getResources().getInteger(R.integer.query_token_publishers)) {
				uri = Publishers.buildPublisherUri(id);
			} else if (queryToken == getResources().getInteger(R.integer.query_token_expansions) ||
				queryToken == getResources().getInteger(R.integer.query_token_base_games)) {
				uri = Games.buildGameUri(id);
			}
			if (uri != null) {
				setTag(uri);
			}
		}
	}

	private String joinNames(List<Pair<Integer, String>> list) {
		StringBuilder sb = new StringBuilder();
		if (list != null) {
			final int count = list.size();
			if (count == 1) {
				return list.get(0).getSecond();
			} else if (count == 2) {
				sb
					.append(list.get(0).getSecond())
					.append(" & ")
					.append(list.get(1).getSecond());
			} else {
				for (int i = 0; i < list.size(); i++) {
					if (i != 0) sb.append(", ");
					sb.append(list.get(i).getSecond());
				}
			}
		}
		return sb.toString();
	}
}