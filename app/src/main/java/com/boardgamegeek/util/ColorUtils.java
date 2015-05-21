package com.boardgamegeek.util;

import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.util.Pair;
import android.util.TypedValue;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

/**
 * Static methods for modifying and applying colors to views.
 */
public class ColorUtils {
    public static final int TRANSPARENT = 0;
    private static final int BLACK = 0xFF000000;
    // private static final int DKGRAY = 0xFF444444;
    private static final int GRAY = 0xFF888888;
    // private static final int LTGRAY = 0xFFCCCCCC;
    private static final int WHITE = 0xFFFFFFFF;
    private static final int RED = 0xFFFF0000;
    private static final int GREEN = 0xFF008000; // dark green really
    private static final int BLUE = 0xFF0000FF;
    private static final int YELLOW = 0xFFFFFF00;
    // private static final int CYAN = 0xFF00FFFF;
    // private static final int MAGENTA = 0xFFFF00FF;
    private static final int PURPLE = 0xFF800080;
    private static final int ORANGE = 0xFFE59400;
    private static final int BROWN = 0xFFA52A2A;
    private static final int NATURAL = 0xFFE9C2A6;
    private static final int TAN = 0xFFDB9370;
    private static final int IVORY = 0xFFFFFFF0;
    private static final int ROSE = 0xFFFF007F;
    private static final int PINK = 0xFFCD919E;
    private static final int TEAL = 0xFF008080;
    // private static final int AQUA = 0xFF66CCCC;
    private static final int BRONZE = 0xFF8C7853;
    private static final int SILVER = 0xFFC0C0C0;
    private static final int GOLD = 0xFFFFD700;

    private ColorUtils() {
    }

    /**
     * Determine the RGB value of a named color, or a string formatted as "#aarrggbb". Returns a transparent color if
     * the color can't be determined from the string.
     */
    public static int parseColor(String colorString) {
        if (TextUtils.isEmpty(colorString)) {
            return TRANSPARENT;
        }
        if (colorString.charAt(0) == '#') {
            // Use a long to avoid rollovers on #ffXXXXXX
            long color = Long.parseLong(colorString.substring(1), 16);
            if (colorString.length() == 7) {
                // Set the alpha value
                color |= 0x00000000ff000000;
            } else if (colorString.length() != 9) {
                return TRANSPARENT;
            }
            return (int) color;
        } else {
            Integer color = sColorNameMap.get(formatKey(colorString));
            if (color != null) {
                return color;
            }
        }
        return TRANSPARENT;
    }

    /**
     * Returns a color based on the rating. This maps to the colors used on BGG for integers, using a proportional blend
     * for any decimal places.
     */
    public static int getRatingColor(double rating) {
        int baseRating = MathUtils.constrain((int) rating, 0, 10);
        return blendColors(RATING_COLORS[baseRating], RATING_COLORS[baseRating + 1], baseRating + 1 - rating);
    }

    /**
     * Returns a color based that is ratio% of color1 and (1 - ratio)% of color2 (including alpha).
     */
    private static int blendColors(int color1, int color2, double ratio) {
        double ir = 1.0 - ratio;

        int a = (int) (Color.alpha(color1) * ratio + Color.alpha(color2) * ir);
        int r = (int) (Color.red(color1) * ratio + Color.red(color2) * ir);
        int g = (int) (Color.green(color1) * ratio + Color.green(color2) * ir);
        int b = (int) (Color.blue(color1) * ratio + Color.blue(color2) * ir);

        return Color.argb(a, r, g, b);
    }

    /**
     * An array of RGBs that match the BGG ratings from 0 to 10.
     */
    private static final int[] RATING_COLORS = {0x00ffffff, 0xffff0000, 0xffff3366, 0xffff6699, 0xffff66cc,
            0xffcc99ff, 0xff9999ff, 0xff99ffff, 0xff66ff99, 0xff33cc99, 0xff00cc00, 0x00ffffff};

    private static final HashMap<String, Integer> sColorNameMap;
    private static final ArrayList<Pair<String, Integer>> sColorNameList;

