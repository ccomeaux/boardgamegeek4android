package com.boardgamegeek.ui.model;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class GeekListValue {
	public abstract int id();

	public abstract String title();

	public abstract String username();

	public abstract int numberOfItems();

	public abstract int numberOfThumbs();

	public abstract String description();

	public abstract long postTicks();

	public abstract long editTicks();

	public static GeekListValue.Builder builder() {
		return new AutoValue_GeekListValue.Builder();
	}

	@AutoValue.Builder
	public abstract static class Builder {
		public abstract GeekListValue.Builder setId(int value);

		public abstract GeekListValue.Builder setTitle(String value);

		public abstract GeekListValue.Builder setUsername(String value);

		public abstract GeekListValue.Builder setNumberOfItems(int value);

		public abstract GeekListValue.Builder setNumberOfThumbs(int value);

		public abstract GeekListValue.Builder setDescription(String value);

		public abstract GeekListValue.Builder setPostTicks(long value);

		public abstract GeekListValue.Builder setEditTicks(long value);

		public abstract GeekListValue build();
	}
}
