package space.zhupeng.alarm.slide;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewGroupCompat;
import android.support.v4.widget.ViewDragHelper;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

class SlideLayout extends FrameLayout {

    private static final int MIN_FLING_VELOCITY = 400; // dips per second

    private int mScreenWidth;
    private int mScreenHeight;

    private View mDimView;
    private View mDecorView;
    private ViewDragHelper mDragHelper;
    private OnLayoutSlideListener mListener;

    private boolean isLocked = false;
    private boolean isEdgeTouched = false;
    private int mEdgePosition;

    private SlideConfig mConfig;

    public SlideLayout(@NonNull Context context) {
        super(context);
    }

    public SlideLayout(Context context, View decorView) {
        this(context, decorView, null);
    }

    public SlideLayout(Context context, View decorView, SlideConfig config) {
        super(context);
        mDecorView = decorView;
        mConfig = (config == null ? new SlideConfig.Builder().build() : config);
        init();
    }

    private void init() {
        mScreenWidth = getResources().getDisplayMetrics().widthPixels;

        final float density = getResources().getDisplayMetrics().density;
        final float minVel = MIN_FLING_VELOCITY * density;

        ViewDragHelper.Callback callback;
        switch (mConfig.getPosition()) {
            case SlideConfig.LEFT:
                callback = mLeftCallback;
                mEdgePosition = ViewDragHelper.EDGE_LEFT;
                break;
            case SlideConfig.RIGHT:
                callback = mRightCallback;
                mEdgePosition = ViewDragHelper.EDGE_RIGHT;
                break;
            case SlideConfig.TOP:
                callback = mTopCallback;
                mEdgePosition = ViewDragHelper.EDGE_TOP;
                break;
            case SlideConfig.BOTTOM:
                callback = mBottomCallback;
                mEdgePosition = ViewDragHelper.EDGE_BOTTOM;
                break;
            case SlideConfig.VERTICAL:
                callback = mVerticalCallback;
                mEdgePosition = ViewDragHelper.EDGE_TOP | ViewDragHelper.EDGE_BOTTOM;
                break;
            case SlideConfig.HORIZONTAL:
                callback = mHorizontalCallback;
                mEdgePosition = ViewDragHelper.EDGE_LEFT | ViewDragHelper.EDGE_RIGHT;
                break;
            default:
                callback = mLeftCallback;
                mEdgePosition = ViewDragHelper.EDGE_LEFT;
        }

        mDragHelper = ViewDragHelper.create(this, mConfig.getSensitivity(), callback);
        mDragHelper.setMinVelocity(minVel);
        mDragHelper.setEdgeTrackingEnabled(mEdgePosition);

        ViewGroupCompat.setMotionEventSplittingEnabled(this, false);

        // Setup the dimmer view
        mDimView = new View(getContext());
        mDimView.setBackgroundColor(mConfig.getScrimColor());
        mDimView.setAlpha(mConfig.getScrimStartAlpha());

        // Add the dimmer view to the layout
        addView(mDimView);

        /*
         * This is so we can get the height of the view and
         * ignore the system navigation that would be included if we
         * retrieved this value from the DisplayMetrics
         */
        post(new Runnable() {
            @Override
            public void run() {
                mScreenHeight = getHeight();
            }
        });
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        boolean interceptForDrag;

        if (isLocked) return false;

        if (mConfig.isEdgeOnly()) isEdgeTouched = canDragFromEdge(ev);

        // Fix for pull request #13 and issue #12
        try {
            interceptForDrag = mDragHelper.shouldInterceptTouchEvent(ev);
        } catch (Exception e) {
            interceptForDrag = false;
        }

        return interceptForDrag && !isLocked;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (isLocked) return false;

        try {
            mDragHelper.processTouchEvent(event);
        } catch (IllegalArgumentException e) {
            return false;
        }

        return true;
    }

    @Override
    public void computeScroll() {
        super.computeScroll();

        if (mDragHelper.continueSettling(true)) {
            ViewCompat.postInvalidateOnAnimation(this);
        }
    }

    public final void lock() {
        mDragHelper.abort();
        isLocked = true;
    }

    public final void unlock() {
        mDragHelper.abort();
        isLocked = false;
    }

