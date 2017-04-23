package com.boardgamegeek.ui.dialog;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AlertDialog.Builder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.Spinner;

import com.boardgamegeek.R;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.BindViews;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import hugo.weaving.DebugLog;

public class CollectionStatusDialogFragment extends DialogFragment {
	public interface CollectionStatusDialogListener {
		void onSelectStatuses(List<String> selectedStatuses, int wishlistPriority);
	}

	@StringRes private int titleResId = R.string.menu_collection_status;
	private ViewGroup root;
	private Unbinder unbinder;
	@BindViews({
		R.id.own,
		R.id.previously_owned,
		R.id.for_trade,
		R.id.want_to_play,
		R.id.want,
		R.id.want_to_buy,
		R.id.preordered,
		R.id.wishlist
	}) List<CheckBox> statusViews;
	@BindView(R.id.wishlist) CheckBox wishlistView;
	@BindView(R.id.wishlist_priority) Spinner wishlistPriorityView;

	private CollectionStatusDialogListener listener;
	@Nullable private List<String> selectedStatuses;
	private int wishlistPriority;

	@NonNull
	public static CollectionStatusDialogFragment newInstance(@Nullable ViewGroup root, CollectionStatusDialogListener listener) {
		CollectionStatusDialogFragment fragment = new CollectionStatusDialogFragment();
		fragment.root = root;
		fragment.listener = listener;
		return fragment;
	}

	public void setTitle(int titleResId) {
		this.titleResId = titleResId;
	}

	@DebugLog
	public void setSelectedStatuses(@Nullable List<String> selectedStatuses) {
		this.selectedStatuses = selectedStatuses;
	}

	@DebugLog
	public void setWishlistPriority(int wishlistPriority) {
		this.wishlistPriority = wishlistPriority;
	}

	@NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		LayoutInflater layoutInflater = LayoutInflater.from(getActivity());
		View rootView = layoutInflater.inflate(R.layout.dialog_collection_status, root, false);

		unbinder = ButterKnife.bind(this, rootView);
		initUi();

		AlertDialog.Builder builder = new Builder(getContext(), R.style.Theme_bgglight_Dialog_Alert)
			.setTitle(titleResId)
			.setView(rootView)
			.setPositiveButton(R.string.ok, new OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					if (listener != null) {
						List<String> statuses = new ArrayList<>();
						int wishlistPriority = 0;
						for (CheckBox checkBox : statusViews) {
							if (checkBox.isChecked()) {
								String status = (String) checkBox.getTag();
								statuses.add(status);
							}
						}
						if (wishlistView.isChecked()) {
							statuses.add("wishlist");
							wishlistPriority = wishlistPriorityView.getSelectedItemPosition() + 1;
						}
						listener.onSelectStatuses(statuses, wishlistPriority);
					}
				}
			})
			.setNegativeButton(R.string.cancel, null);
		return builder.create();
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		if (unbinder != null) unbinder.unbind();
	}

	@OnClick(R.id.wishlist)
	public void onWishlistClick() {
		wishlistPriorityView.setEnabled(wishlistView.isChecked());
	}

	private void initUi() {
		for (CheckBox checkBox : statusViews) {
			String status = (String) checkBox.getTag();
			checkBox.setChecked(selectedStatuses != null && selectedStatuses.contains(status));
		}
		wishlistPriorityView.setAdapter(new WishlistPriorityAdapter(getContext()));
		wishlistPriorityView.setSelection(wishlistPriority - 1);
		wishlistPriorityView.setEnabled(wishlistView.isChecked());
	}

	private static class WishlistPriorityAdapter extends ArrayAdapter<String> {
		public WishlistPriorityAdapter(Context context) {
			super(context,
				R.layout.spinner_textview,
				context.getResources().getStringArray(R.array.wishlist_priority_finite));
			setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		}
	}
}
