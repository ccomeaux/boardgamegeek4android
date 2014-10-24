package com.boardgamegeek.ui;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.boardgamegeek.R;

public class TextFragment extends Fragment {
	public static final String KEY_TEXT = "KEY_TEXT";
	TextView mTextView;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_text, null);
		mTextView = (TextView) view.findViewById(R.id.text);
		return view;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		mTextView.setText(getArguments().getString(KEY_TEXT));
	}
}
