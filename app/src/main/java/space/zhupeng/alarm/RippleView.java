package space.zhupeng.alarm;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Build;
import android.support.annotation.Nullable;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.View;

/**
 * Created by zhupeng on 2017/10/10.
 */

public class RippleView extends View {

    private Paint mStrokePaint;
    private Paint mTextPaint;

    private String mText;
    private int mTextColor;
    private float mTextSize;
    private float mStrokeWidth;
    private int mStrokeColor;

    private float mTextWidth;

    private boolean isStarted;
    private float mMaxRadius;
    private float mVarRadius;

    public RippleView(Context context) {
        this(context, null);
    }

    public RippleView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RippleView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        init(context, attrs, defStyleAttr);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public RippleView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        init(context, attrs, defStyleAttr);
    }

    private void init(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.RippleView, defStyleAttr, 0);
        mText = a.getString(R.styleable.RippleView_android_text);
        mTextColor = a.getColor(R.styleable.RippleView_android_textColor, Color.BLACK);
        mTextSize = a.getDimensionPixelSize(R.styleable.RippleView_android_textSize, 16);
        mStrokeWidth = a.getDimension(R.styleable.RippleView_strokeWidth, 1f);
        mStrokeColor = a.getColor(R.styleable.RippleView_strokeColor, Color.BLACK);
        a.recycle();

        mStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mStrokePaint.setColor(mStrokeColor);
        mStrokePaint.setStrokeWidth(mStrokeWidth);
        mStrokePaint.setStyle(Paint.Style.STROKE);

        mTextPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        mTextPaint.setTextSize(mTextSize);
        mTextPaint.setColor(mTextColor);

        mTextWidth = mTextPaint.measureText(mText);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension((int) (mTextWidth * 3f), (int) (mTextWidth * 3f));
    }

    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        super.onWindowFocusChanged(hasWindowFocus);
        if (hasWindowFocus) {
            mMaxRadius = getMeasuredWidth() / 2;
            mVarRadius = mMaxRadius % 100;
            start();
        } else {
            stop();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        stop();
    }

    public void start() {
        if (!isStarted) {
            isStarted = true;
            new Thread(runnable).start();
        }
    }

    public void stop() {
        isStarted = true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (mMaxRadius <= 0.0F) {
            return;
        }

        float cx = getMeasuredWidth() / 2f;
        float cy = getMeasuredHeight() / 2f;
        float radius = mTextWidth / 2f + 10;

        mStrokePaint.setAlpha(255);
        canvas.drawCircle(cx, cy, radius, mStrokePaint);

        Paint.FontMetricsInt fontMetrics = mTextPaint.getFontMetricsInt();
        float baseline = mTextWidth * 1.5f - (fontMetrics.bottom - fontMetrics.top) / 2 - fontMetrics.top;
        canvas.drawText(mText, mTextWidth, baseline, mTextPaint);

        float waveRadius = mVarRadius % 100;
        while (true) {
            int alpha = (int) (255.0F * (1.0F - (waveRadius + radius) / mMaxRadius));
            if (alpha <= 0) {
                break;
            }

            mStrokePaint.setAlpha(alpha);
            canvas.drawCircle(cx, cy, waveRadius + radius, mStrokePaint);
            waveRadius += 100;
        }
    }

    private final Runnable runnable = new Runnable() {
        @Override
        public void run() {
            while (isStarted) {
                mVarRadius += 4.0F;
                if (mVarRadius > mMaxRadius) {
                    mVarRadius = mMaxRadius % 100;
                    postInvalidate();
                }
                postInvalidate();
                try {
                    Thread.sleep(50L);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    };
}
