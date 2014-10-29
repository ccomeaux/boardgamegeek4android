package com.boardgamegeek.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.annotation.SuppressLint;
import android.text.TextUtils;

public class XmlConverter {
	private static final String BASE_URL = "https://boardgamegeek.com";
	private static final String STATIC_IMAGES_URL = "https://cf.geekdo-static.com/images/";
	private static final String IMAGES_URL = "https://cf.geekdo-images.com/images/";
	private List<Replacable> mReplacers;

	public XmlConverter() {
		mReplacers = new ArrayList<Replacable>();
		mReplacers.add(new SimpleReplacer("\\[hr\\]", "<hr/>"));
		mReplacers.add(new SimpleReplacer("\\[clear\\]", "<div style=\"clear:both\"></div>"));
		createPair("b");
		createPair("i");
		createPair("u");
		createPair("-", "strike");
		mReplacers.add(new SimpleReplacer("\\[floatleft\\]", "<div style=\"float:left\">"));
		mReplacers.add(new SimpleReplacer("\\[/floatleft\\]", "</div>"));
		createPair("center");
		mReplacers.add(new SimpleReplacer("\\[floatright\\]", "<div style=\"float:right\">"));
		mReplacers.add(new SimpleReplacer("\\[/floatright\\]", "</div>"));
		mReplacers.add(new Replacer("\\[COLOR=([^#].*?)\\]", "<span style=\"color:", "\">"));
		mReplacers.add(new Replacer("\\[COLOR=#(.*?)\\]", "<span style=\"color:#", "\">"));
		mReplacers.add(new SimpleReplacer("\\[/COLOR\\]", "</span>"));
		mReplacers.add(new Replacer("\\[BGCOLOR=([^#].*?)\\]", "<span style=\"background-color:", "\">"));
		mReplacers.add(new Replacer("\\[BGCOLOR=#(.*?)\\]", "<span style=\"background-color#:", "\">"));
		mReplacers.add(new SimpleReplacer("\\[/BGCOLOR\\]", "</span>"));
		// TODO: determine when image is a PNG
		mReplacers.add(new Replacer("\\[ImageID=(\\d+).*?\\]", "<div style=\"display:inline\"><img src=\"" + IMAGES_URL
			+ "pic", "_t.jpg\"/></div>"));
		// TODO: image, YouTube, Vimeo, tweet, mp3
		mReplacers.add(new GeekUrlReplacer());
		mReplacers.add(GeekLinkReplacer.createNumeric("thing"));
		mReplacers.add(GeekLinkReplacer.createNumeric("thread"));
		mReplacers.add(GeekLinkReplacer.createNumeric("article", "reply"));// TODO: add #id
		mReplacers.add(GeekLinkReplacer.createNumeric("geeklist", "GeekList"));
		mReplacers.add(GeekLinkReplacer.createNumeric("filepage", "file"));
		mReplacers.add(GeekLinkReplacer.createNumeric("person"));
		mReplacers.add(GeekLinkReplacer.createNumeric("company"));
		mReplacers.add(GeekLinkReplacer.createNumeric("property"));
		mReplacers.add(GeekLinkReplacer.createNumeric("family"));
		mReplacers.add(GeekLinkReplacer.createNumeric("guild"));
		mReplacers.add(GeekLinkReplacer.createAlpha("user"));
		mReplacers.add(GeekLinkReplacer.createNumeric("question", "GeekQuestion"));
		mReplacers.add(GeekLinkReplacer.createNumeric("media", "podcast episode"));
		mReplacers.add(GeekLinkReplacer.createNumeric("blog"));
		mReplacers.add(GeekLinkReplacer.createNumeric("blogpost", "blog post"));
		mReplacers.add(new SimpleReplacer("\\[q\\]", "Quote:<blockquote>"));
		mReplacers.add(new Replacer("\\[q=\"(.*?)\"\\]", "", " wrote:<blockquote>"));
		mReplacers.add(new SimpleReplacer("\\[/q\\]", "</blockquote>"));
		mReplacers.add(new SimpleReplacer("\\[o\\]", "Spoiler: <span style=\"color:white\">"));
		mReplacers.add(new SimpleReplacer("\\[/o\\]", "</span>"));
		createPair("c", "tt");
		mReplacers.add(new UrlReplacer());
		mReplacers.add(new UrlReplacer2());
		// TODO: roll
		// TODO: size isn't working
		mReplacers.add(new Replacer("\\[size=(.*?)\\]", "<span font-size=\"", "px\">"));
		mReplacers.add(new SimpleReplacer("\\[/size\\]", "</span>"));

		mReplacers.add(SimpleReplacer.createCustomImage("\\:\\)\\s", "smile.gif"));
		mReplacers.add(SimpleReplacer.createCustomImage("\\:\\(\\s", "sad.gif"));
		mReplacers.add(SimpleReplacer.createCustomImage("\\:D\\s", "biggrin.gif"));
		mReplacers.add(SimpleReplacer.createCustomImage("\\:p\\s", "tongue.gif"));
		mReplacers.add(SimpleReplacer.createCustomImage("\\;\\)\\s", "wink.gif"));
		mReplacers.add(SimpleReplacer.createImage("what", "rock.gif"));
		mReplacers.add(SimpleReplacer.createGif("wow"));
		mReplacers.add(SimpleReplacer.createGif("angry"));
		mReplacers.add(SimpleReplacer.createGif("cool"));
		mReplacers.add(SimpleReplacer.createGif("laugh"));
		mReplacers.add(SimpleReplacer.createGif("meeple"));
		mReplacers.add(SimpleReplacer.createGif("surprise"));
		mReplacers.add(SimpleReplacer.createGif("blush"));
		mReplacers.add(SimpleReplacer.createGif("snore"));
		mReplacers.add(SimpleReplacer.createGif("cry"));
		mReplacers.add(SimpleReplacer.createGif("kiss"));
		mReplacers.add(SimpleReplacer.createGif("modest"));
		mReplacers.add(SimpleReplacer.createGif("whistle"));
		mReplacers.add(SimpleReplacer.createGif("devil"));
		mReplacers.add(SimpleReplacer.createGif("soblue"));
		mReplacers.add(SimpleReplacer.createGif("yuk"));
		mReplacers.add(SimpleReplacer.createGif("gulp"));
		mReplacers.add(SimpleReplacer.createGif("shake"));
		mReplacers.add(SimpleReplacer.createGif("arrrh"));
		mReplacers.add(SimpleReplacer.createGif("zombie"));
		mReplacers.add(SimpleReplacer.createGif("robot"));
		mReplacers.add(SimpleReplacer.createGif("ninja"));
		mReplacers.add(SimpleReplacer.createGif("sauron"));
		mReplacers.add(SimpleReplacer.createGif("goo"));
		mReplacers.add(SimpleReplacer.createImage("star", "star_yellow.gif"));
		mReplacers.add(SimpleReplacer.createImage("halfstar", "star_yellowhalf.gif"));
		mReplacers.add(SimpleReplacer.createImage("nostar", "star_white.gif"));
		mReplacers.add(SimpleReplacer.createImage("gg", "geekgold.gif"));
		mReplacers.add(SimpleReplacer.createGif("bag"));
		mReplacers.add(SimpleReplacer.createGif("bacon"));
		mReplacers.add(SimpleReplacer.createGif("caravan"));
		createCamelImage("mint");
		createCamelImage("lime");
		createCamelImage("grape");
		createCamelImage("lemon");
		createCamelImage("orange");
		mReplacers.add(SimpleReplacer.createGif("goldencamel"));
		createTajImage("blue");
		createTajImage("brown");
		createTajImage("gray");
		createTajImage("maroon");
		createTajImage("tan");
		createTajImage("white");
		mReplacers.add(SimpleReplacer.createImage("thumbsup", "thumbs-up.gif"));
		mReplacers.add(SimpleReplacer.createImage("thumbsdown", "thumbs-down.gif"));
		mReplacers.add(SimpleReplacer.createGif("coffee"));
		mReplacers.add(SimpleReplacer.createGif("tobacco"));
		mReplacers.add(SimpleReplacer.createGif("indigo"));
		mReplacers.add(SimpleReplacer.createGif("sugar"));
		mReplacers.add(SimpleReplacer.createGif("corn"));
		mReplacers.add(SimpleReplacer.createGif("colonist"));
		mReplacers.add(SimpleReplacer.createGif("1vp"));
		mReplacers.add(SimpleReplacer.createGif("5vp"));
		mReplacers.add(SimpleReplacer.createGif("1db"));
		mReplacers.add(SimpleReplacer.createGif("5db"));
		for (int i = 0; i <= 9; i++) {
			mReplacers.add(SimpleReplacer.createImage("d10-" + i, "d10-" + i + ".gif"));
		}
		mReplacers.add(SimpleReplacer.createImage("city", "ttr_city.gif"));
		createTrainImage("red");
		createTrainImage("green");
		createTrainImage("blue");
		createTrainImage("yellow");
		createTrainImage("black");
		createTrainImage("purple");
		createTrainImage("white");
		mReplacers.add(SimpleReplacer.createGif("wood"));
		mReplacers.add(SimpleReplacer.createGif("wheat"));
		mReplacers.add(SimpleReplacer.createGif("sheep"));
		mReplacers.add(SimpleReplacer.createGif("ore"));
		mReplacers.add(SimpleReplacer.createGif("brick"));
		mReplacers.add(SimpleReplacer.createGif("cinnamon"));
		mReplacers.add(SimpleReplacer.createGif("clove"));
		mReplacers.add(SimpleReplacer.createGif("ginger"));
		mReplacers.add(SimpleReplacer.createGif("nutmeg"));
		mReplacers.add(SimpleReplacer.createGif("pepper"));
		mReplacers.add(SimpleReplacer.createGif("coal"));
		mReplacers.add(SimpleReplacer.createGif("oil"));
		mReplacers.add(SimpleReplacer.createGif("trash"));
		mReplacers.add(SimpleReplacer.createGif("nuclear"));
		for (int i = 1; i <= 6; i++) {
			mReplacers.add(SimpleReplacer.createImage("d6-" + i, "die-white-" + i + ".gif"));
			mReplacers.add(SimpleReplacer.createImage("bd6-" + i, "die-black-" + i + ".gif"));
		}
		mReplacers.add(SimpleReplacer.createPng("tankard"));
		mReplacers.add(SimpleReplacer.createPng("jug"));
		mReplacers.add(SimpleReplacer.createPng("chalice"));
		mReplacers.add(SimpleReplacer.createGif("worker"));
		mReplacers.add(SimpleReplacer.createGif("building"));
		mReplacers.add(SimpleReplacer.createGif("aristocrat"));
		mReplacers.add(SimpleReplacer.createGif("trade"));
		mReplacers.add(SimpleReplacer.createPng("arrowN"));
		mReplacers.add(SimpleReplacer.createPng("arrowNE"));
		mReplacers.add(SimpleReplacer.createPng("arrowE"));
		mReplacers.add(SimpleReplacer.createPng("arrowSE"));
		mReplacers.add(SimpleReplacer.createPng("arrowS"));
		mReplacers.add(SimpleReplacer.createPng("arrowSW"));
		mReplacers.add(SimpleReplacer.createPng("arrowW"));
		mReplacers.add(SimpleReplacer.createPng("arrowNW"));
		mReplacers.add(SimpleReplacer.createPng("power"));
		mReplacers.add(SimpleReplacer.createPng("XBA"));
		mReplacers.add(SimpleReplacer.createPng("XBB"));
		mReplacers.add(SimpleReplacer.createPng("XBX"));
		mReplacers.add(SimpleReplacer.createPng("XBY"));
		mReplacers.add(SimpleReplacer.createPng("PSC"));
		mReplacers.add(SimpleReplacer.createPng("PSS"));
		mReplacers.add(SimpleReplacer.createPng("PST"));
		mReplacers.add(SimpleReplacer.createPng("PSX"));
		mReplacers.add(SimpleReplacer.createPng("WiiH"));
		mReplacers.add(SimpleReplacer.createPng("Wii1"));
		mReplacers.add(SimpleReplacer.createPng("Wii2"));
		mReplacers.add(SimpleReplacer.createPng("WiiA"));
		mReplacers.add(SimpleReplacer.createPng("WiiB"));
		mReplacers.add(SimpleReplacer.createPng("WiiC"));
		mReplacers.add(SimpleReplacer.createPng("WiiX"));
		mReplacers.add(SimpleReplacer.createPng("WiiY"));
		mReplacers.add(SimpleReplacer.createPng("WiiZ"));
		mReplacers.add(SimpleReplacer.createPng("Wii\\+"));
		mReplacers.add(SimpleReplacer.createPng("Wii-"));
		mReplacers.add(SimpleReplacer.createImage("!block", "bang_block.png"));
		mReplacers.add(SimpleReplacer.createImage("\\?block", "question_block.png"));
		mReplacers.add(SimpleReplacer.createImage("blank", "tiles/BLANK.gif"));
		mReplacers.add(new UpperCaseReplacer("\\:([A-Za-z])\\:", "<img src=\"" + STATIC_IMAGES_URL + "tiles/",
			".gif\"/>"));
		mReplacers.add(new Replacer("\\:k([A-Za-z])\\:", "<img src=\"" + STATIC_IMAGES_URL + "k", ".png\"/>"));
	}