    static {
        sColorNameList = new ArrayList<>();
        sColorNameList.add(new Pair<>("Red", RED));
        sColorNameList.add(new Pair<>("Yellow", YELLOW));
        sColorNameList.add(new Pair<>("Blue", BLUE));
        sColorNameList.add(new Pair<>("Green", GREEN));
        sColorNameList.add(new Pair<>("Purple", PURPLE));
        sColorNameList.add(new Pair<>("Orange", ORANGE));
        sColorNameList.add(new Pair<>("White", WHITE));
        sColorNameList.add(new Pair<>("Black", BLACK));
        sColorNameList.add(new Pair<>("Natural", NATURAL));
        sColorNameList.add(new Pair<>("Brown", BROWN));
        sColorNameList.add(new Pair<>("Tan", TAN));
        sColorNameList.add(new Pair<>("Gray", GRAY));
        sColorNameList.add(new Pair<>("Gold", GOLD));
        sColorNameList.add(new Pair<>("Silver", SILVER));
        sColorNameList.add(new Pair<>("Bronze", BRONZE));
        sColorNameList.add(new Pair<>("Ivory", IVORY));
        sColorNameList.add(new Pair<>("Rose", ROSE));
        sColorNameList.add(new Pair<>("Pink", PINK));
        sColorNameList.add(new Pair<>("Teal", TEAL));
        // sColorNameList.add(new Pair<String, Integer>("Aqua", AQUA));
        // sColorNameList.add(new Pair<String, Integer>("Cyan", CYAN));
        // sColorNameList.add(new Pair<String, Integer>("Magenta", MAGENTA));
        // sColorNameList.add(new Pair<String, Integer>("Light Gray", LTGRAY));
        // sColorNameList.add(new Pair<String, Integer>("Dark Gray", DKGRAY));

        sColorNameMap = new HashMap<>();
        for (Pair<String, Integer> pair : sColorNameList) {
            sColorNameMap.put(formatKey(pair.first), pair.second);
        }
    }

    private static String formatKey(String name) {
        return name.toLowerCase(Locale.US);
    }

    public static List<Pair<String, Integer>> getColorList() {
        return (List<Pair<String, Integer>>) sColorNameList.clone();
    }

    @SuppressWarnings("deprecation")
    /**
     * Set the background of a {@link android.widget.View} to the specified color, with a darker version of the
     * color as a border.
     */
    public static void setViewBackground(View view, int color) {
        Resources r = view.getResources();

        Drawable currentDrawable = view.getBackground();
        GradientDrawable backgroundDrawable;
        if (currentDrawable != null && currentDrawable instanceof GradientDrawable) {
            // Reuse drawable
            backgroundDrawable = (GradientDrawable) currentDrawable;
        } else {
            backgroundDrawable = new GradientDrawable();
        }

        int darkenedColor = darkenColor(color);

        backgroundDrawable.setColor(color);
        backgroundDrawable.setStroke(
                (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, r.getDisplayMetrics()), darkenedColor);

        view.setBackgroundDrawable(backgroundDrawable);
    }

    /**
     * Set the background of an {@link android.widget.ImageView} to an oval of the specified color, with a darker
     * version of the color as a border. For a {@link android.widget.TextView}, changes the text color instead. Doesn't
     * do anything for other views. Modified from Roman Nurik's DashClock (https://code.google.com/p/dashclock/).
     */
    public static void setColorViewValue(View view, int color) {
        if (view instanceof ImageView) {
            ImageView imageView = (ImageView) view;

            Resources r = imageView.getResources();

            Drawable currentDrawable = imageView.getDrawable();
            GradientDrawable colorChoiceDrawable;
            if (currentDrawable != null && currentDrawable instanceof GradientDrawable) {
                // Reuse drawable
                colorChoiceDrawable = (GradientDrawable) currentDrawable;
            } else {
                colorChoiceDrawable = new GradientDrawable();
                colorChoiceDrawable.setShape(GradientDrawable.OVAL);
            }

            // Set stroke to dark version of color
            int darkenedColor = darkenColor(color);

            colorChoiceDrawable.setColor(color);
            final int width = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, r.getDisplayMetrics());
            colorChoiceDrawable.setStroke(width, darkenedColor);

            imageView.setImageDrawable(colorChoiceDrawable);
        } else if (view instanceof TextView) {
            if (color != ColorUtils.TRANSPARENT) {
                ((TextView) view).setTextColor(color);
            }
        }
    }

    /**
     * Returns a darker version of the specified color. Returns a translucent gray for transparent colors.
     */
    private static int darkenColor(int color) {
        if (color == TRANSPARENT) {
            return Color.argb(127, 127, 127, 127);
        }
        return Color.rgb(Color.red(color) * 192 / 256, Color.green(color) * 192 / 256, Color.blue(color) * 192 / 256);
    }

    /**
     * Calculate whether a color is light or dark, based on a commonly known brightness formula.
     *
     * @see {@literal http://en.wikipedia.org/wiki/HSV_color_space%23Lightness}
     */
    public static boolean isColorDark(int color) {
        return ((30 * Color.red(color) + 59 * Color.green(color) + 11 * Color.blue(color)) / 100) <= 130;
    }
}
