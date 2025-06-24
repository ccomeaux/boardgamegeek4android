@file:Suppress("SpellCheckingInspection")

package com.boardgamegeek.util

import android.content.Context
import android.text.TextUtils
import com.boardgamegeek.R
import com.boardgamegeek.extensions.ensureHttpsScheme
import java.util.*
import java.util.regex.Pattern

/**
 * Converts XML returned from the BGG API into HTML.
 */
class XmlApiMarkupConverter(context: Context) {
    private var replacerList = mutableListOf<Replaceable>()

    private fun createPair(tag: String) {
        replacerList.add(SimpleReplacer.createStart(tag))
        replacerList.add(SimpleReplacer.createEnd(tag))
    }

    private fun createPair(tag: String, replacementTag: String) {
        replacerList.add(SimpleReplacer.createStart(tag, replacementTag))
        replacerList.add(SimpleReplacer.createEnd(tag, replacementTag))
    }

    private fun createCamelImage(color: String) {
        replacerList.add(SimpleReplacer.createImage("${color}camel", "camel_$color.gif"))
    }

    private fun createTajImage(color: String) {
        replacerList.add(SimpleReplacer.createImage("${color}taj", "taj_$color.gif"))
    }

    private fun createTrainImage(color: String) {
        replacerList.add(SimpleReplacer.createImage("${color}train", "ttr_$color.gif"))
    }

    fun toHtml(text: String): String {
        if (text.isEmpty()) return ""
        var replacedText = text
        for (replacer in replacerList) {
            replacedText = replacer.replace(replacedText)
        }
        return "<div style=\"white-space: pre-wrap\">$replacedText</div>"
    }

    fun strip(text: String): String {
        if (text.isEmpty()) return ""
        var strippedText = text
        for (replacer in replacerList) {
            strippedText = replacer.strip(strippedText)
        }
        return strippedText
    }

    internal interface Replaceable {
        fun replace(text: String): String
        fun strip(text: String): String
    }

    private class SimpleReplacer(regex: String, val replacement: String) : Replaceable {
        private val pattern: Pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE)

        override fun replace(text: String): String {
            val matcher = pattern.matcher(text)
            return if (matcher.find()) {
                matcher.replaceAll(replacement)
            } else text
        }

        override fun strip(text: String): String {
            return pattern.matcher(text).replaceAll("")
        }

