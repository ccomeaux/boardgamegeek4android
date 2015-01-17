/**
 * Adapted from: https://bitbucket.org/Kuitsi/android-textview-html-list/overview
 */
package com.boardgamegeek.util;

import android.text.Editable;
import android.text.Html;
import android.text.Spanned;
import android.text.style.BulletSpan;
import android.text.style.LeadingMarginSpan;

import org.xml.sax.XMLReader;

import java.util.Stack;

import timber.log.Timber;

public class ListTagHandler implements Html.TagHandler {
	private static final String UL = "ul";
	private static final String OL = "ol";
	private static final String LI = "li";

	/**
	 * Keeps track of lists (ol, ul). On bottom of Stack is the outermost list and on top of Stack is the most nested
	 * list
	 */
	private Stack<String> mLists = new Stack<String>();
	/**
	 * Tracks indexes of ordered lists so that after a nested list ends we can continue with correct index of outer list
	 */
	private Stack<Integer> mNextOrderedIndex = new Stack<Integer>();
	/**
	 * List indentation in pixels. Nested lists use multiple of this.
	 */
	private static final int INDENTATION_IN_PIXELS = 10;
	private static final int LIST_ITEM_INDENTATION_IN_PIXELS = INDENTATION_IN_PIXELS * 2;
	private static final BulletSpan BULLET_SPAN = new BulletSpan(INDENTATION_IN_PIXELS);

	@Override
	public void handleTag(boolean opening, String tag, Editable output, XMLReader xmlReader) {
		if (tagIsTypeOf(tag, UL)) {
			if (opening) {
				mLists.push(tag);
			} else {
				mLists.pop();
			}
		} else if (tagIsTypeOf(tag, OL)) {
			if (opening) {
				mLists.push(tag);
				mNextOrderedIndex.push(Integer.valueOf(1)).toString();
			} else {
				mLists.pop();
				mNextOrderedIndex.pop().toString();
			}
		} else if (tagIsTypeOf(tag, LI)) {
			String currentListTag = mLists.peek();
			ensureTrailingNewLine(output);
			if (opening) {
				if (tagIsTypeOf(currentListTag, UL)) {
					startListItem(output, new Ul());
				} else if (tagIsTypeOf(currentListTag, OL)) {
					startListItem(output, new Ol());
					output.append(mNextOrderedIndex.peek().toString()).append(". ");
					mNextOrderedIndex.push(Integer.valueOf(mNextOrderedIndex.pop().intValue() + 1));
				}
			} else {
				if (tagIsTypeOf(currentListTag, UL)) {
					// Nested BulletSpans increases distance between bullet and text, so we must prevent it.
					int margin = INDENTATION_IN_PIXELS;
					if (mLists.size() > 1) {
						margin = INDENTATION_IN_PIXELS - BULLET_SPAN.getLeadingMargin(true);
						if (mLists.size() > 2) {
							// This get's more complicated when we add a LeadingMarginSpan into the same line:
							// we have also counter it's effect to BulletSpan
							margin -= (mLists.size() - 2) * LIST_ITEM_INDENTATION_IN_PIXELS;
						}
					}
					BulletSpan newBullet = new BulletSpan(margin);
					endListItem(output, Ul.class, new LeadingMarginSpan.Standard(LIST_ITEM_INDENTATION_IN_PIXELS
						* (mLists.size() - 1)), newBullet);
				} else if (tagIsTypeOf(currentListTag, OL)) {
					int margin = LIST_ITEM_INDENTATION_IN_PIXELS * (mLists.size() - 1);
					if (mLists.size() > 2) {
						// Same as in ordered lists: counter the effect of nested Spans
						margin -= (mLists.size() - 2) * LIST_ITEM_INDENTATION_IN_PIXELS;
					}
					endListItem(output, Ol.class, new LeadingMarginSpan.Standard(margin));
				}
			}
		} else {
			if (opening) {
				Timber.d("Found an unsupported tag: " + tag);
			}
		}
	}

	private boolean tagIsTypeOf(String tag, String type) {
		return tag.equalsIgnoreCase(type);
	}

	private static void startListItem(Editable text, Object type) {
		int length = text.length();
		text.setSpan(type, length, length, Spanned.SPAN_MARK_MARK);
	}

	private static void endListItem(Editable text, Class<?> kind, Object... replaces) {
		int length = text.length();
		Object lastSpan = getLastSpan(text, kind);
		int where = text.getSpanStart(lastSpan);
		text.removeSpan(lastSpan);
		if (where != length) {
			for (Object replace : replaces) {
				text.setSpan(replace, where, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
			}
		}
	}

	private static Object getLastSpan(Spanned text, Class<?> kind) {
		/*
		 * This knows that the last returned object from getSpans() will be the most recently added.
		 */
		Object[] spans = text.getSpans(0, text.length(), kind);
		if (spans.length == 0) {
			return null;
		}
		return spans[spans.length - 1];
	}

	private void ensureTrailingNewLine(Editable line) {
		if (line.length() > 0 && line.charAt(line.length() - 1) != '\n') {
			line.append("\n");
		}
	}

	private static class Ul {
	}

	private static class Ol {
	}
}