    private boolean canDragFromEdge(MotionEvent ev) {
        float x = ev.getX();
        float y = ev.getY();

        switch (mConfig.getPosition()) {
            case SlideConfig.LEFT:
                return x < mConfig.getEdgeSize(getWidth());
            case SlideConfig.RIGHT:
                return x > getWidth() - mConfig.getEdgeSize(getWidth());
            case SlideConfig.BOTTOM:
                return y > getHeight() - mConfig.getEdgeSize(getHeight());
            case SlideConfig.TOP:
                return y < mConfig.getEdgeSize(getHeight());
            case SlideConfig.HORIZONTAL:
                return x < mConfig.getEdgeSize(getWidth()) || x > getWidth() - mConfig.getEdgeSize(getWidth());
            case SlideConfig.VERTICAL:
                return y < mConfig.getEdgeSize(getHeight()) || y > getHeight() - mConfig.getEdgeSize(getHeight());
        }
        return false;
    }

    private final ViewDragHelper.Callback mLeftCallback = new ViewDragHelper.Callback() {

        @Override
        public boolean tryCaptureView(View child, int pointerId) {
            boolean edgeCase = !mConfig.isEdgeOnly() || mDragHelper.isEdgeTouched(mEdgePosition, pointerId);
            return child.getId() == mDecorView.getId() && edgeCase;
        }

        @Override
        public int clampViewPositionHorizontal(View child, int left, int dx) {
            return clamp(left, 0, mScreenWidth);
        }

        @Override
        public int getViewHorizontalDragRange(View child) {
            return mScreenWidth;
        }

        @Override
        public void onViewReleased(View releasedChild, float xvel, float yvel) {
            super.onViewReleased(releasedChild, xvel, yvel);

            int left = releasedChild.getLeft();
            int settleLeft = 0;
            int leftThreshold = (int) (getWidth() * mConfig.getDistanceThreshold());
            boolean isVerticalSwiping = Math.abs(yvel) > mConfig.getVelocityThreshold();

            if (xvel > 0) {

                if (Math.abs(xvel) > mConfig.getVelocityThreshold() && !isVerticalSwiping) {
                    settleLeft = mScreenWidth;
                } else if (left > leftThreshold) {
                    settleLeft = mScreenWidth;
                }

            } else if (xvel == 0) {
                if (left > leftThreshold) {
                    settleLeft = mScreenWidth;
                }
            }

            mDragHelper.settleCapturedViewAt(settleLeft, releasedChild.getTop());
            invalidate();
        }

        @Override
        public void onViewPositionChanged(View changedView, int left, int top, int dx, int dy) {
            super.onViewPositionChanged(changedView, left, top, dx, dy);
            float percent = 1f - ((float) left / (float) mScreenWidth);

            if (mListener != null) mListener.onSlideChange(percent);

            // Update the dimmer alpha
            applyScrim(percent);
        }

        @Override
        public void onViewDragStateChanged(int state) {
            super.onViewDragStateChanged(state);
            if (mListener != null) mListener.onStateChanged(state);
            switch (state) {
                case ViewDragHelper.STATE_IDLE:
                    if (mDecorView.getLeft() == 0) {
                        // State Open
                        if (mListener != null) mListener.onOpened();
                    } else {
                        // State Closed
                        if (mListener != null) mListener.onClosed();
                    }
                    break;
                case ViewDragHelper.STATE_DRAGGING:

                    break;
                case ViewDragHelper.STATE_SETTLING:

                    break;
            }
        }

    };

