package com.boardgamegeek.ui.model;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class Article {
	public abstract int id();

	public abstract String username();

	public abstract String link();

	public abstract long postTicks();

	public abstract long editTicks();

	public abstract int numberOfEdits();

	public abstract String body();

	public static Builder builder() {
		return new AutoValue_Article.Builder();
	}

	@AutoValue.Builder
	public abstract static class Builder {
		public abstract Builder setId(int value);

		public abstract Builder setUsername(String value);

		public abstract Builder setLink(String value);

		public abstract Builder setPostTicks(long value);

		public abstract Builder setEditTicks(long value);

		public abstract Builder setNumberOfEdits(int value);

		public abstract Builder setBody(String value);

		public abstract Article build();
	}
}