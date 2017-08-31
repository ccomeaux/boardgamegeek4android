package com.boardgamegeek.ui;


import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.ColorInt;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.ui.GameActivity.ColorEvent;
import com.boardgamegeek.util.ActivityUtils;
import com.boardgamegeek.util.PaletteUtils;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.List;

import butterknife.BindViews;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import hugo.weaving.DebugLog;

public class GameLinksFragment extends Fragment {
	private static final String KEY_GAME_ID = "GAME_ID";
	private static final String KEY_GAME_NAME = "GAME_NAME";
	private static final String KEY_ICON_COLOR = "ICON_COLOR";

	private int gameId;
	private String gameName;
	@ColorInt private int iconColor = Color.TRANSPARENT;

	Unbinder unbinder;
	@BindViews({
		R.id.icon_link_bgg,
		R.id.icon_link_bg_prices,
		R.id.icon_link_amazon,
		R.id.icon_link_ebay
	}) List<ImageView> colorizedIcons;

	public static GameLinksFragment newInstance(int gameId, String gameName, @ColorInt int iconColor) {
		Bundle args = new Bundle();
		args.putInt(KEY_GAME_ID, gameId);
		args.putString(KEY_GAME_NAME, gameName);
		args.putInt(KEY_ICON_COLOR, iconColor);
		GameLinksFragment fragment = new GameLinksFragment();
		fragment.setArguments(args);
		return fragment;
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		EventBus.getDefault().register(this);
	}

	@DebugLog
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_game_links, container, false);
		unbinder = ButterKnife.bind(this, rootView);
		readBundle(getArguments());
		colorize();
		return rootView;
	}

	private void readBundle(@Nullable Bundle bundle) {
		if (bundle == null) return;
		gameId = bundle.getInt(KEY_GAME_ID, BggContract.INVALID_ID);
		gameName = bundle.getString(KEY_GAME_NAME);
		iconColor = bundle.getInt(KEY_ICON_COLOR, Color.TRANSPARENT);
	}

	@DebugLog
	@Override
	public void onDestroyView() {
		super.onDestroyView();
		if (unbinder != null) unbinder.unbind();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		EventBus.getDefault().unregister(this);
	}

	@SuppressWarnings("unused")
	@DebugLog
	@OnClick({ R.id.link_bgg, R.id.link_bg_prices, R.id.link_amazon, R.id.link_amazon_uk, R.id.link_amazon_de, R.id.link_ebay })
	void onLinkClick(View view) {
		switch (view.getId()) {
			case R.id.link_bgg:
				ActivityUtils.linkBgg(getActivity(), gameId);
				break;
			case R.id.link_bg_prices:
				ActivityUtils.linkBgPrices(getActivity(), gameName);
				break;
			case R.id.link_amazon:
				ActivityUtils.linkAmazon(getActivity(), gameName, ActivityUtils.LINK_AMAZON_COM);
				break;
			case R.id.link_amazon_uk:
				ActivityUtils.linkAmazon(getActivity(), gameName, ActivityUtils.LINK_AMAZON_UK);
				break;
			case R.id.link_amazon_de:
				ActivityUtils.linkAmazon(getActivity(), gameName, ActivityUtils.LINK_AMAZON_DE);
				break;
			case R.id.link_ebay:
				ActivityUtils.linkEbay(getActivity(), gameName);
				break;
		}
	}

	@SuppressWarnings("unused")
	@Subscribe
	public void onEvent(ColorEvent event) {
		if (event.getGameId() == gameId) {
			iconColor = event.getIconColor();
			colorize();
		}
	}

	@DebugLog
	private void colorize() {
		if (!isAdded()) return;
		if (iconColor != Color.TRANSPARENT) {
			ButterKnife.apply(colorizedIcons, PaletteUtils.rgbIconSetter, iconColor);
		}
	}
}
