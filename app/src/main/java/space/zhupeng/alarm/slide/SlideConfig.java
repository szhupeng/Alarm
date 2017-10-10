package space.zhupeng.alarm.slide;

import android.graphics.Color;
import android.support.annotation.ColorInt;
import android.support.annotation.FloatRange;
import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public final class SlideConfig {

    public static final int LEFT = 0;
    public static final int RIGHT = 1;
    public static final int TOP = 2;
    public static final int BOTTOM = 3;
    public static final int VERTICAL = 4;
    public static final int HORIZONTAL = 5;

    @IntDef({LEFT, RIGHT, TOP, BOTTOM, VERTICAL, HORIZONTAL})
    @Retention(RetentionPolicy.SOURCE)
    @interface SlidePosition {
    }

    private int mPrimaryColor = -1;
    private int mSecondaryColor = -1;
    private float mTouchSize = -1f;
    private float mSensitivity = 1f;
    private int mScrimColor = Color.BLACK;
    private float mScrimStartAlpha = 0.8f;
    private float mScrimEndAlpha = 0f;
    private float mVelocityThreshold = 5f;
    private float mDistanceThreshold = 0.25f;
    private boolean edgeOnly = false;
    private float mEdgeSize = 0.18f;

    @SlidePosition
    private int position = LEFT;

    private OnSlideListener listener;

    public int getPrimaryColor() {
        return mPrimaryColor;
    }

    public void setPrimaryColor(int color) {
        this.mPrimaryColor = color;
    }

    public int getSecondaryColor() {
        return mSecondaryColor;
    }

    public void setSecondaryColor(int color) {
        this.mSecondaryColor = color;
    }

    public float getTouchSize() {
        return mTouchSize;
    }

    public void setTouchSize(float touchSize) {
        this.mTouchSize = touchSize;
    }

    public float getSensitivity() {
        return mSensitivity;
    }

    public void setSensitivity(float sensitivity) {
        this.mSensitivity = sensitivity;
    }

    public int getScrimColor() {
        return mScrimColor;
    }

    public void setScrimColor(int color) {
        this.mScrimColor = color;
    }

    public float getScrimStartAlpha() {
        return mScrimStartAlpha;
    }

    public void setScrimStartAlpha(float alpha) {
        this.mScrimStartAlpha = alpha;
    }

    public float getScrimEndAlpha() {
        return mScrimEndAlpha;
    }

    public void setScrimEndAlpha(float alpha) {
        this.mScrimEndAlpha = alpha;
    }

    public float getVelocityThreshold() {
        return mVelocityThreshold;
    }

    public void setVelocityThreshold(float threshold) {
        this.mVelocityThreshold = threshold;
    }

    public float getDistanceThreshold() {
        return mDistanceThreshold;
    }

    public void setDistanceThreshold(float threshold) {
        this.mDistanceThreshold = threshold;
    }

    public boolean isEdgeOnly() {
        return edgeOnly;
    }

    public void setEdgeOnly(boolean edgeOnly) {
        this.edgeOnly = edgeOnly;
    }

    public float getEdgeSize(float size) {
        return mEdgeSize * size;
    }

    public void setEdgeSize(float edgeSize) {
        this.mEdgeSize = edgeSize;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(@SlidePosition int position) {
        this.position = position;
    }

    public OnSlideListener getListener() {
        return listener;
    }

    public void setListener(OnSlideListener listener) {
        this.listener = listener;
    }

    public boolean areStatusBarColorsValid() {
        return mPrimaryColor != -1 && mSecondaryColor != -1;
    }

    public static class Builder {

        private SlideConfig config;

        public Builder() {
            config = new SlideConfig();
        }

        public Builder primaryColor(@ColorInt int color) {
            config.mPrimaryColor = color;
            return this;
        }

        public Builder secondaryColor(@ColorInt int color) {
            config.mSecondaryColor = color;
            return this;
        }

        public Builder position(@SlidePosition int position) {
            config.position = position;
            return this;
        }

        public Builder touchSize(float size) {
            config.mTouchSize = size;
            return this;
        }

        public Builder sensitivity(float sensitivity) {
            config.mSensitivity = sensitivity;
            return this;
        }

        public Builder scrimColor(@ColorInt int color) {
            config.mScrimColor = color;
            return this;
        }

        public Builder scrimStartAlpha(@FloatRange(from = 0.0, to = 1.0) float alpha) {
            config.mScrimStartAlpha = alpha;
            return this;
        }

        public Builder scrimEndAlpha(@FloatRange(from = 0.0, to = 1.0) float alpha) {
            config.mScrimEndAlpha = alpha;
            return this;
        }

        public Builder velocityThreshold(float threshold) {
            config.mVelocityThreshold = threshold;
            return this;
        }

        public Builder distanceThreshold(@FloatRange(from = .1f, to = .9f) float threshold) {
            config.mDistanceThreshold = threshold;
            return this;
        }

        public Builder edgeOnly(boolean edgeOnly) {
            config.edgeOnly = edgeOnly;
            return this;
        }

        public Builder edgeSize(@FloatRange(from = 0f, to = 1f) float edgeSize) {
            config.mEdgeSize = edgeSize;
            return this;
        }

        public Builder listener(OnSlideListener listener) {
            config.listener = listener;
            return this;
        }

        public SlideConfig build() {
            return config;
        }
    }
}
