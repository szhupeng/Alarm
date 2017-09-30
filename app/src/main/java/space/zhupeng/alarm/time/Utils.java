package space.zhupeng.alarm.time;

import android.animation.Keyframe;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.os.Build;
import android.support.annotation.AttrRes;
import android.support.v4.content.ContextCompat;
import android.util.TypedValue;
import android.view.View;

import java.util.Calendar;

import space.zhupeng.alarm.R;

/**
 * Utility helper functions for time and date pickers.
 */
@SuppressWarnings("WeakerAccess")
public class Utils {

    //public static final int MONDAY_BEFORE_JULIAN_EPOCH = Time.EPOCH_JULIAN_DAY - 3;
    public static final int PULSE_ANIMATOR_DURATION = 544;

    // Alpha level for time picker selection.
    public static final int SELECTED_ALPHA = 255;
    public static final int SELECTED_ALPHA_THEME_DARK = 255;
    // Alpha level for fully opaque.
    public static final int FULL_ALPHA = 255;

    public static boolean isJellybeanOrLater() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN;
    }

    /**
     * Try to speak the specified text, for accessibility. Only available on JB or later.
     *
     * @param text Text to announce.
     */
    @SuppressLint("NewApi")
    public static void tryAccessibilityAnnounce(View view, CharSequence text) {
        if (isJellybeanOrLater() && view != null && text != null) {
            view.announceForAccessibility(text);
        }
    }

    /**
     * Render an animator to pulsate a view in place.
     *
     * @param labelToAnimate the view to pulsate.
     * @return The animator object. Use .start() to begin.
     */
    public static ObjectAnimator getPulseAnimator(View labelToAnimate, float decreaseRatio,
                                                  float increaseRatio) {
        Keyframe k0 = Keyframe.ofFloat(0f, 1f);
        Keyframe k1 = Keyframe.ofFloat(0.275f, decreaseRatio);
        Keyframe k2 = Keyframe.ofFloat(0.69f, increaseRatio);
        Keyframe k3 = Keyframe.ofFloat(1f, 1f);

        PropertyValuesHolder scaleX = PropertyValuesHolder.ofKeyframe("scaleX", k0, k1, k2, k3);
        PropertyValuesHolder scaleY = PropertyValuesHolder.ofKeyframe("scaleY", k0, k1, k2, k3);
        ObjectAnimator pulseAnimator =
                ObjectAnimator.ofPropertyValuesHolder(labelToAnimate, scaleX, scaleY);
        pulseAnimator.setDuration(PULSE_ANIMATOR_DURATION);

        return pulseAnimator;
    }

    /**
     * Convert Dp to Pixel
     */
    @SuppressWarnings("unused")
    public static int dpToPx(float dp, Resources resources) {
        float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, resources.getDisplayMetrics());
        return (int) px;
    }

    public static int darkenColor(int color) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[2] = hsv[2] * 0.8f; // value component
        return Color.HSVToColor(hsv);
    }

    /**
     * Gets the colorAccent from the current context, if possible/available
     *
     * @param context The context to use as reference for the color
     * @return the accent color of the current context
     */
    public static int getAccentColorFromThemeIfAvailable(Context context) {
        TypedValue typedValue = new TypedValue();
        // First, try the android:colorAccent
        if (Build.VERSION.SDK_INT >= 21) {
            context.getTheme().resolveAttribute(android.R.attr.colorAccent, typedValue, true);
            return typedValue.data;
        }
        // Next, try colorAccent from support lib
        int colorAccentResId = context.getResources().getIdentifier("colorAccent", "attr", context.getPackageName());
        if (colorAccentResId != 0 && context.getTheme().resolveAttribute(colorAccentResId, typedValue, true)) {
            return typedValue.data;
        }
        // Return the value in mdtp_accent_color
        return ContextCompat.getColor(context, R.color.colorAccent);
    }

    /**
     * Gets the required boolean value from the current context, if possible/available
     *
     * @param context  The context to use as reference for the boolean
     * @param attr     Attribute id to resolve
     * @param fallback Default value to return if no value is specified in theme
     * @return the boolean value from current theme
     */
    private static boolean resolveBoolean(Context context, @AttrRes int attr, boolean fallback) {
        TypedArray a = context.getTheme().obtainStyledAttributes(new int[]{attr});
        try {
            return a.getBoolean(0, fallback);
        } finally {
            a.recycle();
        }
    }

    /**
     * Trims off all time information, effectively setting it to midnight
     * Makes it easier to compare at just the day level
     *
     * @param calendar The Calendar object to trim
     * @return The trimmed Calendar object
     */
    public static Calendar trimToMidnight(Calendar calendar) {
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar;
    }
}
