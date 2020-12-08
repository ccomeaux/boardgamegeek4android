package com.boardgamegeek.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.text.TextUtils;

import com.boardgamegeek.R;
import com.boardgamegeek.extensions.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Converts XML returned from the BGG API into HTML.
 */
public class XmlApiMarkupConverter {
	private static final String BASE_URL = "https://boardgamegeek.com";
	private static final String STATIC_IMAGES_URL = "https://cf.geekdo-static.com/images/";
	private static final String IMAGES_URL = "https://cf.geekdo-images.com/images/";
	private final List<Replaceable> replacers;

	public XmlApiMarkupConverter(Context context) {
		replacers = new ArrayList<>();
		replacers.add(new SimpleReplacer("\\[hr\\]", "<hr/>"));
		replacers.add(new SimpleReplacer("\\[clear\\]", "<div style=\"clear:both\"></div>"));
		createPair("b");
		createPair("i");
		createPair("u");
		createPair("-", "strike");
		replacers.add(new SimpleReplacer("\\[floatleft\\]", "<div style=\"float:left\">"));
		replacers.add(new SimpleReplacer("\\[/floatleft\\]", "</div>"));
		createPair("center");
		replacers.add(new SimpleReplacer("\\[floatright\\]", "<div style=\"float:right\">"));
		replacers.add(new SimpleReplacer("\\[/floatright\\]", "</div>"));
		replacers.add(new Replacer("\\[COLOR=([^#].*?)\\]", "<span style=\"color:", "\">"));
		replacers.add(new Replacer("\\[COLOR=#(.*?)\\]", "<span style=\"color:#", "\">"));
		replacers.add(new SimpleReplacer("\\[/COLOR\\]", "</span>"));
		replacers.add(new Replacer("\\[BGCOLOR=([^#].*?)\\]", "<span style=\"background-color:", "\">"));
		replacers.add(new Replacer("\\[BGCOLOR=#(.*?)\\]", "<span style=\"background-color#:", "\">"));
		replacers.add(new SimpleReplacer("\\[/BGCOLOR\\]", "</span>"));
		// TODO: determine when image is a PNG
		replacers.add(new Replacer("\\[ImageID=(\\d+).*?\\]", "<div style=\"display:inline\"><img src=\"" + IMAGES_URL + "pic", "_t.jpg\"/></div>"));
		replacers.add(new Replacer("\\[IMG\\](.*?)\\[/IMG\\]", "<div style=\"display:inline\"><img src=\"", "\"/></div>"));
		// TODO: YouTube, Vimeo, tweet, mp3
		replacers.add(new GeekUrlReplacer());
		replacers.add(GeekLinkReplacer.createNumeric("thing"));
		replacers.add(GeekLinkReplacer.createNumeric("thread"));
		replacers.add(GeekLinkReplacer.createNumeric("article", "reply"));// TODO: add #id
		replacers.add(GeekLinkReplacer.createNumeric("geeklist", "GeekList"));
		replacers.add(GeekLinkReplacer.createNumeric("filepage", "file"));
		replacers.add(GeekLinkReplacer.createNumeric("person"));
		replacers.add(GeekLinkReplacer.createNumeric("company"));
		replacers.add(GeekLinkReplacer.createNumeric("property"));
		replacers.add(GeekLinkReplacer.createNumeric("family"));
		replacers.add(GeekLinkReplacer.createNumeric("guild"));
		replacers.add(GeekLinkReplacer.createAlpha("user"));
		replacers.add(GeekLinkReplacer.createNumeric("question", "GeekQuestion"));
		replacers.add(GeekLinkReplacer.createNumeric("media", "podcast episode"));
		replacers.add(GeekLinkReplacer.createNumeric("blog"));
		replacers.add(GeekLinkReplacer.createNumeric("blogpost", "blog post"));
		replacers.add(new SimpleReplacer("\\[q\\]", "Quote:<blockquote>"));
		replacers.add(new Replacer("\\[q=\"(.*?)\"\\]", "", " wrote:<blockquote>"));
		replacers.add(new SimpleReplacer("\\[/q\\]", "</blockquote>"));
		replacers.add(new SimpleReplacer("\\[o\\]", String.format("<details><summary>%s</summary>", context.getString(R.string.spoiler))));
		replacers.add(new SimpleReplacer("\\[/o\\]", "</details>"));
		createPair("c", "tt");
		replacers.add(new UrlReplacer());
		replacers.add(new UrlReplacer2());
		// TODO: roll
		// TODO: size isn't working
		replacers.add(new Replacer("\\[size=(.*?)\\]", "<span font-size=\"", "px\">"));
		replacers.add(new SimpleReplacer("\\[/size\\]", "</span>"));

		replacers.add(SimpleReplacer.createCustomImage("\\:\\)\\s", "smile.gif"));
		replacers.add(SimpleReplacer.createCustomImage("\\:\\(\\s", "sad.gif"));
		replacers.add(SimpleReplacer.createCustomImage("\\:D\\s", "biggrin.gif"));
		replacers.add(SimpleReplacer.createCustomImage("\\:p\\s", "tongue.gif"));
		replacers.add(SimpleReplacer.createCustomImage("\\;\\)\\s", "wink.gif"));
		replacers.add(SimpleReplacer.createImage("what", "rock.gif"));
		replacers.add(SimpleReplacer.createGif("wow"));
		replacers.add(SimpleReplacer.createGif("angry"));
		replacers.add(SimpleReplacer.createGif("cool"));
		replacers.add(SimpleReplacer.createGif("laugh"));
		replacers.add(SimpleReplacer.createGif("meeple"));
		replacers.add(SimpleReplacer.createGif("surprise"));
		replacers.add(SimpleReplacer.createGif("blush"));
		replacers.add(SimpleReplacer.createGif("snore"));
		replacers.add(SimpleReplacer.createGif("cry"));
		replacers.add(SimpleReplacer.createGif("kiss"));
		replacers.add(SimpleReplacer.createGif("modest"));
		replacers.add(SimpleReplacer.createGif("whistle"));
		replacers.add(SimpleReplacer.createGif("devil"));
		replacers.add(SimpleReplacer.createGif("soblue"));
		replacers.add(SimpleReplacer.createGif("yuk"));
		replacers.add(SimpleReplacer.createGif("gulp"));
		replacers.add(SimpleReplacer.createGif("shake"));
		replacers.add(SimpleReplacer.createGif("arrrh"));
		replacers.add(SimpleReplacer.createGif("zombie"));
		replacers.add(SimpleReplacer.createGif("robot"));
		replacers.add(SimpleReplacer.createGif("ninja"));
		replacers.add(SimpleReplacer.createGif("sauron"));
		replacers.add(SimpleReplacer.createGif("goo"));
		replacers.add(SimpleReplacer.createImage("star", "star_yellow.gif"));
		replacers.add(SimpleReplacer.createImage("halfstar", "star_yellowhalf.gif"));
		replacers.add(SimpleReplacer.createImage("nostar", "star_white.gif"));
		replacers.add(SimpleReplacer.createImage("gg", "geekgold.gif"));
		replacers.add(SimpleReplacer.createGif("bag"));
		replacers.add(SimpleReplacer.createGif("bacon"));
		replacers.add(SimpleReplacer.createGif("caravan"));
		createCamelImage("mint");
		createCamelImage("lime");
		createCamelImage("grape");
		createCamelImage("lemon");
		createCamelImage("orange");
		replacers.add(SimpleReplacer.createGif("goldencamel"));
		createTajImage("blue");
		createTajImage("brown");
		createTajImage("gray");
		createTajImage("maroon");
		createTajImage("tan");
		createTajImage("white");
		replacers.add(SimpleReplacer.createImage("thumbsup", "thumbs-up.gif"));
		replacers.add(SimpleReplacer.createImage("thumbsdown", "thumbs-down.gif"));
		replacers.add(SimpleReplacer.createGif("coffee"));
		replacers.add(SimpleReplacer.createGif("tobacco"));
		replacers.add(SimpleReplacer.createGif("indigo"));
		replacers.add(SimpleReplacer.createGif("sugar"));
		replacers.add(SimpleReplacer.createGif("corn"));
		replacers.add(SimpleReplacer.createGif("colonist"));
		replacers.add(SimpleReplacer.createGif("1vp"));
		replacers.add(SimpleReplacer.createGif("5vp"));
		replacers.add(SimpleReplacer.createGif("1db"));
		replacers.add(SimpleReplacer.createGif("5db"));
		for (int i = 0; i <= 9; i++) {
			replacers.add(SimpleReplacer.createImage("d10-" + i, "d10-" + i + ".gif"));
		}
		replacers.add(SimpleReplacer.createImage("city", "ttr_city.gif"));
		createTrainImage("red");
		createTrainImage("green");
		createTrainImage("blue");
		createTrainImage("yellow");
		createTrainImage("black");
		createTrainImage("purple");
		createTrainImage("white");
		replacers.add(SimpleReplacer.createGif("wood"));
		replacers.add(SimpleReplacer.createGif("wheat"));
		replacers.add(SimpleReplacer.createGif("sheep"));
		replacers.add(SimpleReplacer.createGif("ore"));
		replacers.add(SimpleReplacer.createGif("brick"));
		replacers.add(SimpleReplacer.createGif("cinnamon"));
		replacers.add(SimpleReplacer.createGif("clove"));
		replacers.add(SimpleReplacer.createGif("ginger"));
		replacers.add(SimpleReplacer.createGif("nutmeg"));
		replacers.add(SimpleReplacer.createGif("pepper"));
		replacers.add(SimpleReplacer.createGif("coal"));
		replacers.add(SimpleReplacer.createGif("oil"));
		replacers.add(SimpleReplacer.createGif("trash"));
		replacers.add(SimpleReplacer.createGif("nuclear"));
		for (int i = 1; i <= 6; i++) {
			replacers.add(SimpleReplacer.createImage("d6-" + i, "die-white-" + i + ".gif"));
			replacers.add(SimpleReplacer.createImage("bd6-" + i, "die-black-" + i + ".gif"));
		}
		replacers.add(SimpleReplacer.createPng("tankard"));
		replacers.add(SimpleReplacer.createPng("jug"));
		replacers.add(SimpleReplacer.createPng("chalice"));
		replacers.add(SimpleReplacer.createGif("worker"));
		replacers.add(SimpleReplacer.createGif("building"));
		replacers.add(SimpleReplacer.createGif("aristocrat"));
		replacers.add(SimpleReplacer.createGif("trade"));
		replacers.add(SimpleReplacer.createPng("arrowN"));
		replacers.add(SimpleReplacer.createPng("arrowNE"));
		replacers.add(SimpleReplacer.createPng("arrowE"));
		replacers.add(SimpleReplacer.createPng("arrowSE"));
		replacers.add(SimpleReplacer.createPng("arrowS"));
		replacers.add(SimpleReplacer.createPng("arrowSW"));
		replacers.add(SimpleReplacer.createPng("arrowW"));
		replacers.add(SimpleReplacer.createPng("arrowNW"));
		replacers.add(SimpleReplacer.createPng("power"));
		replacers.add(SimpleReplacer.createPng("XBA"));
		replacers.add(SimpleReplacer.createPng("XBB"));
		replacers.add(SimpleReplacer.createPng("XBX"));
		replacers.add(SimpleReplacer.createPng("XBY"));
		replacers.add(SimpleReplacer.createPng("PSC"));
		replacers.add(SimpleReplacer.createPng("PSS"));
		replacers.add(SimpleReplacer.createPng("PST"));
		replacers.add(SimpleReplacer.createPng("PSX"));
		replacers.add(SimpleReplacer.createPng("WiiH"));
		replacers.add(SimpleReplacer.createPng("Wii1"));
		replacers.add(SimpleReplacer.createPng("Wii2"));
		replacers.add(SimpleReplacer.createPng("WiiA"));
		replacers.add(SimpleReplacer.createPng("WiiB"));
		replacers.add(SimpleReplacer.createPng("WiiC"));
		replacers.add(SimpleReplacer.createPng("WiiX"));
		replacers.add(SimpleReplacer.createPng("WiiY"));
		replacers.add(SimpleReplacer.createPng("WiiZ"));
		replacers.add(SimpleReplacer.createPng("Wii\\+"));
		replacers.add(SimpleReplacer.createPng("Wii-"));
		replacers.add(SimpleReplacer.createImage("!block", "bang_block.png"));
		replacers.add(SimpleReplacer.createImage("\\?block", "question_block.png"));
		replacers.add(SimpleReplacer.createImage("blank", "tiles/BLANK.gif"));
		replacers.add(new UpperCaseReplacer("\\:([A-Za-z])\\:", "<img src=\"" + STATIC_IMAGES_URL + "tiles/", ".gif\"/>"));
		replacers.add(new Replacer("\\:k([A-Za-z])\\:", "<img src=\"" + STATIC_IMAGES_URL + "k", ".png\"/>"));
	}

