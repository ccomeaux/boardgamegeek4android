package com.boardgamegeek.ui.widget;

import android.content.Context;
import android.support.v7.graphics.Palette;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.boardgamegeek.R;

import java.text.NumberFormat;

import butterknife.Bind;
import butterknife.ButterKnife;

public class StatBar extends FrameLayout {
	private static final NumberFormat FORMAT = NumberFormat.getInstance();
	@SuppressWarnings("unused") @Bind(R.id.value) View valueView;
	@SuppressWarnings("unused") @Bind(R.id.no_value) View noValueView;
	@SuppressWarnings("unused") @Bind(android.R.id.text1) TextView textView;

	public StatBar(Context context) {
		this(context, null);
	}

	public StatBar(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public StatBar(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(context);
	}

	private void init(Context context) {
		setLayoutParams(new LinearLayout.LayoutParams(
			LinearLayout.LayoutParams.MATCH_PARENT,
			LinearLayout.LayoutParams.MATCH_PARENT));
		setMinimumHeight(getResources().getDimensionPixelSize(R.dimen.stat_bar_height));

		LayoutInflater li = LayoutInflater.from(context);
		li.inflate(R.layout.widget_stat_bar, this, true);

		ButterKnife.bind(this);
	}

	public void setBar(int id, double progress, double max) {
		setBar(id, progress, max, 0.0);
	}

	public void setBar(int id, double progress, double max, double min) {
		textView.setText(String.format(getContext().getResources().getString(id), FORMAT.format(progress)));
		valueView.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT,
			(int) ((progress - min) * 1000)));
		noValueView.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT,
			(int) ((max - progress) * 1000)));
	}

	public void setColor(int color) {
		valueView.setBackgroundColor(color);
	}

	public static final ButterKnife.Setter<StatBar, Palette.Swatch> colorSetter =
		new ButterKnife.Setter<StatBar, Palette.Swatch>() {
			@Override
			public void set(StatBar view, Palette.Swatch value, int index) {
				if (view != null && value != null) {
					view.setColor(value.getRgb());
				}
			}
		};
}
