package com.boardgamegeek.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.entities.ArticleEntity;
import com.boardgamegeek.ui.widget.TimestampView;
import com.boardgamegeek.util.UIUtils;

import java.text.NumberFormat;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import hugo.weaving.DebugLog;

public class ArticleFragment extends Fragment {
	private static final String KEY_ARTICLE = "ARTICLE";

	private ArticleEntity article;

	private Unbinder unbinder;
	@BindView(R.id.username) TextView usernameView;
	@BindView(R.id.post_date) TimestampView postDateView;
	@BindView(R.id.edit_date) TimestampView editDateView;
	@BindView(R.id.body) WebView bodyView;

	public static ArticleFragment newInstance(ArticleEntity article) {
		Bundle args = new Bundle();
		args.putParcelable(KEY_ARTICLE, article);
		ArticleFragment fragment = new ArticleFragment();
		fragment.setArguments(args);
		return fragment;
	}

	@Override
	@DebugLog
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		readBundle(getArguments());
		ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.fragment_article, container, false);
		unbinder = ButterKnife.bind(this, rootView);

		usernameView.setText(article.getUsername());
		postDateView.setTimestamp(article.getPostTicks());
		if (article.getNumberOfEdits() > 0) {
			editDateView.setFormat(getResources().getQuantityString(R.plurals.edit_timestamp, article.getNumberOfEdits()));
			editDateView.setFormatArg(NumberFormat.getNumberInstance().format(article.getNumberOfEdits()));
			editDateView.setTimestamp(article.getEditTicks());
			editDateView.setVisibility(View.VISIBLE);
		} else {
			editDateView.setVisibility(View.GONE);
		}
		UIUtils.setWebViewText(bodyView, article.getBody());

		return rootView;
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		unbinder.unbind();
	}

	private void readBundle(@Nullable Bundle bundle) {
		if (bundle == null) return;
		article = bundle.getParcelable(KEY_ARTICLE);
	}
}
