package com.boardgamegeek.model;

import java.util.Date;

public class ForumSubscription extends Subscription {
    public String game;
    public String subject;
    public int forumId;
    public Date lastUpdated;
    public int gameId;
    public String forumName;
    public int threadId;

    @Override
    public String getType() {
        return Subscription.FORUM_SUBSCRIPTION;
    }
}
