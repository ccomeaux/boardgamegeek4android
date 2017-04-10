package com.boardgamegeek.ui.model;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class GeekList {
	public abstract int id();

	public abstract String title();

	public abstract String username();

	public abstract int numberOfItems();

	public abstract int numberOfThumbs();

	public abstract String description();

	public abstract long postTicks();

	public abstract long editTicks();

	public static GeekList.Builder builder() {
		return new AutoValue_GeekList.Builder();
	}

	@AutoValue.Builder
	public abstract static class Builder {
		public abstract GeekList.Builder setId(int value);

		public abstract GeekList.Builder setTitle(String value);

		public abstract GeekList.Builder setUsername(String value);

		public abstract GeekList.Builder setNumberOfItems(int value);

		public abstract GeekList.Builder setNumberOfThumbs(int value);

		public abstract GeekList.Builder setDescription(String value);

		public abstract GeekList.Builder setPostTicks(long value);

		public abstract GeekList.Builder setEditTicks(long value);

		public abstract GeekList build();
	}
}
