package com.boardgamegeek.model;

import java.util.ArrayList;
import java.util.List;

public class SubscriptionsResponse {
    public static final int PAGE_SIZE = 12;

    public List<GeekListSubscription> geekListSubscriptions;

    public List<ForumSubscription> forumSubscriptions;

    public List<Subscription> getAllSubscriptions() {
        List<Subscription> subscriptions = new ArrayList<>();
        subscriptions.addAll(geekListSubscriptions);
        subscriptions.addAll(forumSubscriptions);
        return subscriptions;
    }
}
