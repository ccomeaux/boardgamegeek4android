package com.boardgamegeek.ui;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.View;
import android.widget.AbsListView;
import android.widget.ListView;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.auth.Authenticator;
import com.boardgamegeek.model.ForumSubscription;
import com.boardgamegeek.model.GeekListSubscription;
import com.boardgamegeek.model.Subscription;
import com.boardgamegeek.model.SubscriptionsResponse;
import com.boardgamegeek.ui.widget.PaginatedArrayAdapter;
import com.boardgamegeek.ui.widget.PaginatedData;
import com.boardgamegeek.ui.widget.PaginatedLoader;
import com.boardgamegeek.util.DateTimeUtils;
import com.boardgamegeek.util.ForumsUtils;
import com.boardgamegeek.util.GeekListUtils;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;

public class SubscriptionsFragment extends BggListFragment implements AbsListView.OnScrollListener,
        LoaderManager.LoaderCallbacks<PaginatedData<Subscription>> {

    private static final int SUBSCRIPTIONS_LOADER_ID = 0;

    private SubscriptionsAdapter mSubscriptionsAdapter;

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        getListView().setOnScrollListener(this);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setEmptyText(getString(R.string.empty_subscriptions));
    }

    @Override
    public void onResume() {
        super.onResume();
        getLoaderManager().initLoader(SUBSCRIPTIONS_LOADER_ID, null, this);
    }

    public void loadMoreResults() {
        if (isAdded()) {
            Loader<List<Subscription>> loader = getLoaderManager().getLoader(SUBSCRIPTIONS_LOADER_ID);
            if (loader != null) {
                loader.forceLoad();
            }
        }
    }

    @Override
    public void onListItemClick(ListView listView, View convertView, int position, long id) {
        SubscriptionRowViewBinder.ViewHolder holder = (SubscriptionRowViewBinder.ViewHolder) convertView.getTag();
        if (holder != null) {
            if (holder.type.equals(Subscription.GEEKLIST_SUBSCRIPTION)) {
                Intent intent = new Intent(getActivity(), GeekListActivity.class);
                intent.putExtra(GeekListUtils.KEY_ID, holder.id);
                intent.putExtra(GeekListUtils.KEY_TITLE, holder.title.getText());
                startActivity(intent);
            }
            else if (holder.type.equals(Subscription.FORUM_SUBSCRIPTION)) {
                Intent intent = new Intent(getActivity(), ThreadActivity.class);
                intent.putExtra(ForumsUtils.KEY_THREAD_ID, holder.id);
                intent.putExtra(ForumsUtils.KEY_THREAD_SUBJECT, holder.title.getText());
                startActivity(intent);
            }
        }
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
    }

    @Override
    public void onScroll(AbsListView absListView, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        if (!isLoading() && loaderHasMoreResults() && visibleItemCount != 0
                && firstVisibleItem + visibleItemCount >= totalItemCount - 1) {
            saveScrollState();
            loadMoreResults();
        }
    }

    @Override
    public Loader<PaginatedData<Subscription>> onCreateLoader(int id, Bundle data) {
        return new SubscriptionsLoader(getActivity());
    }

    @Override
    public void onLoadFinished(Loader<PaginatedData<Subscription>> loader, PaginatedData<Subscription> data) {
        if (getActivity() == null) {
            return;
        }

        if (mSubscriptionsAdapter == null) {
            mSubscriptionsAdapter = new SubscriptionsAdapter(getActivity(), R.layout.row_subscription, data);
            setListAdapter(mSubscriptionsAdapter);
        } else {
            mSubscriptionsAdapter.update(data);
        }
        restoreScrollState();
    }

    @Override
    public void onLoaderReset(Loader<PaginatedData<Subscription>> loader) {
    }

    private boolean isLoading() {
        final SubscriptionsLoader loader = getLoader();
        return (loader != null) ? loader.isLoading() : true;
    }

    private boolean loaderHasMoreResults() {
        final SubscriptionsLoader loader = getLoader();
        return (loader != null) ? loader.hasMoreResults() : false;
    }

    private SubscriptionsLoader getLoader() {
        if (isAdded()) {
            Loader<PaginatedData<Subscription>> loader = getLoaderManager().getLoader(SUBSCRIPTIONS_LOADER_ID);
            return (SubscriptionsLoader) loader;
        }
        return null;
    }

    private static class SubscriptionsLoader extends PaginatedLoader<Subscription> {
        private Context context;

        public SubscriptionsLoader(Context context) {
            super(context);
            this.context = context;
        }

        @Override
        public PaginatedData<Subscription> loadInBackground() {
            super.loadInBackground();
            SubscriptionsData data;
            try {
                int page = getNextPage();
                data = new SubscriptionsData(getSubscriptions(context, page), page);
            } catch (Exception e) {
                data = new SubscriptionsData(e);
            }
            return data;
        }
    }

    private static SubscriptionsResponse getSubscriptions(Context context, int page) {
        SubscriptionsResponse response = new SubscriptionsResponse();

        List<ForumSubscription> forumSubscriptions = new ArrayList<>();
        List<GeekListSubscription> geekListSubscriptions = new ArrayList<>();

        try {
            Connection connection = Jsoup.connect("http://boardgamegeek.com/subscriptions");
            connection = connection.timeout(10000);

            AccountManager accountManager = AccountManager.get(context);
            final Account account = Authenticator.getAccount(context);
            try {
                final String authToken = accountManager.blockingGetAuthToken(account, Authenticator.AUTHTOKEN_TYPE, true);
                connection = connection.cookie("bggusername", account.name);
                connection = connection.cookie("bggpassword", authToken);
            } catch (OperationCanceledException | AuthenticatorException | IOException e) {
                //todo
            }

            Document doc = connection.post();

            Elements forumRows = doc.select("#module_1 tr");
            if (!forumRows.isEmpty()) {
                for (Element forumRow : forumRows) {
                    try {
                        ForumSubscription forumSubscription = getForumSubscription(forumRow);
                        if (forumSubscription != null) {
                            forumSubscriptions.add(forumSubscription);
                        }
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            Elements geekListRows = doc.select("#module_2 tr");
            if (!geekListRows.isEmpty()) {
                for (Element geekListRow : geekListRows) {
                    try {
                        GeekListSubscription geekListSubscription = getGeekListSubscription(geekListRow);
                        if (geekListSubscription != null) {
                            geekListSubscriptions.add(geekListSubscription);
                        }
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }

        response.forumSubscriptions = forumSubscriptions;
        response.geekListSubscriptions = geekListSubscriptions;

        return response;
    }

    private static ForumSubscription getForumSubscription(Element forumRow) throws ParseException
    {
        ForumSubscription forumSubscription = new ForumSubscription();
        String idString = forumRow.attr("id");
        if (idString == null || idString.isEmpty()) {
            return null;
        }
        String prefix = "GSUB_itemline_thread_";
        int threadId = Integer.parseInt(idString.substring(prefix.length()));
        forumSubscription.threadId = threadId;

        Elements columns = forumRow.getElementsByTag("td");

        Element forumColumn = columns.get(0);
        Element forumSpan = forumColumn.getElementsByTag("span").first();
        Elements forumLinks = forumSpan.getElementsByTag("a");
        Element forumLink = forumLinks.get(0);

        String gameUrl = forumLink.attr("href");
        String gameIdSubString = gameUrl.substring(gameUrl.indexOf("/boardgame/") + 11);
        try {
            int gameId = Integer.parseInt(gameIdSubString.substring(0, gameIdSubString.indexOf("/")));
            forumSubscription.gameId = gameId;
        } catch (NumberFormatException ignored) {}

        String game = forumLink.text();
        forumSubscription.game = game;

        if (forumLinks.size() > 1) {
            Element forumParentLink = forumLinks.get(1);
            forumSubscription.forumName = forumParentLink.text();

            String forumUrl = forumParentLink.attr("href");
            String forumIdSubString = forumUrl.substring(forumUrl.indexOf("/forum/") + 7);
            try {
                int forumId = Integer.parseInt(forumIdSubString.substring(0, forumIdSubString.indexOf("/")));
                forumSubscription.forumId = forumId;
            } catch (NumberFormatException ignored) {}
        }

        Element subjectColumn = columns.get(1);
        Element subjectSpan = subjectColumn.getElementsByTag("span").last();
        Element subjectLink = subjectSpan.getElementsByTag("a").first();
        String subject = subjectLink.text();
        forumSubscription.subject = subject;

        Element updatedColumn = columns.get(4);
        Elements updatedColumnDivs = updatedColumn.getElementsByTag("div");
        Element lastUpdatedDiv = null;
        for (Element updatedColumnDiv : updatedColumnDivs) {
            if (updatedColumnDiv.text().startsWith("from ")) {
                lastUpdatedDiv = updatedColumnDiv;
                break;
            }
        }
        if (lastUpdatedDiv != null) {
            String lastUpdatedString = lastUpdatedDiv.text();
            lastUpdatedString = lastUpdatedString.substring(5);
            Date lastUpdated = DateTimeUtils.getDateFromBggString(lastUpdatedString);
            forumSubscription.lastUpdated = lastUpdated;
        }

        return forumSubscription;
    }

    private static GeekListSubscription getGeekListSubscription(Element geekListRow) throws ParseException
    {
        GeekListSubscription geekListSubscription = new GeekListSubscription();
        String idString = geekListRow.attr("id");
        if (idString == null || idString.isEmpty()) {
            return null;
        }
        String prefix = "GSUB_itemline_geeklist_";
        int geekListId = Integer.parseInt(idString.substring(prefix.length()));
        geekListSubscription.geekListId = geekListId;

        Elements columns = geekListRow.getElementsByTag("td");

        Element titleColumn = columns.get(0);
        Element titleSpan = titleColumn.getElementsByTag("span").last();
        Element titleLink = titleSpan.getElementsByTag("a").first();
        String title = titleLink.text();
        geekListSubscription.title = title;

        Element updatedColumn = columns.get(3);
        Elements updatedColumnDivs = updatedColumn.getElementsByTag("div");
        Element lastUpdatedDiv = null;
        for (Element updatedColumnDiv : updatedColumnDivs) {
            if (updatedColumnDiv.text().startsWith("from ")) {
                lastUpdatedDiv = updatedColumnDiv;
                break;
            }
        }
        if (lastUpdatedDiv != null) {
            String lastUpdatedString = lastUpdatedDiv.text();
            lastUpdatedString = lastUpdatedString.substring(5);
            Date lastUpdated = DateTimeUtils.getDateFromBggString(lastUpdatedString);
            geekListSubscription.lastUpdated = lastUpdated;
        }

        return geekListSubscription;
    }

    static class SubscriptionsData extends PaginatedData<Subscription> {
        public SubscriptionsData(SubscriptionsResponse response, int page) {
            super(response.getAllSubscriptions(), response.getAllSubscriptions().size(), page, SubscriptionsResponse.PAGE_SIZE);
        }

        public SubscriptionsData(Exception e) {
            super(e);
        }
    }

    private class SubscriptionsAdapter extends PaginatedArrayAdapter<Subscription> {
        public SubscriptionsAdapter(Context context, int resource, PaginatedData<Subscription> data) {
            super(context, resource, data);
        }

        @Override
        protected boolean isLoaderLoading() {
            return isLoading();
        }

        @Override
        protected void bind(View view, Subscription subscription) {
            SubscriptionRowViewBinder.bindActivityView(view, subscription);
        }
    }

    public static class SubscriptionRowViewBinder {
        public static class ViewHolder {
            public int id;
            public String type;
            @InjectView(R.id.subscription_title)
            TextView title;

            public ViewHolder(View view) {
                ButterKnife.inject(this, view);
            }
        }

        public static void bindActivityView(View rootView, Subscription subscription) {
            ViewHolder tag = (ViewHolder) rootView.getTag();
            final ViewHolder holder;
            if (tag != null) {
                holder = tag;
            } else {
                holder = new ViewHolder(rootView);
                rootView.setTag(holder);
            }

            holder.type = subscription.getType();

            if (subscription.isGeekListSubscription()) {
                GeekListSubscription geekListSubscription = (GeekListSubscription) subscription;
                holder.id = geekListSubscription.geekListId;
                holder.title.setText("GeekList: " + geekListSubscription.title);
            }
            else if(subscription.isForumSubscription()) {
                ForumSubscription forumSubscription = (ForumSubscription) subscription;
                holder.id = forumSubscription.threadId;
                holder.title.setText("Forum: " + forumSubscription.game + ">>" + forumSubscription.forumName + " - Subject: " + forumSubscription.subject);
            }
        }
    }
}