	private void createPair(String tag) {
		replacers.add(SimpleReplacer.createStart(tag));
		replacers.add(SimpleReplacer.createEnd(tag));
	}

	private void createPair(String tag, String replacementTag) {
		replacers.add(SimpleReplacer.createStart(tag, replacementTag));
		replacers.add(SimpleReplacer.createEnd(tag, replacementTag));
	}

	private void createCamelImage(String color) {
		replacers.add(SimpleReplacer.createImage(color + "camel", "camel_" + color + ".gif"));
	}

	private void createTajImage(String color) {
		replacers.add(SimpleReplacer.createImage(color + "taj", "taj_" + color + ".gif"));
	}

	private void createTrainImage(String color) {
		replacers.add(SimpleReplacer.createImage(color + "train", "ttr_" + color + ".gif"));
	}

	public String toHtml(String text) {
		if (TextUtils.isEmpty(text)) {
			return "";
		}

		for (Replaceable replacer : replacers) {
			text = replacer.replace(text);
		}
		text = "<div style=\"white-space: pre-wrap\">" + text + "</div>";

		return text;
	}

	public String strip(String text) {
		if (TextUtils.isEmpty(text)) {
			return "";
		}

		for (Replaceable replacer : replacers) {
			text = replacer.strip(text);
		}

		return text;
	}