    private final ViewDragHelper.Callback mRightCallback = new ViewDragHelper.Callback() {
        @Override
        public boolean tryCaptureView(View child, int pointerId) {
            boolean edgeCase = !mConfig.isEdgeOnly() || mDragHelper.isEdgeTouched(mEdgePosition, pointerId);
            return child.getId() == mDecorView.getId() && edgeCase;
        }

        @Override
        public int clampViewPositionHorizontal(View child, int left, int dx) {
            return clamp(left, -mScreenWidth, 0);
        }

        @Override
        public int getViewHorizontalDragRange(View child) {
            return mScreenWidth;
        }

        @Override
        public void onViewReleased(View releasedChild, float xvel, float yvel) {
            super.onViewReleased(releasedChild, xvel, yvel);

            int left = releasedChild.getLeft();
            int settleLeft = 0;
            int leftThreshold = (int) (getWidth() * mConfig.getDistanceThreshold());
            boolean isVerticalSwiping = Math.abs(yvel) > mConfig.getVelocityThreshold();

            if (xvel < 0) {

                if (Math.abs(xvel) > mConfig.getVelocityThreshold() && !isVerticalSwiping) {
                    settleLeft = -mScreenWidth;
                } else if (left < -leftThreshold) {
                    settleLeft = -mScreenWidth;
                }

            } else if (xvel == 0) {
                if (left < -leftThreshold) {
                    settleLeft = -mScreenWidth;
                }
            }

            mDragHelper.settleCapturedViewAt(settleLeft, releasedChild.getTop());
            invalidate();
        }

        @Override
        public void onViewPositionChanged(View changedView, int left, int top, int dx, int dy) {
            super.onViewPositionChanged(changedView, left, top, dx, dy);
            float percent = 1f - ((float) Math.abs(left) / (float) mScreenWidth);

            if (mListener != null) mListener.onSlideChange(percent);

            // Update the dimmer alpha
            applyScrim(percent);
        }

        @Override
        public void onViewDragStateChanged(int state) {
            super.onViewDragStateChanged(state);
            if (mListener != null) mListener.onStateChanged(state);
            switch (state) {
                case ViewDragHelper.STATE_IDLE:
                    if (mDecorView.getLeft() == 0) {
                        // State Open
                        if (mListener != null) mListener.onOpened();
                    } else {
                        // State Closed
                        if (mListener != null) mListener.onClosed();
                    }
                    break;
                case ViewDragHelper.STATE_DRAGGING:

                    break;
                case ViewDragHelper.STATE_SETTLING:

                    break;
            }
        }
    };

    private final ViewDragHelper.Callback mTopCallback = new ViewDragHelper.Callback() {
        @Override
        public boolean tryCaptureView(View child, int pointerId) {
            return child.getId() == mDecorView.getId() && (!mConfig.isEdgeOnly() || isEdgeTouched);
        }

        @Override
        public int clampViewPositionVertical(View child, int top, int dy) {
            return clamp(top, 0, mScreenHeight);
        }

        @Override
        public int getViewVerticalDragRange(View child) {
            return mScreenHeight;
        }

        @Override
        public void onViewReleased(View releasedChild, float xvel, float yvel) {
            super.onViewReleased(releasedChild, xvel, yvel);

            int top = releasedChild.getTop();
            int settleTop = 0;
            int topThreshold = (int) (getHeight() * mConfig.getDistanceThreshold());
            boolean isSideSwiping = Math.abs(xvel) > mConfig.getVelocityThreshold();

            if (yvel > 0) {
                if (Math.abs(yvel) > mConfig.getVelocityThreshold() && !isSideSwiping) {
                    settleTop = mScreenHeight;
                } else if (top > topThreshold) {
                    settleTop = mScreenHeight;
                }
            } else if (yvel == 0) {
                if (top > topThreshold) {
                    settleTop = mScreenHeight;
                }
            }

            mDragHelper.settleCapturedViewAt(releasedChild.getLeft(), settleTop);
            invalidate();
        }

        @Override
        public void onViewPositionChanged(View changedView, int left, int top, int dx, int dy) {
            super.onViewPositionChanged(changedView, left, top, dx, dy);
            float percent = 1f - ((float) Math.abs(top) / (float) mScreenHeight);

            if (mListener != null) mListener.onSlideChange(percent);

            // Update the dimmer alpha
            applyScrim(percent);
        }

        @Override
        public void onViewDragStateChanged(int state) {
            super.onViewDragStateChanged(state);
            if (mListener != null) mListener.onStateChanged(state);
            switch (state) {
                case ViewDragHelper.STATE_IDLE:
                    if (mDecorView.getTop() == 0) {
                        // State Open
                        if (mListener != null) mListener.onOpened();
                    } else {
                        // State Closed
                        if (mListener != null) mListener.onClosed();
                    }
                    break;
                case ViewDragHelper.STATE_DRAGGING:

                    break;
                case ViewDragHelper.STATE_SETTLING:

                    break;
            }
        }
    };

