package com.boardgamegeek.ui.model;

import com.boardgamegeek.model.Game.Comment;
import com.boardgamegeek.model.Game.Comments;
import com.boardgamegeek.model.ThingResponse;

public class GameComments extends PaginatedData<Comment> {
	public GameComments(Comments comments, int page) {
		super(comments.comments, comments.totalitems, page, ThingResponse.PAGE_SIZE);
	}

	public GameComments(Exception e) {
		super(e);
	}
}