	interface Replaceable {
		String replace(String text);

		String strip(String text);
	}

	private static class SimpleReplacer implements Replaceable {
		final Pattern pattern;
		final String replacement;

		public SimpleReplacer(String pattern, String replacement) {
			this.pattern = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
			this.replacement = replacement;
		}

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

		@Override
		public String replace(String text) {
			Matcher matcher = pattern.matcher(text);
			if (matcher.find()) {
				return matcher.replaceAll(replacement);
			}
			return text;
		}

		@Override
		public String strip(String text) {
			return pattern.matcher(text).replaceAll("");
		}
	}

	private static class Replacer implements Replaceable {
		private final Pattern pattern;
		private final String prefix;
		private final String suffix;

		public Replacer(String pattern, String prefix, String suffix) {
			this.pattern = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
			this.prefix = prefix;
			this.suffix = suffix;
		}

		@Override
		public String replace(String text) {
			Matcher matcher = pattern.matcher(text);
			StringBuffer buffer = new StringBuffer();
			while (matcher.find()) {
				matcher.appendReplacement(buffer, prefix + matcher.group(1) + suffix);
			}
			matcher.appendTail(buffer);
			return buffer.toString();
		}

		@Override
		public String strip(String text) {
			Matcher matcher = pattern.matcher(text);
			StringBuffer buffer = new StringBuffer();
			while (matcher.find()) {
				matcher.appendReplacement(buffer, matcher.group(1));
			}
			matcher.appendTail(buffer);
			return buffer.toString();
		}
	}

