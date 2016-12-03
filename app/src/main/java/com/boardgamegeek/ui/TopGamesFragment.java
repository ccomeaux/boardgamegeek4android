package com.boardgamegeek.ui;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.model.TopGame;
import com.boardgamegeek.ui.adapter.TopGamesAdapter;
import com.boardgamegeek.util.AnimationUtils;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import rx.Single;
import rx.SingleSubscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import timber.log.Timber;

public class TopGamesFragment extends Fragment {
    Unbinder unbinder;

    @BindView(android.R.id.progress)
    View progressView;
    @BindView(android.R.id.empty)
    TextView emptyView;
    @BindView(android.R.id.list)
    RecyclerView recyclerView;

    TopGamesAdapter adapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_top_games, container, false);
        unbinder = ButterKnife.bind(this, rootView);
        setUpRecyclerView();
        loadTopGames();
        return rootView;
    }

    private void setUpRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false));
    }

    private void loadTopGames() {
        Single.create(new Single.OnSubscribe<List<TopGame>>() {
            @Override
            public void call(SingleSubscriber<? super List<TopGame>> singleSubscriber) {
                List<TopGame> topGames = findTopGames();
                singleSubscriber.onSuccess(topGames);
            }
        })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleSubscriber<List<TopGame>>() {
                    @Override
                    public void onSuccess(List<TopGame> topGames) {
                        if (topGames.isEmpty()) {
                            AnimationUtils.fadeIn(emptyView);
                        } else {
                            adapter = new TopGamesAdapter(topGames);
                            recyclerView.setAdapter(adapter);
                            AnimationUtils.fadeIn(getActivity(), recyclerView, isResumed());
                        }

                        AnimationUtils.fadeOut(progressView);
                    }

                    @Override
                    public void onError(Throwable error) {
                        Timber.e(error, "Error loading top games");

                        AnimationUtils.fadeIn(emptyView);
                        AnimationUtils.fadeOut(progressView);
                    }
                });
    }

    private List<TopGame> findTopGames() {
        List<TopGame> topGames = new ArrayList<>();

        try {
            int rank = 1;
            Document doc = Jsoup.connect("https://www.boardgamegeek.com/browse/boardgame").get();
            Elements gameElements = doc.select("td.collection_thumbnail");
            for (Element element : gameElements) {
                TopGame game = new TopGame();
                Element link = element.getElementsByTag("a").first();
                game.id = getGameIdFromLink(link.attr("href"));
                game.rank = rank;
                game.yearPublished = 2016;
                game.thumbnailUrl = link.child(0).attr("src");

                Element gameNameElement = element.parent().select(".collection_objectname").get(0).child(1);
                game.name = gameNameElement.child(0).text();
                String yearPublishedText = gameNameElement.child(1).text();
                game.yearPublished = Integer.parseInt(yearPublishedText.substring(1, yearPublishedText.length() - 1));

                topGames.add(game);
                rank++;
            }
        } catch (Throwable t) {
            Timber.e(t, "Error loading top games");
            return topGames;
        }

        return topGames;
    }

    private int getGameIdFromLink(String href) {
        int boardGameIndex = href.indexOf("/boardgame/");
        String afterBoardGameString;
        if (boardGameIndex != -1) {
            afterBoardGameString = href.substring(boardGameIndex + 11);
        } else {
            int boardGameExpansionIndex = href.indexOf("/boardgameexpansion/");
            afterBoardGameString = href.substring(boardGameExpansionIndex + 20);
        }
        int slashIndex = afterBoardGameString.indexOf("/");
        return Integer.parseInt(afterBoardGameString.substring(0, slashIndex));
    }
}
