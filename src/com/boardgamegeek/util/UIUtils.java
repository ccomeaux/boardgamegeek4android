package com.boardgamegeek.util;

import java.util.Random;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.boardgamegeek.BggApplication;
import com.boardgamegeek.R;
import com.boardgamegeek.ui.HomeActivity;
import com.boardgamegeek.ui.widget.BezelImageView;

public class UIUtils {

	private Activity mActivity;
	private static Random mRandom;

	public UIUtils(Activity activity) {
		mActivity = activity;
	}

	/**
	 * Sets the current activity to the home screen.
	 */
	public static void goHome(Context context) {
		final Intent intent = new Intent(context, HomeActivity.class);
		context.startActivity(intent);
	}

	/**
	 * Sets the current activity to the home screen, clearing the activity
	 * stack.
	 */
	public static void resetToHome(Context context) {
		final Intent intent = new Intent(context, HomeActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		context.startActivity(intent);
	}

	/**
	 * Sets the custom title since the default is hidden.
	 */
	public static void setTitle(Activity activity) {
		setTitle(activity, activity.getTitle());
	}

	/**
	 * Sets the custom title since the default is hidden.
	 */
	public static void setTitle(Activity activity, CharSequence title) {
		((TextView) activity.findViewById(R.id.title_text)).setText(title);
	}

	public static void setGameName(Activity activity, CharSequence gameName) {
		((TextView) activity.findViewById(R.id.game_name)).setText(gameName);
	}

	public void setGameName(CharSequence gameName) {
		setGameName(mActivity, gameName);
	}

	public void setThumbnail(String thumbnailUrl) {
		if (BggApplication.getInstance().getImageLoad() && !TextUtils.isEmpty(thumbnailUrl)) {
			new ThumbnailTask(mActivity).execute(thumbnailUrl);
		}
	}

	private class ThumbnailTask extends AsyncTask<String, Void, Drawable> {
		private Activity mActivity;
		private BezelImageView mThumbnail;
		private View mProgress;

		public ThumbnailTask(Activity activity) {
			if (activity != null) {
				mActivity = activity;
				mThumbnail = (BezelImageView) mActivity.findViewById(R.id.game_thumbnail);
				mProgress = mActivity.findViewById(R.id.thumbnail_progress);
			}
		}

		@Override
		protected void onPreExecute() {
			if (mProgress != null) {
				mProgress.setVisibility(View.VISIBLE);
			}
		}

		@Override
		protected Drawable doInBackground(String... params) {
			if (mActivity == null || mThumbnail == null) {
				return null;
			}
			return ImageCache.getImage(mActivity.getBaseContext(), params[0]);
		}

		@Override
		protected void onPostExecute(Drawable result) {
			if (mProgress != null) {
				mProgress.setVisibility(View.GONE);
			}
			if (mThumbnail != null) {
				mThumbnail.setVisibility(View.VISIBLE);
				if (result != null) {
					mThumbnail.setImageDrawable(result);
				} else {
					mThumbnail.setImageResource(R.drawable.noimage);
				}
			}
		}
	}

	public static void allowTypeToSearch(Activity activity) {
		activity.setDefaultKeyMode(Activity.DEFAULT_KEYS_SEARCH_LOCAL);
	}

	public static String[] projectionFromEnums(Enum<?> e[]) {
		String projection[] = new String[e.length];

		int index = 0;
		for (Enum<?> currentEnum : e) {
			projection[index] = currentEnum.toString();
			index++;
		}

		return projection;
	}

	public static void showListMessage(Activity activity, int messageResourceId) {
		showListMessage(activity, messageResourceId, true);
	}

	public static void showListMessage(Activity activity, int messageResourceId, boolean hideProgressBar) {
		TextView tv = (TextView) activity.findViewById(R.id.list_message);
		tv.setText(messageResourceId);

		hideProgressBar(activity, hideProgressBar);
	}

	public static void showListMessage(Activity activity, String message) {
		showListMessage(activity, message, true);
	}

	public static void showListMessage(Activity activity, String message, boolean hideProgressBar) {
		TextView tv = (TextView) activity.findViewById(R.id.list_message);
		tv.setText(message);

		hideProgressBar(activity, hideProgressBar);
	}

	private static void hideProgressBar(Activity activity, boolean hide) {
		ProgressBar pb = (ProgressBar) activity.findViewById(R.id.list_progress);
		if (hide) {
			pb.setVisibility(View.GONE);
		} else {
			pb.setVisibility(View.VISIBLE);
		}
	}

	public static Random getRandom() {
		if (mRandom == null) {
			mRandom = new Random();
		}
		return mRandom;
	}

	public static AlertDialog createCancelDialog(final Activity activity) {
		AlertDialog.Builder builder = new AlertDialog.Builder(activity);
		builder.setTitle(R.string.are_you_sure_title)
			.setMessage(R.string.are_you_sure_message)
			.setCancelable(false)
			.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					activity.finish();
				}
			}).setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					activity.setResult(Activity.RESULT_CANCELED);
					dialog.cancel();
				}
			});
		return builder.create();
	}
}