	private void createPair(String tag) {
		mReplacers.add(SimpleReplacer.createStart(tag));
		mReplacers.add(SimpleReplacer.createEnd(tag));
	}

	private void createPair(String tag, String replacementTag) {
		mReplacers.add(SimpleReplacer.createStart(tag, replacementTag));
		mReplacers.add(SimpleReplacer.createEnd(tag, replacementTag));
	}

	private void createCamelImage(String color) {
		mReplacers.add(SimpleReplacer.createImage(color + "camel", "camel_" + color + ".gif"));
	}

	private void createTajImage(String color) {
		mReplacers.add(SimpleReplacer.createImage(color + "taj", "taj_" + color + ".gif"));
	}

	private void createTrainImage(String color) {
		mReplacers.add(SimpleReplacer.createImage(color + "train", "ttr_" + color + ".gif"));
	}

	interface Replacable {
		String replace(String text);
	}

	private static class SimpleReplacer implements Replacable {
		Pattern mPattern;
		String mReplacement;

		public static SimpleReplacer createStart(String tag) {
			return new SimpleReplacer("\\[" + tag + "\\]", "<" + tag + ">");
		}

		public static SimpleReplacer createStart(String tag, String replacementTag) {
			return new SimpleReplacer("\\[" + tag + "\\]", "<" + replacementTag + ">");
		}

