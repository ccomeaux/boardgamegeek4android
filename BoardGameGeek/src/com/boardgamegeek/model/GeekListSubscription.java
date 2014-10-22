package com.boardgamegeek.model;

import java.util.Date;

public class GeekListSubscription extends Subscription {
    public String title;
    public int geekListId;
    public Date lastUpdated;

    @Override
    public String getType() {
        return Subscription.GEEKLIST_SUBSCRIPTION;
    }
}
