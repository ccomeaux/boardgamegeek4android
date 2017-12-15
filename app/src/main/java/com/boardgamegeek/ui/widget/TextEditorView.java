package com.boardgamegeek.ui.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.v7.graphics.Palette;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.util.PresentationUtils;

import butterknife.BindDimen;
import butterknife.BindView;
import butterknife.ButterKnife;

public class TextEditorView extends ForegroundLinearLayout {
	@BindView(R.id.text_editor_header) TextView headerView;
	@BindView(R.id.text_editor_content) TextView contentView;
	@BindView(R.id.text_editor_timestamp) TimestampView timestampView;
	@BindView(R.id.text_editor_image) ImageView imageView;
	@BindDimen(R.dimen.edit_row_height) int minHeight;
	boolean isInEditMode;

	public TextEditorView(Context context) {
		super(context);
		init(null);
	}

	public TextEditorView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(attrs);
	}

	private void init(AttributeSet attrs) {
		LayoutInflater.from(getContext()).inflate(R.layout.widget_text_editor, this, true);
		ButterKnife.bind(this);

		setVisibility(View.GONE);

		setGravity(Gravity.CENTER_VERTICAL);
		setMinimumHeight(minHeight);
		setOrientation(HORIZONTAL);
		PresentationUtils.setSelectableBackground(this);

		if (attrs != null) {
			TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.TextEditorView);
			try {
				String title = a.getString(R.styleable.TextEditorView_headerText);
				headerView.setText(title);
			} finally {
				a.recycle();
			}
		}
	}

	public void setContent(CharSequence text, long timestamp) {
		PresentationUtils.setTextOrHide(contentView, text);
		timestampView.setTimestamp(timestamp);
		setEditMode();
	}

	public String getContentText() {
		return contentView.getText().toString();
	}

	public String getHeaderText() {
		return headerView.getText().toString();
	}

	public void setHeaderColor(@ColorInt int color) {
		headerView.setTextColor(color);
	}

	public void enableEditMode(boolean enable) {
		isInEditMode = enable;
		setEditMode();
	}

	public static final ButterKnife.Setter<TextEditorView, Palette.Swatch> headerColorSetter =
		new ButterKnife.Setter<TextEditorView, Palette.Swatch>() {
			@Override
			public void set(@NonNull TextEditorView view, Palette.Swatch value, int index) {
				if (value != null) {
					view.setHeaderColor(value.getRgb());
				}
			}
		};

	private void setEditMode() {
		if (isInEditMode) {
			imageView.setVisibility(VISIBLE);
			setVisibility(View.VISIBLE);
			setClickable(true);
		} else {
			imageView.setVisibility(GONE);
			setVisibility(TextUtils.isEmpty(contentView.getText()) && timestampView.getTimestamp() == 0 ? View.GONE : View.VISIBLE);
			setClickable(false);
		}
	}
}
