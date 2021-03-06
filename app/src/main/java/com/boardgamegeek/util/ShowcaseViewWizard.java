package com.boardgamegeek.util;

import android.app.Activity;
import android.os.Handler;
import android.view.MotionEvent;

import com.github.amlcurran.showcaseview.OnShowcaseEventListener;
import com.github.amlcurran.showcaseview.ShowcaseView;
import com.github.amlcurran.showcaseview.ShowcaseView.Builder;
import com.github.amlcurran.showcaseview.targets.Target;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.StringRes;
import androidx.core.util.Pair;

public class ShowcaseViewWizard {
	private final WeakReference<Activity> activityWeakReference;
	private final String helpKey;
	private final int helpVersion;
	private ShowcaseView showcaseView;
	private int helpIndex;
	final List<Pair<Integer, Target>> helpTargets = new ArrayList<>();

	public ShowcaseViewWizard(Activity activity, String helpKey, int helpVersion) {
		this.activityWeakReference = new WeakReference<>(activity);
		this.helpKey = helpKey;
		this.helpVersion = helpVersion;
		helpTargets.clear();
	}

	public void addTarget(@StringRes int contextResId, Target target) {
		helpTargets.add(new Pair<>(contextResId, target));
	}

	public void showHelp() {
		final Activity activity = activityWeakReference.get();
		if (activity == null) return;
		helpIndex = 0;
		Builder builder = HelpUtils.getShowcaseBuilder(activity);
		if (builder == null) return;
		builder.setOnClickListener(v -> showNextHelp());
		showcaseView = builder.build();
		showcaseView.setOnShowcaseEventListener(new OnShowcaseEventListener() {
			@Override
			public void onShowcaseViewHide(ShowcaseView showcaseView) {
				HelpUtils.updateHelp(activity, helpKey, helpVersion);
			}

			@Override
			public void onShowcaseViewDidHide(ShowcaseView showcaseView) {
			}

			@Override
			public void onShowcaseViewShow(ShowcaseView showcaseView) {
			}

			@Override
			public void onShowcaseViewTouchBlocked(MotionEvent motionEvent) {
			}
		});
		showcaseView.setButtonPosition(HelpUtils.getLowerLeftLayoutParams(activity));
		showNextHelp();
	}

	private void showNextHelp() {
		Activity activity = activityWeakReference.get();
		if (activity == null) return;
		if (helpIndex < helpTargets.size()) {
			Pair<Integer, Target> helpTarget = helpTargets.get(helpIndex);
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i <= helpIndex; i++) {
				int resId = helpTargets.get(i).first;
				if (resId > 0) {
					sb.append("\n").append(activity.getString(resId));
				}
			}
			showcaseView.setContentText(sb.toString());
			showcaseView.setShowcase(helpTarget.second, true);
		} else {
			showcaseView.hide();
			HelpUtils.updateHelp(activity, helpKey, helpVersion);
		}
		helpIndex++;
	}

	public void maybeShowHelp() {
		Activity activity = activityWeakReference.get();
		if (activity == null) return;
		if (HelpUtils.shouldShowHelp(activity, helpKey, helpVersion)) {
			new Handler().postDelayed(this::showHelp, 100);
		}
	}
}
