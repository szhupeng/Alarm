package space.zhupeng.alarm;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.support.annotation.ColorInt;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * Created by zhupeng on 2017/10/10.
 */

public class ArrowDrawable extends Drawable {

    private static final int OFFSET = 5;
    private static final float ANGLE = 40f;

    private Paint mPaint;
    private int height = 50;

    private Handler handler = new Handler();

    public ArrowDrawable(@ColorInt int color, float strokeWidth, int height) {
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setColor(color);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(strokeWidth);

        this.height = height;
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        float halfWidth = (float) (this.height / 2f * Math.tan(Math.PI * (ANGLE / 180)));
        canvas.drawLine(this.height / 2f - halfWidth, this.height / 2f, this.height / 2f, 0f, mPaint);
        canvas.drawLine(this.height / 2f, 0f, this.height / 2f + halfWidth, this.height / 2f, mPaint);
        mPaint.setAlpha(150 == mPaint.getAlpha() ? 255 : 150);
        canvas.drawLine(this.height / 2f - halfWidth, this.height + OFFSET, this.height / 2f, this.height / 2f + OFFSET, mPaint);
        canvas.drawLine(this.height / 2f, this.height / 2f + OFFSET, this.height / 2f + halfWidth, this.height + OFFSET, mPaint);

        handler.postDelayed(runnable, 500);
    }

    @Override
    public void setAlpha(@IntRange(from = 0, to = 255) int alpha) {
        mPaint.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter) {
        mPaint.setColorFilter(colorFilter);
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

    @Override
    public int getIntrinsicWidth() {
        return getIntrinsicHeight();
    }

    @Override
    public int getIntrinsicHeight() {
        return height + OFFSET;
    }

    private final Runnable runnable = new Runnable() {
        @Override
        public void run() {
            invalidateSelf();
        }
    };
}
