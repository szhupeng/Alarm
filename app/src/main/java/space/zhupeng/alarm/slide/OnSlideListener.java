package space.zhupeng.alarm.slide;

public interface OnSlideListener {
    void onSlideStateChanged(int state);

    void onSlideChange(float percent);

    void onSlideOpened();

    void onSlideClosed();
}
