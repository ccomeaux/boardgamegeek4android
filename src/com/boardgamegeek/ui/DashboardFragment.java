package com.boardgamegeek.ui;

import android.accounts.Account;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;

import com.actionbarsherlock.app.SherlockFragment;
import com.boardgamegeek.R;
import com.boardgamegeek.auth.Authenticator;
import com.boardgamegeek.pref.Preferences;
import com.boardgamegeek.provider.BggContract.Buddies;
import com.boardgamegeek.provider.BggContract.Collection;
import com.boardgamegeek.provider.BggContract.Plays;
import com.boardgamegeek.util.PreferencesUtils;

public class DashboardFragment extends SherlockFragment {

	private View mCollection;
	private View mBuddies;
	private View mPlays;
	private View mSignIn;
	private Account mAccount;

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		mAccount = Authenticator.getAccount(activity);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View root = inflater.inflate(R.layout.fragment_dashboard, container);

		mCollection = root.findViewById(R.id.home_btn_collection);
		mCollection.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				startActivity(new Intent(Intent.ACTION_VIEW, Collection.CONTENT_URI));
			}
		});

		root.findViewById(R.id.home_btn_hotness).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				startActivity(new Intent(getActivity(), HotnessActivity.class));
			}
		});

		root.findViewById(R.id.home_btn_forums).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				startActivity(new Intent(getActivity(), ForumsActivity.class));
			}
		});

		mBuddies = root.findViewById(R.id.home_btn_buddies);
		mBuddies.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				startActivity(new Intent(Intent.ACTION_VIEW, Buddies.CONTENT_URI));
			}
		});

		mPlays = root.findViewById(R.id.home_btn_plays);
		mPlays.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				startActivity(new Intent(Intent.ACTION_VIEW, Plays.CONTENT_URI));
			}
		});

		root.findViewById(R.id.home_btn_settings).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				startActivity(new Intent(getActivity(), Preferences.class));
			}
		});

		mSignIn = root.findViewById(R.id.home_btn_signin);
		mSignIn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				startActivityForResult(new Intent(getActivity(), LoginActivity.class), 0);
			}
		});

		return root;
	}

	@Override
	public void onResume() {
		super.onResume();
		mSignIn.setVisibility(mAccount == null ? View.VISIBLE : View.GONE);
		mCollection.setVisibility((mAccount != null && getActivity() != null
			&& PreferencesUtils.getSyncStatuses(getActivity()) != null && PreferencesUtils
			.getSyncStatuses(getActivity()).length > 0) ? View.VISIBLE : View.GONE);
		mBuddies.setVisibility(mAccount != null && getActivity() != null
			&& PreferencesUtils.getSyncBuddies(getActivity()) ? View.VISIBLE : View.GONE);
		mPlays
			.setVisibility(mAccount != null && getActivity() != null && PreferencesUtils.getSyncPlays(getActivity()) ? View.VISIBLE
				: View.GONE);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		mAccount = Authenticator.getAccount(getActivity());
	}
}
