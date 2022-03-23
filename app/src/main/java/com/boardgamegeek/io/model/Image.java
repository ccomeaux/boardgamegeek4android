package com.boardgamegeek.io.model;

import java.util.List;

@SuppressWarnings({ "unused", "SpellCheckingInspection" })
public class Image {
	String type;
	String id;
	Dimensions dimensions;
	int imageid;
	String caption;
	String postdate; // "2015-02-18T03:37:49+00:00"
	String gallery;
	int uploader;
	String extension;
	boolean hidden;
	Source source;
	String href;
	String canonical_link;
	String browse_href;
	public Images images;
	List<Link> links;

	static class Dimensions {
		int width;
		int height;
	}

	static class Source {
		String type;
		String id;
	}

	public static class Images {
		ImageData micro;
		public ImageData small;
		public ImageData medium;
		ImageData large;
		ImageData square;
		ImageData imagepage;
		ImageData crop100;
		ImageData square200;
		ImageData original;
	}

	public static class ImageData {
		public String url;
		int width;
		int height;
	}

	static class Link {
		String rel;
		String url;
	}
}
