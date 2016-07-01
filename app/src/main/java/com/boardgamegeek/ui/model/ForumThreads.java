package com.boardgamegeek.ui.model;

import com.boardgamegeek.model.ForumResponse;
import com.boardgamegeek.model.Thread;

public class ForumThreads extends PaginatedData<Thread> {
	public ForumThreads(ForumResponse response, int page) {
		super(response.getThreads(), response.numberOfThreads(), page, ForumResponse.PAGE_SIZE);
	}

	public ForumThreads(Exception e) {
		super(e);
	}
}