		public static SimpleReplacer createEnd(String tag) {
			return new SimpleReplacer("\\[/" + tag + "\\]", "</" + tag + ">");
		}

		public static SimpleReplacer createEnd(String tag, String replacementTag) {
			return new SimpleReplacer("\\[/" + tag + "\\]", "</" + replacementTag + ">");
		}

		public static SimpleReplacer createGif(String image) {
			return new SimpleReplacer("\\:" + image + "\\:", "<img src=\"" + STATIC_IMAGES_URL + image + ".gif\"/>");
		}

		public static SimpleReplacer createPng(String image) {
			return new SimpleReplacer("\\:" + image + "\\:", "<img src=\"" + STATIC_IMAGES_URL + image + ".png\"/>");
		}

		public static SimpleReplacer createImage(String image, String imageFile) {
			return new SimpleReplacer("\\:" + image + "\\:", "<img src=\"" + STATIC_IMAGES_URL + imageFile + "\"/>");
		}

		public static SimpleReplacer createCustomImage(String image, String imageFile) {
			return new SimpleReplacer(image, "<img src=\"" + STATIC_IMAGES_URL + imageFile + "\"/>");
		}

		public SimpleReplacer(String pattern, String replacement) {
			mPattern = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
			mReplacement = replacement;
		}

