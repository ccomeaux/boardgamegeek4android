package com.boardgamegeek.io.model;

import java.util.List;

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

	class Dimensions {
		int width;
		int height;
	}

	class Source {
		String type;
		String id;
	}

	public class Images {
		ImageData micro;
		ImageData small;
		ImageData medium;
		ImageData large;
		ImageData square;
		ImageData imagepage;
		ImageData crop100;
		ImageData square200;
		ImageData original;
	}

	public class ImageData {
		public String url;
		int width;
		int height;
	}

	class Link {
		String rel;
		String url;
	}
}