    private final ViewDragHelper.Callback mBottomCallback = new ViewDragHelper.Callback() {
        @Override
        public boolean tryCaptureView(View child, int pointerId) {
            return child.getId() == mDecorView.getId() && (!mConfig.isEdgeOnly() || isEdgeTouched);
        }

        @Override
        public int clampViewPositionVertical(View child, int top, int dy) {
            return clamp(top, -mScreenHeight, 0);
        }

        @Override
        public int getViewVerticalDragRange(View child) {
            return mScreenHeight;
        }

        @Override
        public void onViewReleased(View releasedChild, float xvel, float yvel) {
            super.onViewReleased(releasedChild, xvel, yvel);

            int top = releasedChild.getTop();
            int settleTop = 0;
            int topThreshold = (int) (getHeight() * mConfig.getDistanceThreshold());
            boolean isSideSwiping = Math.abs(xvel) > mConfig.getVelocityThreshold();

            if (yvel < 0) {
                if (Math.abs(yvel) > mConfig.getVelocityThreshold() && !isSideSwiping) {
                    settleTop = -mScreenHeight;
                } else if (top < -topThreshold) {
                    settleTop = -mScreenHeight;
                }
            } else if (yvel == 0) {
                if (top < -topThreshold) {
                    settleTop = -mScreenHeight;
                }
            }

            mDragHelper.settleCapturedViewAt(releasedChild.getLeft(), settleTop);
            invalidate();
        }

        @Override
        public void onViewPositionChanged(View changedView, int left, int top, int dx, int dy) {
            super.onViewPositionChanged(changedView, left, top, dx, dy);
            float percent = 1f - ((float) Math.abs(top) / (float) mScreenHeight);

            if (mListener != null) mListener.onSlideChange(percent);

            // Update the dimmer alpha
            applyScrim(percent);
        }

        @Override
        public void onViewDragStateChanged(int state) {
            super.onViewDragStateChanged(state);
            if (mListener != null) mListener.onStateChanged(state);
            switch (state) {
                case ViewDragHelper.STATE_IDLE:
                    if (mDecorView.getTop() == 0) {
                        // State Open
                        if (mListener != null) mListener.onOpened();
                    } else {
                        // State Closed
                        if (mListener != null) mListener.onClosed();
                    }
                    break;
                case ViewDragHelper.STATE_DRAGGING:

                    break;
                case ViewDragHelper.STATE_SETTLING:

                    break;
            }
        }
    };

    private final ViewDragHelper.Callback mVerticalCallback = new ViewDragHelper.Callback() {
        @Override
        public boolean tryCaptureView(View child, int pointerId) {
            return child.getId() == mDecorView.getId() && (!mConfig.isEdgeOnly() || isEdgeTouched);
        }

        @Override
        public int clampViewPositionVertical(View child, int top, int dy) {
            return clamp(top, -mScreenHeight, mScreenHeight);
        }

        @Override
        public int getViewVerticalDragRange(View child) {
            return mScreenHeight;
        }

        @Override
        public void onViewReleased(View releasedChild, float xvel, float yvel) {
            super.onViewReleased(releasedChild, xvel, yvel);

            int top = releasedChild.getTop();
            int settleTop = 0;
            int topThreshold = (int) (getHeight() * mConfig.getDistanceThreshold());
            boolean isSideSwiping = Math.abs(xvel) > mConfig.getVelocityThreshold();

            if (yvel > 0) {

                // Being slinged down
                if (Math.abs(yvel) > mConfig.getVelocityThreshold() && !isSideSwiping) {
                    settleTop = mScreenHeight;
                } else if (top > topThreshold) {
                    settleTop = mScreenHeight;
                }

            } else if (yvel < 0) {
                // Being slinged up
                if (Math.abs(yvel) > mConfig.getVelocityThreshold() && !isSideSwiping) {
                    settleTop = -mScreenHeight;
                } else if (top < -topThreshold) {
                    settleTop = -mScreenHeight;
                }

            } else {

                if (top > topThreshold) {
                    settleTop = mScreenHeight;
                } else if (top < -topThreshold) {
                    settleTop = -mScreenHeight;
                }

            }

            mDragHelper.settleCapturedViewAt(releasedChild.getLeft(), settleTop);
            invalidate();
        }

        @Override
        public void onViewPositionChanged(View changedView, int left, int top, int dx, int dy) {
            super.onViewPositionChanged(changedView, left, top, dx, dy);
            float percent = 1f - ((float) Math.abs(top) / (float) mScreenHeight);

            if (mListener != null) mListener.onSlideChange(percent);

            // Update the dimmer alpha
            applyScrim(percent);
        }

        @Override
        public void onViewDragStateChanged(int state) {
            super.onViewDragStateChanged(state);
            if (mListener != null) mListener.onStateChanged(state);
            switch (state) {
                case ViewDragHelper.STATE_IDLE:
                    if (mDecorView.getTop() == 0) {
                        // State Open
                        if (mListener != null) mListener.onOpened();
                    } else {
                        // State Closed
                        if (mListener != null) mListener.onClosed();
                    }
                    break;
                case ViewDragHelper.STATE_DRAGGING:

                    break;
                case ViewDragHelper.STATE_SETTLING:

                    break;
            }
        }
    };

