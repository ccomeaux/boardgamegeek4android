package com.boardgamegeek.model;

public abstract class Subscription {
    public static final String GEEKLIST_SUBSCRIPTION = "geeklist_subscription";
    public static final String FORUM_SUBSCRIPTION = "forum_subscription";

    public abstract String getType();

    public boolean isGeekListSubscription() {
        return GEEKLIST_SUBSCRIPTION.equals(getType());
    }

    public boolean isForumSubscription() {
        return FORUM_SUBSCRIPTION.equals(getType());
    }
}