	private static class UpperCaseReplacer implements Replaceable {
		private final Pattern pattern;
		private final String prefix;
		private final String suffix;

		public UpperCaseReplacer(String pattern, String prefix, String suffix) {
			this.pattern = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
			this.prefix = prefix;
			this.suffix = suffix;
		}

		@SuppressLint("DefaultLocale")
		@Override
		public String replace(String text) {
			Matcher matcher = pattern.matcher(text);
			StringBuffer buffer = new StringBuffer();
			while (matcher.find()) {
				matcher.appendReplacement(buffer, prefix + matcher.group(1).toUpperCase() + suffix);
			}
			matcher.appendTail(buffer);
			return buffer.toString();
		}

		@Override
		public String strip(String text) {
			Matcher matcher = pattern.matcher(text);
			StringBuffer buffer = new StringBuffer();
			while (matcher.find()) {
				matcher.appendReplacement(buffer, matcher.group(1).toUpperCase());
			}
			matcher.appendTail(buffer);
			return buffer.toString();
		}
	}

	public static class UrlReplacer implements Replaceable {
		private final Pattern pattern;

		public UrlReplacer() {
			pattern = Pattern.compile("\\[url](.*?)\\[/url]", Pattern.CASE_INSENSITIVE);
		}

		@Override
		public String replace(String text) {
			Matcher matcher = pattern.matcher(text);
			StringBuffer result = new StringBuffer();
			while (matcher.find()) {
				String url = StringUtils.ensureHttpsScheme(matcher.group(1));
				matcher.appendReplacement(result, "<a href=\"" + url + "\">" + url + "</a>");
			}
			matcher.appendTail(result);
			return result.toString();
		}

		@Override
		public String strip(String text) {
			Matcher matcher = pattern.matcher(text);
			StringBuffer result = new StringBuffer();
			while (matcher.find()) {
				matcher.appendReplacement(result, StringUtils.ensureHttpsScheme(matcher.group(1)));
			}
			matcher.appendTail(result);
			return result.toString();
		}
	}

	public static class UrlReplacer2 implements Replaceable {
		private final Pattern pattern;