    private ViewDragHelper.Callback mHorizontalCallback = new ViewDragHelper.Callback() {
        @Override
        public boolean tryCaptureView(View child, int pointerId) {
            boolean edgeCase = !mConfig.isEdgeOnly() || mDragHelper.isEdgeTouched(mEdgePosition, pointerId);
            return child.getId() == mDecorView.getId() && edgeCase;
        }

        @Override
        public int clampViewPositionHorizontal(View child, int left, int dx) {
            return clamp(left, -mScreenWidth, mScreenWidth);
        }

        @Override
        public int getViewHorizontalDragRange(View child) {
            return mScreenWidth;
        }

        @Override
        public void onViewReleased(View releasedChild, float xvel, float yvel) {
            super.onViewReleased(releasedChild, xvel, yvel);

            int left = releasedChild.getLeft();
            int settleLeft = 0;
            int leftThreshold = (int) (getWidth() * mConfig.getDistanceThreshold());
            boolean isVerticalSwiping = Math.abs(yvel) > mConfig.getVelocityThreshold();

            if (xvel > 0) {

                if (Math.abs(xvel) > mConfig.getVelocityThreshold() && !isVerticalSwiping) {
                    settleLeft = mScreenWidth;
                } else if (left > leftThreshold) {
                    settleLeft = mScreenWidth;
                }

            } else if (xvel < 0) {

                if (Math.abs(xvel) > mConfig.getVelocityThreshold() && !isVerticalSwiping) {
                    settleLeft = -mScreenWidth;
                } else if (left < -leftThreshold) {
                    settleLeft = -mScreenWidth;
                }

            } else {
                if (left > leftThreshold) {
                    settleLeft = mScreenWidth;
                } else if (left < -leftThreshold) {
                    settleLeft = -mScreenWidth;
                }
            }

            mDragHelper.settleCapturedViewAt(settleLeft, releasedChild.getTop());
            invalidate();
        }

        @Override
        public void onViewPositionChanged(View changedView, int left, int top, int dx, int dy) {
            super.onViewPositionChanged(changedView, left, top, dx, dy);
            float percent = 1f - ((float) Math.abs(left) / (float) mScreenWidth);

            if (mListener != null) mListener.onSlideChange(percent);

            // Update the dimmer alpha
            applyScrim(percent);
        }

        @Override
        public void onViewDragStateChanged(int state) {
            super.onViewDragStateChanged(state);
            if (mListener != null) mListener.onStateChanged(state);
            switch (state) {
                case ViewDragHelper.STATE_IDLE:
                    if (mDecorView.getLeft() == 0) {
                        // State Open
                        if (mListener != null) mListener.onOpened();
                    } else {
                        // State Closed
                        if (mListener != null) mListener.onClosed();
                    }
                    break;
                case ViewDragHelper.STATE_DRAGGING:

                    break;
                case ViewDragHelper.STATE_SETTLING:

                    break;
            }
        }
    };

    public void applyScrim(float percent) {
        float alpha = (percent * (mConfig.getScrimStartAlpha() - mConfig.getScrimEndAlpha())) + mConfig.getScrimEndAlpha();
        mDimView.setAlpha(alpha);
    }

    protected int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    public void setOnLayoutSlideListener(OnLayoutSlideListener listener) {
        mListener = listener;
    }

    public interface OnLayoutSlideListener {
        void onStateChanged(int state);

        void onClosed();

        void onOpened();

        void onSlideChange(float percent);
    }
}
