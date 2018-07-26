package com.boardgamegeek.ui.model;

import com.boardgamegeek.io.model.Game.Comment;
import com.boardgamegeek.io.model.Game.Comments;

public class GameComments extends PaginatedData<Comment> {
	public static final int PAGE_SIZE = 100;

	public GameComments(Comments comments, int page) {
		super(comments.comments, comments.totalitems, page, PAGE_SIZE);
	}

	public GameComments(Exception e) {
		super(e);
	}
}
