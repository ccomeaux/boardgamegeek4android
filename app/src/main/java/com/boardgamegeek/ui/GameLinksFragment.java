package com.boardgamegeek.ui;


import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.graphics.Palette;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.util.ActivityUtils;
import com.boardgamegeek.util.PaletteUtils;
import com.boardgamegeek.util.UIUtils;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.List;

import butterknife.BindViews;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import hugo.weaving.DebugLog;

public class GameLinksFragment extends Fragment {
	private Uri gameUri;
	private String gameName;

	Unbinder unbinder;
	@BindViews({
		R.id.icon_link_bgg,
		R.id.icon_link_bg_prices,
		R.id.icon_link_amazon,
		R.id.icon_link_ebay
	}) List<ImageView> colorizedIcons;

	private Palette palette = null;

	@Override
	@DebugLog
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final Intent intent = UIUtils.fragmentArgumentsToIntent(getArguments());
		gameUri = intent.getData();
		gameName = intent.getStringExtra(ActivityUtils.KEY_GAME_NAME);
	}

	@Override
	public void onStart() {
		super.onStart();
		EventBus.getDefault().register(this);
	}

	@Override
	public void onStop() {
		super.onStop();
		EventBus.getDefault().unregister(this);
	}

	@DebugLog
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_game_links, container, false);
		unbinder = ButterKnife.bind(this, rootView);

		colorize();

		return rootView;
	}

	@DebugLog
	@Override
	public void onDestroyView() {
		super.onDestroyView();
		if (unbinder != null) unbinder.unbind();
	}

	@SuppressWarnings("unused")
	@DebugLog
	@OnClick({ R.id.link_bgg, R.id.link_bg_prices, R.id.link_amazon, R.id.link_amazon_uk, R.id.link_amazon_de, R.id.link_ebay })
	void onLinkClick(View view) {
		switch (view.getId()) {
			case R.id.link_bgg:
				ActivityUtils.linkBgg(getActivity(), Games.getGameId(gameUri));
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
	public void onEvent(GameActivity.PaletteEvent event) {
		if (event.getGameId() == Games.getGameId(gameUri)) {
			palette = event.getPalette();
			colorize();
		}
	}

	@DebugLog
	private void colorize() {
		if (palette == null) return;
		Palette.Swatch swatch = PaletteUtils.getIconSwatch(palette);
		ButterKnife.apply(colorizedIcons, PaletteUtils.colorIconSetter, swatch);
	}
}
