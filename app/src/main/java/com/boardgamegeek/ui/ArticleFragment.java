package com.boardgamegeek.ui;

import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.databinding.FragmentArticleBinding;
import com.boardgamegeek.ui.widget.TimestampView;
import com.boardgamegeek.util.UIUtils;

import java.text.NumberFormat;

public class ArticleFragment extends Fragment {
	private static final String KEY_USER = "USER";
	private static final String KEY_POST_DATE = "POST_DATE";
	private static final String KEY_EDIT_DATE = "EDIT_DATE";
	private static final String KEY_EDIT_COUNT = "EDIT_COUNT";
	private static final String KEY_BODY = "BODY";

	private String user;
	private long postDate;
	private long editDate;
	private int editCount;
	private String body;

	private FragmentArticleBinding binding;

	public static ArticleFragment newInstance(String user, long postDate, long editDate, int editCount, String body) {
		Bundle args = new Bundle();
		args.putString(KEY_USER, user);
		args.putLong(KEY_POST_DATE, postDate);
		args.putLong(KEY_EDIT_DATE, editDate);
		args.putInt(KEY_EDIT_COUNT, editCount);
		args.putString(KEY_BODY, body);

		ArticleFragment fragment = new ArticleFragment();
		fragment.setArguments(args);
		return fragment;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		readBundle(getArguments());
		binding = FragmentArticleBinding.inflate(inflater, container, false);

		binding.username.setText(user);
		binding.postDate.setTimestamp(postDate);
		if (editCount > 0) {
			binding.editDate.setFormat(getResources().getQuantityString(R.plurals.edit_timestamp, editCount));
			binding.editDate.setFormatArg(NumberFormat.getNumberInstance().format(editCount));
			binding.editDate.setTimestamp(editDate);
			binding.editDate.setVisibility(View.VISIBLE);
		} else {
			binding.editDate.setVisibility(View.GONE);
		}
		UIUtils.setWebViewText(binding.body, body);

		return binding.getRoot();
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		binding = null;
	}

	private void readBundle(@Nullable Bundle bundle) {
		if (bundle == null) return;
		user = bundle.getString(KEY_USER);
		postDate = bundle.getLong(KEY_POST_DATE, 0);
		editDate = bundle.getLong(KEY_EDIT_DATE, 0);
		editCount = bundle.getInt(KEY_EDIT_COUNT, 0);
		body = bundle.getString(KEY_BODY);
	}
}
