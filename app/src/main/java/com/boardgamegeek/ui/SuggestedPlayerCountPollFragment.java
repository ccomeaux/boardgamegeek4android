package com.boardgamegeek.ui;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.ColorRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.ContentLoadingProgressBar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.provider.BggContract.GameSuggestedPlayerCountPollPollResults;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.ui.widget.PlayerNumberRow;
import com.boardgamegeek.ui.widget.PollKeyRow;
import com.boardgamegeek.util.AnimationUtils;
import com.boardgamegeek.util.DialogUtils;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;

public class SuggestedPlayerCountPollFragment extends DialogFragment implements LoaderCallbacks<Cursor> {
	private static final String KEY_GAME_ID = "GAME_ID";
	private int gameId;
	private Uri uri;
	private Unbinder unbinder;
	@BindView(R.id.progress) ContentLoadingProgressBar progressView;
	@BindView(R.id.poll_scroll) ScrollView scrollView;
	@BindView(R.id.poll_vote_total) TextView totalVoteView;
	@BindView(R.id.poll_list) LinearLayout pollList;
	@BindView(R.id.poll_key_container) LinearLayout keyContainer;
	@BindView(R.id.no_votes_switch) Switch noVotesSwitch;

	public static void launch(Fragment host, int gameId) {
		Bundle arguments = new Bundle(1);
		arguments.putInt(KEY_GAME_ID, gameId);
		final SuggestedPlayerCountPollFragment dialog = new SuggestedPlayerCountPollFragment();
		dialog.setArguments(arguments);
		dialog.setStyle(DialogFragment.STYLE_NORMAL, R.style.Theme_bgglight_Dialog);
		DialogUtils.showAndSurvive(host, dialog);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		readBundle(getArguments());
		uri = Games.buildSuggestedPlayerCountPollResultsUri(gameId);
	}

	private void readBundle(@Nullable Bundle bundle) {
		if (bundle == null) return;
		gameId = bundle.getInt(KEY_GAME_ID, BggContract.INVALID_ID);
	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.fragment_poll_suggested_player_count, container, false);
		unbinder = ButterKnife.bind(this, rootView);
		return rootView;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		getDialog().setTitle(R.string.suggested_numplayers);

		addKeyRow(R.color.best, R.string.best);
		addKeyRow(R.color.recommended, R.string.recommended);
		addKeyRow(R.color.not_recommended, R.string.not_recommended);

		getLoaderManager().restartLoader(Query._TOKEN, null, this);
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		unbinder.unbind();
	}

	@NonNull
	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle data) {
		return new CursorLoader(getContext(), uri, Query.PROJECTION, null, null, null);
	}

	@Override
	public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor cursor) {
		if (getActivity() == null) return;

		int totalVoteCount = cursor != null && cursor.moveToFirst() ? cursor.getInt(Query.TOTAL_VOTE_COUNT) : 0;
		totalVoteView.setText(getResources().getQuantityString(R.plurals.votes_suffix, totalVoteCount, totalVoteCount));

		pollList.setVisibility((totalVoteCount == 0) ? View.GONE : View.VISIBLE);
		keyContainer.setVisibility((totalVoteCount == 0) ? View.GONE : View.VISIBLE);
		noVotesSwitch.setVisibility((totalVoteCount == 0) ? View.GONE : View.VISIBLE);
		if (totalVoteCount > 0) {
			pollList.removeAllViews();
			do {
				PlayerNumberRow row = new PlayerNumberRow(getActivity());
				row.setText(cursor.getString(Query.PLAYER_COUNT));
				row.setTotal(totalVoteCount);
				row.setBest(cursor.getInt(Query.BEST_VOTE_COUNT));
				row.setRecommended(cursor.getInt(Query.RECOMMENDED_VOTE_COUNT));
				row.setNotRecommended(cursor.getInt(Query.NOT_RECOMMENDED_VOTE_COUNT));
				row.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						for (int i = 0; i < pollList.getChildCount(); i++) {
							((PlayerNumberRow) pollList.getChildAt(i)).clearHighlight();
						}
						PlayerNumberRow row = (PlayerNumberRow) v;
						row.setHighlight();

						int[] voteCount = row.getVotes();
						for (int i = 0; i < keyContainer.getChildCount(); i++) {
							((PollKeyRow) keyContainer.getChildAt(i)).setInfo(String.valueOf(voteCount[i]));
						}
					}
				});
				pollList.addView(row);
			} while (cursor.moveToNext());
			pollList.setVisibility(View.VISIBLE);
			keyContainer.setVisibility(View.VISIBLE);
			noVotesSwitch.setVisibility(View.VISIBLE);
		} else {
			pollList.setVisibility(View.GONE);
			keyContainer.setVisibility(View.GONE);
			noVotesSwitch.setVisibility(View.GONE);
		}

		progressView.hide();
		AnimationUtils.fadeIn(scrollView);
	}

	@Override
	public void onLoaderReset(@NonNull Loader<Cursor> loader) {
	}

	@OnClick(R.id.no_votes_switch)
	public void onNoVotesClick() {
		for (int i = 0; i < pollList.getChildCount(); i++) {
			PlayerNumberRow row = (PlayerNumberRow) pollList.getChildAt(i);
			if (row != null) row.showNoVotes(noVotesSwitch.isChecked());
		}
	}

	private void addKeyRow(@ColorRes int colorResId, @StringRes int textResId) {
		PollKeyRow pkr = new PollKeyRow(getContext());
		pkr.setColor(ContextCompat.getColor(getContext(), colorResId));
		pkr.setText(getString(textResId));
		keyContainer.addView(pkr);
	}

	private interface Query {
		int _TOKEN = 0x0;
		String[] PROJECTION = {
			GameSuggestedPlayerCountPollPollResults.SUGGESTED_PLAYER_COUNT_POLL_VOTE_TOTAL,
			GameSuggestedPlayerCountPollPollResults.PLAYER_COUNT,
			GameSuggestedPlayerCountPollPollResults.BEST_VOTE_COUNT,
			GameSuggestedPlayerCountPollPollResults.RECOMMENDED_VOTE_COUNT,
			GameSuggestedPlayerCountPollPollResults.NOT_RECOMMENDED_VOTE_COUNT,
		};
		int TOTAL_VOTE_COUNT = 0;
		int PLAYER_COUNT = 1;
		int BEST_VOTE_COUNT = 2;
		int RECOMMENDED_VOTE_COUNT = 3;
		int NOT_RECOMMENDED_VOTE_COUNT = 4;
	}
}