		@Override
		public String replace(String text) {
			Matcher matcher = mPattern.matcher(text);
			if (matcher.find()) {
				return matcher.replaceAll(mReplacement);
			}
			return text;
		}
	}

	private static class Replacer implements Replacable {
		private Pattern mPattern;
		private String mPrefix;
		private String mSuffix;

		public Replacer(String pattern, String prefix, String suffix) {
			mPattern = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
			mPrefix = prefix;
			mSuffix = suffix;
		}

		@Override
		public String replace(String text) {
			Matcher matcher = mPattern.matcher(text);
			StringBuffer buffer = new StringBuffer();
			while (matcher.find()) {
				matcher.appendReplacement(buffer, mPrefix + matcher.group(1) + mSuffix);
			}
			matcher.appendTail(buffer);
			return buffer.toString();
		}
	}

	private static class UpperCaseReplacer implements Replacable {
		private Pattern mPattern;
		private String mPrefix;
		private String mSuffix;

		public UpperCaseReplacer(String pattern, String prefix, String suffix) {
			mPattern = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
			mPrefix = prefix;
			mSuffix = suffix;
		}

		@SuppressLint("DefaultLocale")
		@Override
		public String replace(String text) {
			Matcher matcher = mPattern.matcher(text);
			StringBuffer buffer = new StringBuffer();
			while (matcher.find()) {
				matcher.appendReplacement(buffer, mPrefix + matcher.group(1).toUpperCase() + mSuffix);
			}
			matcher.appendTail(buffer);
			return buffer.toString();
		}
	}

