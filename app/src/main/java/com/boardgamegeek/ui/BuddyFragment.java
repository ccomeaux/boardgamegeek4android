package com.boardgamegeek.ui;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Buddies;
import com.boardgamegeek.provider.BggContract.PlayPlayers;
import com.boardgamegeek.provider.BggContract.PlayerColors;
import com.boardgamegeek.provider.BggContract.Plays;
import com.boardgamegeek.service.UpdateService;
import com.boardgamegeek.tasks.BuddyNicknameUpdateTask;
import com.boardgamegeek.ui.model.Buddy;
import com.boardgamegeek.ui.model.BuddyColor;
import com.boardgamegeek.ui.model.Player;
import com.boardgamegeek.util.ActivityUtils;
import com.boardgamegeek.util.ColorUtils;
import com.boardgamegeek.util.HttpUtils;
import com.boardgamegeek.util.PresentationUtils;
import com.boardgamegeek.util.StringUtils;
import com.boardgamegeek.util.TaskUtils;
import com.boardgamegeek.util.UIUtils;
import com.squareup.picasso.Picasso;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import hugo.weaving.DebugLog;

public class BuddyFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final String KEY_REFRESHED = "REFRESHED";
    private static final int PLAYS_TOKEN = 1;
    private static final int COLORS_TOKEN = 2;
    private static final int TIME_HINT_UPDATE_INTERVAL = 30000; // 30 sec

    private Handler mHandler = new Handler();
    private Runnable mUpdaterRunnable = null;
    private static final int TOKEN = 0;

    private String mBuddyName;
    private boolean mRefreshed;
    private ViewGroup mRootView;
    @SuppressWarnings("unused") @InjectView(R.id.full_name) TextView mFullName;
    @SuppressWarnings("unused") @InjectView(R.id.username) TextView mName;
    @SuppressWarnings("unused") @InjectView(R.id.avatar) ImageView mAvatar;
    @SuppressWarnings("unused") @InjectView(R.id.nickname) TextView mNickname;
    @SuppressWarnings("unused") @InjectView(R.id.plays_label) TextView mPlays;
    @SuppressWarnings("unused") @InjectView(R.id.color_container) View mColorContainer;
    @SuppressWarnings("unused") @InjectView(R.id.color_1) ImageView mColor1;
    @SuppressWarnings("unused") @InjectView(R.id.color_2) ImageView mColor2;
    @SuppressWarnings("unused") @InjectView(R.id.color_3) ImageView mColor3;
    @SuppressWarnings("unused") @InjectView(R.id.updated) TextView mUpdated;
    private int mDefaultTextColor;
    private int mLightTextColor;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mHandler = new Handler();
        if (savedInstanceState != null) {
            mRefreshed = savedInstanceState.getBoolean(KEY_REFRESHED);
        }

        final Intent intent = UIUtils.fragmentArgumentsToIntent(getArguments());
        mBuddyName = intent.getStringExtra(ActivityUtils.KEY_BUDDY_NAME);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_REFRESHED, mRefreshed);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mRootView = (ViewGroup) inflater.inflate(R.layout.fragment_buddy, container, false);

        ButterKnife.inject(this, mRootView);

        mDefaultTextColor = mNickname.getTextColors().getDefaultColor();
        mLightTextColor = getResources().getColor(R.color.light_text);

        getLoaderManager().restartLoader(TOKEN, null, this);
        getLoaderManager().restartLoader(PLAYS_TOKEN, null, this);
        getLoaderManager().restartLoader(COLORS_TOKEN, null, this);

        return mRootView;
    }

    @Override
    @DebugLog
    public void onResume() {
        super.onResume();
        if (mUpdaterRunnable != null) {
            mHandler.postDelayed(mUpdaterRunnable, TIME_HINT_UPDATE_INTERVAL);
        }
    }

    @Override
    @DebugLog
    public void onPause() {
        super.onPause();
        if (mUpdaterRunnable != null) {
            mHandler.removeCallbacks(mUpdaterRunnable);
        }
    }

    @Override
    @DebugLog
    public Loader<Cursor> onCreateLoader(int id, Bundle data) {
        CursorLoader loader = null;
        switch (id) {
            case TOKEN:
                loader = new CursorLoader(getActivity(), Buddies.buildBuddyUri(mBuddyName), Buddy.PROJECTION, null, null, null);
                break;
            case PLAYS_TOKEN:
                loader = new CursorLoader(getActivity(),
                        Plays.buildPlayersByUniqueUserUri(),
                        Player.PROJECTION,
                        PlayPlayers.USER_NAME + "=?",
                        new String[]{String.valueOf(mBuddyName)},
                        null);
                break;
            case COLORS_TOKEN:
                loader = new CursorLoader(getActivity(),
                        PlayerColors.buildUserUri(mBuddyName),
                        BuddyColor.PROJECTION,
                        null, null, null);
                break;
        }
        return loader;
    }

    @Override
    @DebugLog
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        if (getActivity() == null) {
            return;
        }

        switch (loader.getId()) {
            case TOKEN:
                onBuddyQueryComplete(cursor);
                break;
            case PLAYS_TOKEN:
                onPlaysQueryComplete(cursor);
                break;
            case COLORS_TOKEN:
                onColorsQueryComplete(cursor);
                break;
            default:
                cursor.close();
                break;
        }
    }

    @Override
    @DebugLog
    public void onLoaderReset(Loader<Cursor> loader) {
    }

    @DebugLog
    @SuppressWarnings("unused")
    @OnClick(R.id.nickname)
    public void onEditNicknameClick(View v) {
        showDialog(mNickname.getText().toString(), mBuddyName);
    }

    @DebugLog
    @SuppressWarnings("unused")
    @OnClick(R.id.collection_root)
    public void onCollectionClick(View v) {
        Intent intent = new Intent(getActivity(), BuddyCollectionActivity.class);
        intent.putExtra(ActivityUtils.KEY_BUDDY_NAME, mBuddyName);
        startActivity(intent);
    }

    @DebugLog
    @SuppressWarnings("unused")
    @OnClick(R.id.plays_root)
    public void onPlaysClick(View v) {
        Intent intent = new Intent(getActivity(), BuddyPlaysActivity.class);
        intent.putExtra(ActivityUtils.KEY_BUDDY_NAME, mBuddyName);
        startActivity(intent);
    }

    @DebugLog
    @SuppressWarnings("unused")
    @OnClick(R.id.colors_root)
    public void onColorsClick(View v) {
        Intent intent = new Intent(getActivity(), BuddyColorsActivity.class);
        intent.putExtra(ActivityUtils.KEY_BUDDY_NAME, mBuddyName);
        startActivity(intent);
    }

    private void onBuddyQueryComplete(Cursor cursor) {
        if (cursor == null || !cursor.moveToFirst()) {
            requestRefresh();
            return;
        }

        Buddy buddy = Buddy.fromCursor(cursor);

        Picasso.with(getActivity())
                .load(HttpUtils.ensureScheme(buddy.getAvatarUrl()))
                .placeholder(R.drawable.person_image_empty)
                .error(R.drawable.person_image_empty)
                .fit().into(mAvatar);
        mFullName.setText(buddy.getFullName());
        mName.setText(mBuddyName);
        if (TextUtils.isEmpty(buddy.getNickName())) {
            mNickname.setTextColor(mLightTextColor);
            mNickname.setText(buddy.getFirstName());
        } else {
            mNickname.setTextColor(mDefaultTextColor);
            mNickname.setText(buddy.getNickName());
        }
        mUpdated.setTag(buddy.getUpdated());

        updateTimeBasedUi();
        if (mUpdaterRunnable != null) {
            mHandler.removeCallbacks(mUpdaterRunnable);
        }
        mUpdaterRunnable = new Runnable() {
            @Override
            public void run() {
                updateTimeBasedUi();
                mHandler.postDelayed(mUpdaterRunnable, TIME_HINT_UPDATE_INTERVAL);
            }
        };
        mHandler.postDelayed(mUpdaterRunnable, TIME_HINT_UPDATE_INTERVAL);
    }

    @DebugLog
    private void updateTimeBasedUi() {
        if (mUpdated != null) {
            long updated = (long) mUpdated.getTag();
            mUpdated.setText(PresentationUtils.describePastTimeSpan(updated, getString(R.string.needs_updating), getString(R.string.updated)));
        }
    }

    private void onPlaysQueryComplete(Cursor cursor) {
        if (cursor == null || !cursor.moveToFirst()) {
            return;
        }

        Player player = Player.fromCursor(cursor);
        mPlays.setText(StringUtils.boldSecondString("", String.valueOf(player.getPlayCount()), getString(R.string.title_plays)));
    }

    private void onColorsQueryComplete(Cursor cursor) {
        if (cursor == null) {
            return;
        }

        populateColor(cursor, mColor1);
        populateColor(cursor, mColor2);
        populateColor(cursor, mColor3);
    }

    private void populateColor(Cursor cursor, ImageView view) {
        if (cursor.moveToNext()) {
            mColorContainer.setVisibility(View.VISIBLE);
            BuddyColor color = BuddyColor.fromCursor(cursor);
            ColorUtils.setColorViewValue(view, ColorUtils.parseColor(color.getColor()));
        }
    }

    private void requestRefresh() {
        if (!mRefreshed) {
            forceRefresh();
            mRefreshed = true;
        }
    }

    public void forceRefresh() {
        UpdateService.start(getActivity(), UpdateService.SYNC_TYPE_BUDDY, mBuddyName);
    }

    private void showDialog(final String nickname, final String username) {
        final LayoutInflater inflater = (LayoutInflater) getActivity()
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.dialog_edit_nickname, mRootView, false);

        final EditText editText = (EditText) view.findViewById(R.id.edit_nickname);
        final CheckBox checkBox = (CheckBox) view.findViewById(R.id.change_plays);
        if (!TextUtils.isEmpty(nickname)) {
            editText.setText(nickname);
            editText.setSelection(0, nickname.length());
        }

        AlertDialog dialog = new AlertDialog.Builder(getActivity()).setView(view)
                .setTitle(R.string.title_edit_nickname).setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String newNickname = editText.getText().toString();
                        BuddyNicknameUpdateTask task = new BuddyNicknameUpdateTask(getActivity(),
                                username, newNickname, checkBox.isChecked());
                        TaskUtils.executeAsyncTask(task);
                    }
                }).create();
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        dialog.show();
    }
}