        companion object {
            fun createStart(tag: String): SimpleReplacer {
                return SimpleReplacer("\\[$tag\\]", "<$tag>")
            }

            fun createStart(tag: String, replacementTag: String): SimpleReplacer {
                return SimpleReplacer("\\[$tag\\]", "<$replacementTag>")
            }

            fun createEnd(tag: String): SimpleReplacer {
                return SimpleReplacer("\\[/$tag\\]", "</$tag>")
            }

            fun createEnd(tag: String, replacementTag: String): SimpleReplacer {
                return SimpleReplacer("\\[/$tag\\]", "</$replacementTag>")
            }

            fun createGif(image: String): SimpleReplacer {
                return SimpleReplacer("\\:$image\\:", "<img src=\"$STATIC_IMAGES_URL$image.gif\"/>")
            }

            fun createPng(image: String): SimpleReplacer {
                return SimpleReplacer("\\:$image\\:", "<img src=\"$STATIC_IMAGES_URL$image.png\"/>")
            }

            fun createImage(image: String, imageFile: String): SimpleReplacer {
                return SimpleReplacer("\\:$image\\:", "<img src=\"$STATIC_IMAGES_URL$imageFile\"/>")
            }

            fun createCustomImage(image: String, imageFile: String): SimpleReplacer {
                return SimpleReplacer(image, "<img src=\"$STATIC_IMAGES_URL$imageFile\"/>")
            }
        }
    }

    private class Replacer(regex: String, val prefix: String, val suffix: String) : Replaceable {
        private val pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE)

        override fun replace(text: String): String {
            val matcher = pattern.matcher(text)
            val buffer = StringBuffer()
            while (matcher.find()) {
                matcher.appendReplacement(buffer, "$prefix${matcher.group(1)}$suffix")
            }
            matcher.appendTail(buffer)
            return buffer.toString()
        }

        override fun strip(text: String): String {
            val matcher = pattern.matcher(text)
            val buffer = StringBuffer()
            while (matcher.find()) {
                matcher.appendReplacement(buffer, matcher.group(1).orEmpty())
            }
            matcher.appendTail(buffer)
            return buffer.toString()
        }

    }

    private class UpperCaseReplacer(regex: String, val prefix: String, val suffix: String) : Replaceable {
        private val pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE)

        override fun replace(text: String): String {
            val matcher = pattern.matcher(text)
            val buffer = StringBuffer()
            while (matcher.find()) {
                matcher.appendReplacement(buffer, "$prefix${matcher.group(1).orEmpty().uppercase(Locale.getDefault())}$suffix")
            }
            matcher.appendTail(buffer)
            return buffer.toString()
        }

        override fun strip(text: String): String {
            val matcher = pattern.matcher(text)
            val buffer = StringBuffer()
            while (matcher.find()) {
                matcher.appendReplacement(buffer, matcher.group(1).orEmpty().uppercase(Locale.getDefault()))
            }
            matcher.appendTail(buffer)
            return buffer.toString()
        }
    }

    class UrlReplacer : Replaceable {
        private val pattern = Pattern.compile("\\[url](.*?)\\[/url]", Pattern.CASE_INSENSITIVE)

        override fun replace(text: String): String {
            val matcher = pattern.matcher(text)
            val result = StringBuffer()
            while (matcher.find()) {
                val url = matcher.group(1).ensureHttpsScheme()
                matcher.appendReplacement(result, "<a href=\"$url\">$url</a>")
            }
            matcher.appendTail(result)
            return result.toString()
        }

        override fun strip(text: String): String {
            val matcher = pattern.matcher(text)
            val result = StringBuffer()
            while (matcher.find()) {
                matcher.appendReplacement(result, matcher.group(1).ensureHttpsScheme().orEmpty())
            }
            matcher.appendTail(result)
            return result.toString()
        }
    }

    class UrlReplacer2 : Replaceable {
        private val pattern = Pattern.compile("\\[url=(.*?)](.*?)\\[/url]", Pattern.CASE_INSENSITIVE)

        override fun replace(text: String): String {
            val matcher = pattern.matcher(text)
            val result = StringBuffer()
            while (matcher.find()) {
                val url = matcher.group(1).ensureHttpsScheme()
                val displayText = matcher.group(2)
                if (TextUtils.isEmpty(displayText)) {
                    matcher.appendReplacement(result, "<a href=\"$url\">$url</a>")
                } else {
                    matcher.appendReplacement(result, "<a href=\"$url\">$displayText</a>")
                }
            }
            matcher.appendTail(result)
            return result.toString()
        }

        override fun strip(text: String): String {
            val matcher = pattern.matcher(text)
            val result = StringBuffer()
            while (matcher.find()) {
                val displayText = matcher.group(2)
                if (displayText == null || displayText.isBlank()) {
                    matcher.appendReplacement(result, matcher.group(1).ensureHttpsScheme().orEmpty())
                } else {
                    matcher.appendReplacement(result, displayText)
                }
            }
            matcher.appendTail(result)
            return result.toString()
        }
    }

    class GeekUrlReplacer : Replaceable {
        private val pattern = Pattern.compile("\\[geekurl=(.*?)](.*?)\\[/geekurl]", Pattern.CASE_INSENSITIVE)

        override fun replace(text: String): String {
            val matcher = pattern.matcher(text)
            val result = StringBuffer()
            while (matcher.find()) {
                val displayText = matcher.group(2)
                if (TextUtils.isEmpty(displayText)) {
                    matcher.appendReplacement(result, "<a href=\"$BASE_URL${matcher.group(1)}\">${matcher.group(1)}</a>")
                } else {
                    matcher.appendReplacement(result, "<a href=\"$BASE_URL${matcher.group(1)}\">$displayText</a>")
                }
            }
            matcher.appendTail(result)
            return result.toString()
        }

        override fun strip(text: String): String {
            val matcher = pattern.matcher(text)
            val result = StringBuffer()
            while (matcher.find()) {
                val displayText = matcher.group(2)
                if (displayText == null || displayText.isEmpty()) {
                    matcher.appendReplacement(result, matcher.group(1).orEmpty())
                } else {
                    matcher.appendReplacement(result, displayText)
                }
            }
            matcher.appendTail(result)
            return result.toString()
        }
    }

    /***
     * Replaces a GeekLink with an HREF tag.
     * [thing=13]Catan[/thing] becomes [Catan](https://boardgamegeek.com/thing/13)
     * [thing=13][/thing] becomes [thing 13](https://boardgamegeek.com/thing/13)
     */
    class GeekLinkReplacer private constructor(pattern: String, val url: String, private val displayPrefix: String) : Replaceable {
        private val pattern = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE)

        override fun replace(text: String): String {
            val matcher = pattern.matcher(text)
            val result = StringBuffer()
            while (matcher.find()) {
                val displayText = matcher.group(2)
                if (displayText == null || displayText.isBlank()) {
                    matcher.appendReplacement(result, "<a href=\"$url${matcher.group(1)}\">$displayPrefix ${matcher.group(1)}</a>")
                } else {
                    matcher.appendReplacement(result, "<a href=\"$url${matcher.group(1)}\">$displayText</a>")
                }
            }
            matcher.appendTail(result)
            return result.toString()
        }

        override fun strip(text: String): String {
            val matcher = pattern.matcher(text)
            val result = StringBuffer()
            while (matcher.find()) {
                val displayText = matcher.group(2)
                if (displayText == null || displayText.isBlank()) {
                    matcher.appendReplacement(result, "$displayPrefix ${matcher.group(1)}")
                } else {
                    matcher.appendReplacement(result, displayText)
                }
            }
            matcher.appendTail(result)
            return result.toString()
        }

        companion object {
            fun createAlpha(path: String): GeekLinkReplacer {
                return GeekLinkReplacer("\\[$path=(.*?)\\](.*?)\\[/$path\\]", "$BASE_URL/$path/", path)
            }

            fun createNumeric(path: String, display: String = path): GeekLinkReplacer {
                return GeekLinkReplacer("\\[$path=(\\d+)\\](.*?)\\[/$path\\]", "$BASE_URL/$path/", display)
            }
        }
    }

    companion object {
        private const val BASE_URL = "https://boardgamegeek.com"
        private const val STATIC_IMAGES_URL = "https://cf.geekdo-static.com/images/"
        private const val IMAGES_URL = "https://cf.geekdo-images.com/images/"
    }

    init {
        replacerList = ArrayList()
        replacerList.add(SimpleReplacer("\\n", "<br/>"))
        replacerList.add(SimpleReplacer("\\[hr\\]", "<hr/>"))
        replacerList.add(SimpleReplacer("\\[clear\\]", "<div style=\"clear:both\"></div>"))
        createPair("b")
        createPair("i")
        createPair("u")
        createPair("-", "strike")
        replacerList.add(SimpleReplacer("\\[floatleft\\]", "<div style=\"float:left\">"))
        replacerList.add(SimpleReplacer("\\[/floatleft\\]", "</div>"))
        createPair("center")
        replacerList.add(SimpleReplacer("\\[floatright\\]", "<div style=\"float:right\">"))
        replacerList.add(SimpleReplacer("\\[/floatright\\]", "</div>"))
        replacerList.add(Replacer("\\[COLOR=([^#].*?)\\]", "<span style=\"color:", "\">"))
        replacerList.add(Replacer("\\[COLOR=#(.*?)\\]", "<span style=\"color:#", "\">"))
        replacerList.add(SimpleReplacer("\\[/COLOR\\]", "</span>"))
        replacerList.add(Replacer("\\[BGCOLOR=([^#].*?)\\]", "<span style=\"background-color:", "\">"))
        replacerList.add(Replacer("\\[BGCOLOR=#(.*?)\\]", "<span style=\"background-color#:", "\">"))
        replacerList.add(SimpleReplacer("\\[/BGCOLOR\\]", "</span>"))
        // TODO: determine when image is a PNG
        replacerList.add(Replacer("\\[ImageID=(\\d+).*?\\]", "<div style=\"display:inline\"><img src=\"${IMAGES_URL}pic", "_t.jpg\"/></div>"))
        replacerList.add(Replacer("\\[IMG\\](.*?)\\[/IMG\\]", "<div style=\"display:inline\"><img src=\"", "\"/></div>"))
        // TODO: YouTube, Vimeo, tweet, mp3
        replacerList.add(GeekUrlReplacer())
        replacerList.add(GeekLinkReplacer.createNumeric("thing"))
        replacerList.add(GeekLinkReplacer.createNumeric("thread"))
        replacerList.add(GeekLinkReplacer.createNumeric("article", "reply")) // TODO: add #id
        replacerList.add(GeekLinkReplacer.createNumeric("geeklist", "GeekList"))
        replacerList.add(GeekLinkReplacer.createNumeric("filepage", "file"))
        replacerList.add(GeekLinkReplacer.createNumeric("person"))
        replacerList.add(GeekLinkReplacer.createNumeric("company"))
        replacerList.add(GeekLinkReplacer.createNumeric("property"))
        replacerList.add(GeekLinkReplacer.createNumeric("family"))
        replacerList.add(GeekLinkReplacer.createNumeric("guild"))
        replacerList.add(GeekLinkReplacer.createAlpha("user"))
        replacerList.add(GeekLinkReplacer.createNumeric("question", "GeekQuestion"))
        replacerList.add(GeekLinkReplacer.createNumeric("media", "podcast episode"))
        replacerList.add(GeekLinkReplacer.createNumeric("blog"))
        replacerList.add(GeekLinkReplacer.createNumeric("blogpost", "blog post"))
        replacerList.add(SimpleReplacer("\\[q\\]", "Quote:<blockquote>"))
        replacerList.add(Replacer("\\[q=\"(.*?)\"\\]", "", " wrote:<blockquote>"))
        replacerList.add(SimpleReplacer("\\[/q\\]", "</blockquote>"))
        replacerList.add(SimpleReplacer("\\[o\\]", String.format("<details><summary>%s</summary>", context.getString(R.string.spoiler))))
        replacerList.add(SimpleReplacer("\\[/o\\]", "</details>"))
        createPair("c", "tt")
        replacerList.add(UrlReplacer())
        replacerList.add(UrlReplacer2())
        // TODO: roll
        // TODO: size isn't working
        replacerList.add(Replacer("\\[size=(.*?)\\]", "<span font-size=\"", "px\">"))
        replacerList.add(SimpleReplacer("\\[/size\\]", "</span>"))
        replacerList.add(SimpleReplacer.createCustomImage("\\:\\)\\s", "smile.gif"))
        replacerList.add(SimpleReplacer.createCustomImage("\\:\\(\\s", "sad.gif"))
        replacerList.add(SimpleReplacer.createCustomImage("\\:D\\s", "biggrin.gif"))
        replacerList.add(SimpleReplacer.createCustomImage("\\:p\\s", "tongue.gif"))
        replacerList.add(SimpleReplacer.createCustomImage("\\;\\)\\s", "wink.gif"))
        replacerList.add(SimpleReplacer.createImage("what", "rock.gif"))
        replacerList.add(SimpleReplacer.createGif("wow"))
        replacerList.add(SimpleReplacer.createGif("angry"))
        replacerList.add(SimpleReplacer.createGif("cool"))
        replacerList.add(SimpleReplacer.createGif("laugh"))
        replacerList.add(SimpleReplacer.createGif("meeple"))
        replacerList.add(SimpleReplacer.createGif("surprise"))
        replacerList.add(SimpleReplacer.createGif("blush"))
        replacerList.add(SimpleReplacer.createGif("snore"))
        replacerList.add(SimpleReplacer.createGif("cry"))
        replacerList.add(SimpleReplacer.createGif("kiss"))
        replacerList.add(SimpleReplacer.createGif("modest"))
        replacerList.add(SimpleReplacer.createGif("whistle"))
        replacerList.add(SimpleReplacer.createGif("devil"))
        replacerList.add(SimpleReplacer.createGif("soblue"))
        replacerList.add(SimpleReplacer.createGif("yuk"))
        replacerList.add(SimpleReplacer.createGif("gulp"))
        replacerList.add(SimpleReplacer.createGif("shake"))
        replacerList.add(SimpleReplacer.createGif("arrrh"))
        replacerList.add(SimpleReplacer.createGif("zombie"))
        replacerList.add(SimpleReplacer.createGif("robot"))
        replacerList.add(SimpleReplacer.createGif("ninja"))
        replacerList.add(SimpleReplacer.createGif("sauron"))
        replacerList.add(SimpleReplacer.createGif("goo"))
        replacerList.add(SimpleReplacer.createImage("star", "star_yellow.gif"))
        replacerList.add(SimpleReplacer.createImage("halfstar", "star_yellowhalf.gif"))
        replacerList.add(SimpleReplacer.createImage("nostar", "star_white.gif"))
        replacerList.add(SimpleReplacer.createImage("gg", "geekgold.gif"))
        replacerList.add(SimpleReplacer.createGif("bag"))
        replacerList.add(SimpleReplacer.createGif("bacon"))
        replacerList.add(SimpleReplacer.createGif("caravan"))
        createCamelImage("mint")
        createCamelImage("lime")
        createCamelImage("grape")
        createCamelImage("lemon")
        createCamelImage("orange")
        replacerList.add(SimpleReplacer.createGif("goldencamel"))
        createTajImage("blue")
        createTajImage("brown")
        createTajImage("gray")
        createTajImage("maroon")
        createTajImage("tan")
        createTajImage("white")
        replacerList.add(SimpleReplacer.createImage("thumbsup", "thumbs-up.gif"))
        replacerList.add(SimpleReplacer.createImage("thumbsdown", "thumbs-down.gif"))
        replacerList.add(SimpleReplacer.createGif("coffee"))
        replacerList.add(SimpleReplacer.createGif("tobacco"))
        replacerList.add(SimpleReplacer.createGif("indigo"))
        replacerList.add(SimpleReplacer.createGif("sugar"))
        replacerList.add(SimpleReplacer.createGif("corn"))
        replacerList.add(SimpleReplacer.createGif("colonist"))
        replacerList.add(SimpleReplacer.createGif("1vp"))
        replacerList.add(SimpleReplacer.createGif("5vp"))
        replacerList.add(SimpleReplacer.createGif("1db"))
        replacerList.add(SimpleReplacer.createGif("5db"))
        for (i in 0..9) {
            replacerList.add(SimpleReplacer.createImage("d10-$i", "d10-$i.gif"))
        }
        replacerList.add(SimpleReplacer.createImage("city", "ttr_city.gif"))
        createTrainImage("red")
        createTrainImage("green")
        createTrainImage("blue")
        createTrainImage("yellow")
        createTrainImage("black")
        createTrainImage("purple")
        createTrainImage("white")
        replacerList.add(SimpleReplacer.createGif("wood"))
        replacerList.add(SimpleReplacer.createGif("wheat"))
        replacerList.add(SimpleReplacer.createGif("sheep"))
        replacerList.add(SimpleReplacer.createGif("ore"))
        replacerList.add(SimpleReplacer.createGif("brick"))
        replacerList.add(SimpleReplacer.createGif("cinnamon"))
        replacerList.add(SimpleReplacer.createGif("clove"))
        replacerList.add(SimpleReplacer.createGif("ginger"))
        replacerList.add(SimpleReplacer.createGif("nutmeg"))
        replacerList.add(SimpleReplacer.createGif("pepper"))
        replacerList.add(SimpleReplacer.createGif("coal"))
        replacerList.add(SimpleReplacer.createGif("oil"))
        replacerList.add(SimpleReplacer.createGif("trash"))
        replacerList.add(SimpleReplacer.createGif("nuclear"))
        for (i in 1..6) {
            replacerList.add(SimpleReplacer.createImage("d6-$i", "die-white-$i.gif"))
            replacerList.add(SimpleReplacer.createImage("bd6-$i", "die-black-$i.gif"))
        }
        replacerList.add(SimpleReplacer.createPng("tankard"))
        replacerList.add(SimpleReplacer.createPng("jug"))
        replacerList.add(SimpleReplacer.createPng("chalice"))
        replacerList.add(SimpleReplacer.createGif("worker"))
        replacerList.add(SimpleReplacer.createGif("building"))
        replacerList.add(SimpleReplacer.createGif("aristocrat"))
        replacerList.add(SimpleReplacer.createGif("trade"))
        replacerList.add(SimpleReplacer.createPng("arrowN"))
        replacerList.add(SimpleReplacer.createPng("arrowNE"))
        replacerList.add(SimpleReplacer.createPng("arrowE"))
        replacerList.add(SimpleReplacer.createPng("arrowSE"))
        replacerList.add(SimpleReplacer.createPng("arrowS"))
        replacerList.add(SimpleReplacer.createPng("arrowSW"))
        replacerList.add(SimpleReplacer.createPng("arrowW"))
        replacerList.add(SimpleReplacer.createPng("arrowNW"))
        replacerList.add(SimpleReplacer.createPng("power"))
        replacerList.add(SimpleReplacer.createPng("XBA"))
        replacerList.add(SimpleReplacer.createPng("XBB"))
        replacerList.add(SimpleReplacer.createPng("XBX"))
        replacerList.add(SimpleReplacer.createPng("XBY"))
        replacerList.add(SimpleReplacer.createPng("PSC"))
        replacerList.add(SimpleReplacer.createPng("PSS"))
        replacerList.add(SimpleReplacer.createPng("PST"))
        replacerList.add(SimpleReplacer.createPng("PSX"))
        replacerList.add(SimpleReplacer.createPng("WiiH"))
        replacerList.add(SimpleReplacer.createPng("Wii1"))
        replacerList.add(SimpleReplacer.createPng("Wii2"))
        replacerList.add(SimpleReplacer.createPng("WiiA"))
        replacerList.add(SimpleReplacer.createPng("WiiB"))
        replacerList.add(SimpleReplacer.createPng("WiiC"))
        replacerList.add(SimpleReplacer.createPng("WiiX"))
        replacerList.add(SimpleReplacer.createPng("WiiY"))
        replacerList.add(SimpleReplacer.createPng("WiiZ"))
        replacerList.add(SimpleReplacer.createPng("Wii\\+"))
        replacerList.add(SimpleReplacer.createPng("Wii-"))
        replacerList.add(SimpleReplacer.createImage("!block", "bang_block.png"))
        replacerList.add(SimpleReplacer.createImage("\\?block", "question_block.png"))
        replacerList.add(SimpleReplacer.createImage("blank", "tiles/BLANK.gif"))
        replacerList.add(UpperCaseReplacer("\\:([A-Za-z])\\:", "<img src=\"${STATIC_IMAGES_URL}tiles/", ".gif\"/>"))
        replacerList.add(Replacer("\\:k([A-Za-z])\\:", "<img src=\"${STATIC_IMAGES_URL}k", ".png\"/>"))
    }
}