		public UrlReplacer2() {
			pattern = Pattern.compile("\\[url=(.*?)](.*?)\\[/url]", Pattern.CASE_INSENSITIVE);
		}

		@Override
		public String replace(String text) {
			Matcher matcher = pattern.matcher(text);
			StringBuffer result = new StringBuffer();
			while (matcher.find()) {
				String url = StringUtils.ensureHttpsScheme(matcher.group(1));
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

		@Override
		public String strip(String text) {
			Matcher matcher = pattern.matcher(text);
			StringBuffer result = new StringBuffer();
			while (matcher.find()) {
				String displayText = matcher.group(2);
				if (TextUtils.isEmpty(displayText)) {
					matcher.appendReplacement(result, StringUtils.ensureHttpsScheme(matcher.group(1)));
				} else {
					matcher.appendReplacement(result, displayText);
				}
			}
			matcher.appendTail(result);
			return result.toString();
		}
	}

	public static class GeekUrlReplacer implements Replaceable {
		private final Pattern pattern;

		public GeekUrlReplacer() {
			pattern = Pattern.compile("\\[geekurl=(.*?)](.*?)\\[/geekurl]", Pattern.CASE_INSENSITIVE);
		}

		@Override
		public String replace(String text) {
			Matcher matcher = pattern.matcher(text);
			StringBuffer result = new StringBuffer();
			while (matcher.find()) {
				String displayText = matcher.group(2);
				if (TextUtils.isEmpty(displayText)) {
					matcher.appendReplacement(result, "<a href=\"" + BASE_URL + matcher.group(1) + "\">" + matcher.group(1) + "</a>");
				} else {
					matcher.appendReplacement(result, "<a href=\"" + BASE_URL + matcher.group(1) + "\">" + displayText + "</a>");
				}
			}
			matcher.appendTail(result);
			return result.toString();
		}

		@Override
		public String strip(String text) {
			Matcher matcher = pattern.matcher(text);
			StringBuffer result = new StringBuffer();
			while (matcher.find()) {
				String displayText = matcher.group(2);
				if (TextUtils.isEmpty(displayText)) {
					matcher.appendReplacement(result, matcher.group(1));
				} else {
					matcher.appendReplacement(result, displayText);
				}
			}
			matcher.appendTail(result);
			return result.toString();
		}
	}

	/***
	 * Replaces a GeekLink with an HREF tag.
	 * [thing=13]Catan[/thing] becomes <a href="https://boardgamegeek.com/thing/13">Catan</a>
	 * [thing=13][/thing] becomes <a href="https://boardgamegeek.com/thing/13">thing 13</a>
	 */
	public static class GeekLinkReplacer implements Replaceable {
		private final Pattern pattern;
		private final String url;
		private final String displayPrefix;

		private GeekLinkReplacer(String pattern, String url, String displayPrefix) {
			this.pattern = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
			this.url = url;
			this.displayPrefix = displayPrefix;
		}

		static GeekLinkReplacer createAlpha(String path) {
			return new GeekLinkReplacer("\\[" + path + "=(.*?)\\](.*?)\\[/" + path + "\\]", BASE_URL + "/" + path + "/", path);
		}

		static GeekLinkReplacer createNumeric(String path) {
			return createNumeric(path, path);
		}

		static GeekLinkReplacer createNumeric(String path, String display) {
			return new GeekLinkReplacer("\\[" + path + "=(\\d+)\\](.*?)\\[/" + path + "\\]", BASE_URL + "/" + path + "/", display);
		}

		@Override
		public String replace(String text) {
			Matcher matcher = pattern.matcher(text);
			StringBuffer result = new StringBuffer();
			while (matcher.find()) {
				String displayText = matcher.group(2);
				if (TextUtils.isEmpty(displayText)) {
					matcher.appendReplacement(result, "<a href=\"" + url + matcher.group(1) + "\">" + displayPrefix + " " + matcher.group(1) + "</a>");
				} else {
					matcher.appendReplacement(result, "<a href=\"" + url + matcher.group(1) + "\">" + displayText + "</a>");
				}
			}
			matcher.appendTail(result);
			return result.toString();
		}

		@Override
		public String strip(String text) {
			Matcher matcher = pattern.matcher(text);
			StringBuffer result = new StringBuffer();
			while (matcher.find()) {
				String displayText = matcher.group(2);
				if (TextUtils.isEmpty(displayText)) {
					matcher.appendReplacement(result, displayPrefix + " " + matcher.group(1));
				} else {
					matcher.appendReplacement(result, displayText);
				}
			}
			matcher.appendTail(result);
			return result.toString();
		}
	}
}