	public static class UrlReplacer implements Replacable {
		private Pattern mPattern;

		public UrlReplacer() {
			mPattern = Pattern.compile("\\[url\\](.*?)\\[/url\\]", Pattern.CASE_INSENSITIVE);
		}

		@Override
		public String replace(String text) {
			Matcher matcher = mPattern.matcher(text);
			StringBuffer result = new StringBuffer();
			while (matcher.find()) {
				String url = HttpUtils.ensureScheme(matcher.group(1));
				matcher.appendReplacement(result, "<a href=\"" + url + "\">" + url + "</a>");
			}
			matcher.appendTail(result);
			return result.toString();
		}
	}

	public static class UrlReplacer2 implements Replacable {
		private Pattern mPattern;

		public UrlReplacer2() {
			mPattern = Pattern.compile("\\[url=(.*?)\\](.*?)\\[/url\\]", Pattern.CASE_INSENSITIVE);
		}

		@Override
		public String replace(String text) {
			Matcher matcher = mPattern.matcher(text);
			StringBuffer result = new StringBuffer();
			while (matcher.find()) {
				String url = HttpUtils.ensureScheme(matcher.group(1));
				String displayText = matcher.group(2);
				if (TextUtils.isEmpty(displayText)) {
					matcher.appendReplacement(result, "<a href=\"" + url + "\">" + url + "</a>");
				} else {
					matcher.appendReplacement(result, "<a href=\"" + url + "\">" + displayText + "</a>");
				}
			}
			matcher.appendTail(result);
			return result.toString();
		}
	}

	public static class GeekUrlReplacer implements Replacable {
		private Pattern mPattern;

		public GeekUrlReplacer() {
			mPattern = Pattern.compile("\\[geekurl=(.*?)\\](.*?)\\[/geekurl\\]", Pattern.CASE_INSENSITIVE);
		}

		@Override
		public String replace(String text) {
			Matcher matcher = mPattern.matcher(text);
			StringBuffer result = new StringBuffer();
			while (matcher.find()) {
				String displayText = matcher.group(2);
				if (TextUtils.isEmpty(displayText)) {
					matcher.appendReplacement(result,
						"<a href=\"" + BASE_URL + matcher.group(1) + "\">" + matcher.group(1) + "</a>");
				} else {
					matcher.appendReplacement(result, "<a href=\"" + BASE_URL + matcher.group(1) + "\">" + displayText
						+ "</a>");
				}
			}
			matcher.appendTail(result);
			return result.toString();
		}
	}

	public static class GeekLinkReplacer implements Replacable {
		private Pattern mPattern;
		private String mUrl;
		private String mDisplayPrefix;

		static GeekLinkReplacer createAlpha(String path) {
			return new GeekLinkReplacer("\\[" + path + "=(.*?)\\](.*?)\\[/" + path + "\\]",
				BASE_URL + "/" + path + "/", path);
		}

		static GeekLinkReplacer createNumeric(String path) {
			return createNumeric(path, path);
		}

		static GeekLinkReplacer createNumeric(String path, String display) {
			return new GeekLinkReplacer("\\[" + path + "=(\\d+)\\](.*?)\\[/" + path + "\\]", BASE_URL + "/" + path
				+ "/", display);
		}

		public GeekLinkReplacer(String pattern, String url, String displayPrefix) {
			mPattern = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
			mUrl = url;
			mDisplayPrefix = displayPrefix;
		}

		@Override
		public String replace(String text) {
			Matcher matcher = mPattern.matcher(text);
			StringBuffer result = new StringBuffer();
			while (matcher.find()) {
				String displayText = matcher.group(2);
				if (TextUtils.isEmpty(displayText)) {
					matcher.appendReplacement(result, "<a href=\"" + mUrl + matcher.group(1) + "\">" + mDisplayPrefix
						+ " " + matcher.group(1) + "</a>");
				} else {
					matcher.appendReplacement(result, "<a href=\"" + mUrl + matcher.group(1) + "\">" + displayText
						+ "</a>");
				}
			}
			matcher.appendTail(result);
			return result.toString();
		}
	}

	public String toHtml(String text) {
		if (TextUtils.isEmpty(text)) {
			return "";
		}

		for (Replacable replacer : mReplacers) {
			text = replacer.replace(text);
		}
		text = "<div style=\"white-space: pre-wrap\">" + text + "</div>";

		return text;
	}
}
