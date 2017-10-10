package space.zhupeng.alarm.slide;

import android.animation.ArgbEvaluator;
import android.annotation.TargetApi;
import android.app.Activity;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;

import space.zhupeng.alarm.R;

public final class SlideHandler {

    private SlideHandler() {
    }

    public static Slide attach(Activity activity) {
        return attach(activity, -1, -1);
    }

    public static Slide attach(final Activity activity, final int startStatusBarColor, final int endStatusBarColor) {

        final SlideLayout layout = initSlideLayout(activity, null);
        layout.setOnLayoutSlideListener(new SlideLayout.OnLayoutSlideListener() {

            private final ArgbEvaluator mEvaluator = new ArgbEvaluator();

            @Override
            public void onStateChanged(int state) {

            }

            @Override
            public void onClosed() {
                activity.finish();
                activity.overridePendingTransition(0, 0);
            }

            @Override
            public void onOpened() {

            }

            @TargetApi(Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void onSlideChange(float percent) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP &&
                        startStatusBarColor != -1 && endStatusBarColor != -1) {
                    int newColor = (int) mEvaluator.evaluate(percent, startStatusBarColor, endStatusBarColor);
                    activity.getWindow().setStatusBarColor(newColor);
                }
            }
        });

        return init(layout);
    }

    public static Slide attach(final Activity activity, final SlideConfig config) {

        final SlideLayout layout = initSlideLayout(activity, config);
        layout.setOnLayoutSlideListener(new SlideLayout.OnLayoutSlideListener() {

            private final ArgbEvaluator mEvaluator = new ArgbEvaluator();

            @Override
            public void onStateChanged(int state) {
                if (config.getListener() != null) {
                    config.getListener().onSlideStateChanged(state);
                }
            }

            @Override
            public void onClosed() {
                if (config.getListener() != null) {
                    config.getListener().onSlideClosed();
                }

                activity.finish();
                activity.overridePendingTransition(0, 0);
            }

            @Override
            public void onOpened() {
                if (config.getListener() != null) {
                    config.getListener().onSlideOpened();
                }
            }

            @TargetApi(Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void onSlideChange(float percent) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP &&
                        config.areStatusBarColorsValid()) {

                    int newColor = (int) mEvaluator.evaluate(percent, config.getPrimaryColor(),
                            config.getSecondaryColor());

                    activity.getWindow().setStatusBarColor(newColor);
                }

                if (config.getListener() != null) {
                    config.getListener().onSlideChange(percent);
                }
            }
        });

        return init(layout);
    }

    private static SlideLayout initSlideLayout(final Activity activity, final SlideConfig config) {

        ViewGroup decorView = (ViewGroup) activity.getWindow().getDecorView();
        View firstChild = decorView.getChildAt(0);
        decorView.removeViewAt(0);

        SlideLayout layout = new SlideLayout(activity, firstChild, config);
        layout.setId(R.id.slidable_layout);
        firstChild.setId(R.id.slidable_content);
        layout.addView(firstChild);
        decorView.addView(layout, 0);
        return layout;
    }

    private static Slide init(final SlideLayout layout) {
        Slide slide = new Slide() {
            @Override
            public void lock() {
                layout.lock();
            }

            @Override
            public void unlock() {
                layout.unlock();
            }
        };

        return slide;
    }
}
