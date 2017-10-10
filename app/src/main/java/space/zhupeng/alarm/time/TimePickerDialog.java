package space.zhupeng.alarm.time;

import android.animation.ObjectAnimator;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.ColorInt;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.v4.content.ContextCompat;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import space.zhupeng.alarm.R;

/**
 * Created by zhupeng on 2017/9/30.
 */

public class TimePickerDialog extends DialogFragment implements RadialPickerLayout.OnValueSelectedListener, TimePickerController {

    private static final String KEY_INITIAL_TIME = "initial_time";
    private static final String KEY_TITLE = "dialog_title";
    private static final String KEY_CURRENT_ITEM_SHOWING = "current_item_showing";
    private static final String KEY_IN_KB_MODE = "in_kb_mode";
    private static final String KEY_TYPED_TIMES = "typed_times";
    private static final String KEY_ACCENT = "accent";
    private static final String KEY_VIBRATE = "vibrate";
    private static final String KEY_DISMISS = "dismiss";
    private static final String KEY_ENABLE_MINUTES = "enable_minutes";
    private static final String KEY_OK_RESID = "ok_resid";
    private static final String KEY_OK_STRING = "ok_string";
    private static final String KEY_OK_COLOR = "ok_color";
    private static final String KEY_CANCEL_RESID = "cancel_resid";
    private static final String KEY_CANCEL_STRING = "cancel_string";
    private static final String KEY_CANCEL_COLOR = "cancel_color";
    private static final String KEY_TIMEPOINTLIMITER = "timepoint_limiter";

    public static final int HOUR_INDEX = 0;
    public static final int MINUTE_INDEX = 1;

    // Delay before starting the pulse animation, in ms.
    private static final int PULSE_ANIMATOR_DELAY = 300;

    private OnTimeSetListener mCallback;
    private DialogInterface.OnCancelListener mOnCancelListener;
    private DialogInterface.OnDismissListener mOnDismissListener;

    private HapticFeedbackController mHapticFeedbackController;

    private TextView mCancelButton;
    private TextView mOkButton;
    private TextView mHourView;
    private TextView mMinuteView;
    private RadialPickerLayout mTimePicker;

    private int mSelectedColor;
    private int mUnselectedColor;

    private boolean mAllowAutoAdvance;
    private Timepoint mInitialTime;
    private String mTitle;
    private boolean mVibrate;
    private int mAccentColor = -1;
    private boolean mDismissOnPause;
    private int mOkResid;
    private String mOkString;
    private int mOkColor;
    private int mCancelResid;
    private String mCancelString;
    private int mCancelColor;
    private DefaultTimepointLimiter mDefaultLimiter = new DefaultTimepointLimiter();
    private TimepointLimiter mLimiter = mDefaultLimiter;

    // For hardware IME input.
    private char mPlaceholderText;
    private String mDoublePlaceholderText;
    private String mDeletedKeyFormat;
    private boolean mInKbMode;
    private ArrayList<Integer> mTypedTimes;
    private Node mLegalTimesTree;

    // Accessibility strings.
    private String mHourPickerDescription;
    private String mSelectHours;
    private String mMinutePickerDescription;
    private String mSelectMinutes;

    /**
     * The callback interface used to indicate the user is done filling in
     * the time (they clicked on the 'Set' button).
     */
    public interface OnTimeSetListener {

        /**
         * @param view      The view associated with this listener.
         * @param hourOfDay The hour that was set.
         * @param minute    The minute that was set.
         */
        void onTimeSet(TimePickerDialog view, int hourOfDay, int minute);
    }

    public TimePickerDialog() {
        // Empty constructor required for dialog fragment.
    }

    public static TimePickerDialog newInstance(OnTimeSetListener callback, int hourOfDay, int minute) {
        TimePickerDialog ret = new TimePickerDialog();
        ret.initialize(callback, hourOfDay, minute);
        return ret;
    }

    @SuppressWarnings("unused")
    public static TimePickerDialog newInstance(OnTimeSetListener callback) {
        Calendar now = Calendar.getInstance();
        return TimePickerDialog.newInstance(callback, now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE));
    }

    public void initialize(OnTimeSetListener callback, int hourOfDay, int minute) {
        mCallback = callback;

        mInitialTime = new Timepoint(hourOfDay, minute);
        mInKbMode = false;
        mTitle = "";
        mAccentColor = -1;
        mVibrate = true;
        mDismissOnPause = false;
        mOkColor = -1;
        mCancelColor = -1;
    }

    /**
     * Set a title. NOTE: this will only take effect with the next onCreateView
     */
    public void setTitle(String title) {
        mTitle = title;
    }

    public String getTitle() {
        return mTitle;
    }

    /**
     * Set the accent color of this dialog
     *
     * @param color the accent color you want
     */
    @SuppressWarnings("unused")
    public void setAccentColor(String color) {
        mAccentColor = Color.parseColor(color);
    }

    /**
     * Set the accent color of this dialog
     *
     * @param color the accent color you want
     */
    public void setAccentColor(@ColorInt int color) {
        mAccentColor = Color.argb(255, Color.red(color), Color.green(color), Color.blue(color));
    }

    /**
     * Set the text color of the OK button
     *
     * @param color the color you want
     */
    @SuppressWarnings("unused")
    public void setOkColor(String color) {
        mOkColor = Color.parseColor(color);
    }

    /**
     * Set the text color of the OK button
     *
     * @param color the color you want
     */
    @SuppressWarnings("unused")
    public void setOkColor(@ColorInt int color) {
        mOkColor = Color.argb(255, Color.red(color), Color.green(color), Color.blue(color));
    }

    /**
     * Set the text color of the Cancel button
     *
     * @param color the color you want
     */
    @SuppressWarnings("unused")
    public void setCancelColor(String color) {
        mCancelColor = Color.parseColor(color);
    }

    /**
     * Set the text color of the Cancel button
     *
     * @param color the color you want
     */
    @SuppressWarnings("unused")
    public void setCancelColor(@ColorInt int color) {
        mCancelColor = Color.argb(255, Color.red(color), Color.green(color), Color.blue(color));
    }

    @Override
    public int getAccentColor() {
        return mAccentColor;
    }

    /**
     * Set whether the device should vibrate when touching fields
     *
     * @param vibrate true if the device should vibrate when touching a field
     */
    public void vibrate(boolean vibrate) {
        mVibrate = vibrate;
    }

    /**
     * Set whether the picker should dismiss itself when it's pausing or whether it should try to survive an orientation change
     *
     * @param dismissOnPause true if the picker should dismiss itself
     */
    public void dismissOnPause(boolean dismissOnPause) {
        mDismissOnPause = dismissOnPause;
    }

    @SuppressWarnings("unused")
    public void setMinTime(int hour, int minute) {
        setMinTime(new Timepoint(hour, minute));
    }

    public void setMinTime(Timepoint minTime) {
        mDefaultLimiter.setMinTime(minTime);
    }

    @SuppressWarnings("unused")
    public void setMaxTime(int hour, int minute) {
        setMaxTime(new Timepoint(hour, minute));
    }

    public void setMaxTime(Timepoint maxTime) {
        mDefaultLimiter.setMaxTime(maxTime);
    }

    /**
     * Pass in an array of Timepoints which are the only possible selections.
     * Try to specify Timepoints only up to the resolution of your picker (i.e. do not add seconds
     * if the resolution of the picker is minutes)
     *
     * @param selectableTimes Array of Timepoints which are the only valid selections in the picker
     */
    public void setSelectableTimes(Timepoint[] selectableTimes) {
        mDefaultLimiter.setSelectableTimes(selectableTimes);
    }

    /**
     * Pass in an array of Timepoints that cannot be selected. These take precedence over
     * {@link TimePickerDialog#setSelectableTimes(Timepoint[])}
     * Be careful when using this without selectableTimes: rounding to a valid Timepoint is a
     * very expensive operation if a lot of consecutive Timepoints are disabled
     * Try to specify Timepoints only up to the resolution of your picker (i.e. do not add seconds
     * if the resolution of the picker is minutes)
     *
     * @param disabledTimes Array of Timepoints which are disabled in the resulting picker
     */
    public void setDisabledTimes(Timepoint[] disabledTimes) {
        mDefaultLimiter.setDisabledTimes(disabledTimes);
    }

    /**
     * Set the interval for selectable times in the TimePickerDialog
     * This is a convenience wrapper around {@link TimePickerDialog#setSelectableTimes(Timepoint[])}
     * The interval for all three time components can be set independently
     * If you are not using the seconds / minutes picker, set the respective item to 60 for
     * better performance.
     *
     * @param hourInterval   The interval between 2 selectable hours ([1,24])
     * @param minuteInterval The interval between 2 selectable minutes ([1,60])
     */
    public void setTimeInterval(@IntRange(from = 1, to = 24) int hourInterval,
                                @IntRange(from = 1, to = 60) int minuteInterval) {
        List<Timepoint> timepoints = new ArrayList<>();

        int hour = 0;
        while (hour < 24) {
            int minute = 0;
            while (minute < 60) {
                timepoints.add(new Timepoint(hour, minute));
                minute += minuteInterval;
            }
            hour += hourInterval;
        }
        setSelectableTimes(timepoints.toArray(new Timepoint[timepoints.size()]));
    }

    /**
     * Set the interval for selectable times in the TimePickerDialog
     * This is a convenience wrapper around setSelectableTimes
     * The interval for all three time components can be set independently
     * If you are not using the seconds / minutes picker, set the respective item to 60 for
     * better performance.
     *
     * @param hourInterval The interval between 2 selectable hours ([1,24])
     */
    @SuppressWarnings("unused")
    public void setTimeInterval(@IntRange(from = 1, to = 24) int hourInterval) {
        setTimeInterval(hourInterval, 1);
    }

    public void setOnTimeSetListener(OnTimeSetListener callback) {
        mCallback = callback;
    }

    public void setOnCancelListener(DialogInterface.OnCancelListener onCancelListener) {
        mOnCancelListener = onCancelListener;
    }

    @SuppressWarnings("unused")
    public void setOnDismissListener(DialogInterface.OnDismissListener onDismissListener) {
        mOnDismissListener = onDismissListener;
    }

    /**
     * Set the time that will be shown when the picker opens for the first time
     * Overrides the value given in newInstance()
     *
     * @param hourOfDay the hour of the day
     * @param minute    the minute of the hour
     * @deprecated in favor of {@link #setInitialSelection(int, int)}
     */
    @Deprecated
    public void setStartTime(int hourOfDay, int minute) {
        mInitialTime = roundToNearest(new Timepoint(hourOfDay, minute));
        mInKbMode = false;
    }

    /**
     * Set the time that will be shown when the picker opens for the first time
     * Overrides the value given in newInstance()
     *
     * @param hourOfDay the hour of the day
     * @param minute    the minute of the hour
     */
    public void setInitialSelection(int hourOfDay, int minute) {
        setInitialSelection(new Timepoint(hourOfDay, minute));
    }


    /**
     * Set the time that will be shown when the picker opens for the first time
     * Overrides the value given in newInstance()
     *
     * @param time the Timepoint selected when the Dialog opens
     */
    public void setInitialSelection(Timepoint time) {
        mInitialTime = roundToNearest(time);
        mInKbMode = false;
    }

    /**
     * Set the label for the Ok button (max 12 characters)
     *
     * @param okString A literal String to be used as the Ok button label
     */
    @SuppressWarnings("unused")
    public void setOkText(String okString) {
        mOkString = okString;
    }

    /**
     * Set the label for the Ok button (max 12 characters)
     *
     * @param okResid A resource ID to be used as the Ok button label
     */
    @SuppressWarnings("unused")
    public void setOkText(@StringRes int okResid) {
        mOkString = null;
        mOkResid = okResid;
    }

    /**
     * Set the label for the Cancel button (max 12 characters)
     *
     * @param cancelString A literal String to be used as the Cancel button label
     */
    @SuppressWarnings("unused")
    public void setCancelText(String cancelString) {
        mCancelString = cancelString;
    }

    /**
     * Set the label for the Cancel button (max 12 characters)
     *
     * @param cancelResid A resource ID to be used as the Cancel button label
     */
    @SuppressWarnings("unused")
    public void setCancelText(@StringRes int cancelResid) {
        mCancelString = null;
        mCancelResid = cancelResid;
    }

    /**
     * Pass in a custom implementation of TimeLimiter
     * Disables setSelectableTimes, setDisabledTimes, setTimeInterval, setMinTime and setMaxTime
     *
     * @param limiter A custom implementation of TimeLimiter
     */
    @SuppressWarnings("unused")
    public void setTimepointLimiter(TimepointLimiter limiter) {
        mLimiter = limiter;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null && savedInstanceState.containsKey(KEY_INITIAL_TIME)) {
            mInitialTime = savedInstanceState.getParcelable(KEY_INITIAL_TIME);
            mInKbMode = savedInstanceState.getBoolean(KEY_IN_KB_MODE);
            mTitle = savedInstanceState.getString(KEY_TITLE);
            mAccentColor = savedInstanceState.getInt(KEY_ACCENT);
            mVibrate = savedInstanceState.getBoolean(KEY_VIBRATE);
            mDismissOnPause = savedInstanceState.getBoolean(KEY_DISMISS);
            mOkResid = savedInstanceState.getInt(KEY_OK_RESID);
            mOkString = savedInstanceState.getString(KEY_OK_STRING);
            mOkColor = savedInstanceState.getInt(KEY_OK_COLOR);
            mCancelResid = savedInstanceState.getInt(KEY_CANCEL_RESID);
            mCancelString = savedInstanceState.getString(KEY_CANCEL_STRING);
            mCancelColor = savedInstanceState.getInt(KEY_CANCEL_COLOR);
            mLimiter = savedInstanceState.getParcelable(KEY_TIMEPOINTLIMITER);

            /*
            If the user supplied a custom limiter, we need to create a new default one to prevent
            null pointer exceptions on the configuration methods
            If the user did not supply a custom limiter we need to ensure both mDefaultLimiter
            and mLimiter are the same reference, so that the config methods actually
            effect the behaviour of the picker (in the unlikely event the user reconfigures
            the picker when it is shown)
             */
            mDefaultLimiter = mLimiter instanceof DefaultTimepointLimiter
                    ? (DefaultTimepointLimiter) mLimiter
                    : new DefaultTimepointLimiter();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.dialog_time, container, false);
        KeyboardListener keyboardListener = new KeyboardListener();
        view.findViewById(R.id.dialog_time_picker).setOnKeyListener(keyboardListener);

        // If an accent color has not been set manually, get it from the context
        if (mAccentColor == -1) {
            mAccentColor = Utils.getAccentColorFromThemeIfAvailable(getActivity());
        }

        Resources res = getResources();
        Context context = getActivity();
        mHourPickerDescription = res.getString(R.string.hour_picker_description);
        mSelectHours = res.getString(R.string.select_hours);
        mMinutePickerDescription = res.getString(R.string.minute_picker_description);
        mSelectMinutes = res.getString(R.string.select_minutes);
        mSelectedColor = ContextCompat.getColor(context, android.R.color.white);
        mUnselectedColor = ContextCompat.getColor(context, R.color.text_color_unselected);

        mHourView = (TextView) view.findViewById(R.id.tv_hour);
        mHourView.setOnKeyListener(keyboardListener);
        mMinuteView = (TextView) view.findViewById(R.id.tv_minute);
        mMinuteView.setOnKeyListener(keyboardListener);

        mHapticFeedbackController = new HapticFeedbackController(getActivity());

        if (mTimePicker != null) {
            mInitialTime = new Timepoint(mTimePicker.getHours(), mTimePicker.getMinutes());
        }

        mInitialTime = roundToNearest(mInitialTime);

        mTimePicker = (RadialPickerLayout) view.findViewById(R.id.time_picker);
        mTimePicker.setOnValueSelectedListener(this);
        mTimePicker.setOnKeyListener(keyboardListener);
        mTimePicker.initialize(getActivity(), this, mInitialTime);

        int currentItemShowing = HOUR_INDEX;
        if (savedInstanceState != null &&
                savedInstanceState.containsKey(KEY_CURRENT_ITEM_SHOWING)) {
            currentItemShowing = savedInstanceState.getInt(KEY_CURRENT_ITEM_SHOWING);
        }
        setCurrentItemShowing(currentItemShowing, false, true, true);
        mTimePicker.invalidate();

        mHourView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setCurrentItemShowing(HOUR_INDEX, true, false, true);
                tryVibrate();
            }
        });
        mMinuteView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setCurrentItemShowing(MINUTE_INDEX, true, false, true);
                tryVibrate();
            }
        });

        mOkButton = (TextView) view.findViewById(R.id.btn_ok);
        mOkButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mInKbMode && isTypedTimeFullyLegal()) {
                    finishKbMode(false);
                } else {
                    tryVibrate();
                }
                notifyOnDateListener();
                dismiss();
            }
        });
        mOkButton.setOnKeyListener(keyboardListener);
        mOkButton.setTypeface(TypefaceHelper.get(context, "Roboto-Medium"));
        if (mOkString != null) mOkButton.setText(mOkString);

        mCancelButton = (TextView) view.findViewById(R.id.btn_cancel);
        mCancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tryVibrate();
                if (getDialog() != null) getDialog().cancel();
            }
        });
        mCancelButton.setTypeface(TypefaceHelper.get(context, "Roboto-Medium"));
        if (mCancelString != null) mCancelButton.setText(mCancelString);
        mCancelButton.setVisibility(isCancelable() ? View.VISIBLE : View.GONE);

        mAllowAutoAdvance = true;
        setHour(mInitialTime.getHour(), true);
        setMinute(mInitialTime.getMinute());

        // Set up for keyboard mode.
        mDoublePlaceholderText = res.getString(R.string.time_placeholder);
        mDeletedKeyFormat = res.getString(R.string.deleted_key);
        mPlaceholderText = mDoublePlaceholderText.charAt(0);
        generateLegalTimesTree();
        if (mInKbMode) {
            mTypedTimes = savedInstanceState.getIntegerArrayList(KEY_TYPED_TIMES);
            tryStartingKbMode(-1);
            mHourView.invalidate();
        } else if (mTypedTimes == null) {
            mTypedTimes = new ArrayList<>();
        }

        // Set the title (if any)
        TextView timePickerHeader = (TextView) view.findViewById(R.id.tv_picker_header);
        if (!mTitle.isEmpty()) {
            timePickerHeader.setVisibility(TextView.VISIBLE);
            timePickerHeader.setText(mTitle.toUpperCase(Locale.getDefault()));
        }

        // Set the theme at the end so that the initialize()s above don't counteract the theme.
        timePickerHeader.setBackgroundColor(Utils.darkenColor(mAccentColor));
        view.findViewById(R.id.ll_picker).setBackgroundColor(mAccentColor);
        view.findViewById(R.id.ll_time_display).setBackgroundColor(mAccentColor);

        // Button text can have a different color
        if (mOkColor != -1) mOkButton.setTextColor(mOkColor);
        else mOkButton.setTextColor(mAccentColor);
        if (mCancelColor != -1) mCancelButton.setTextColor(mCancelColor);
        else mCancelButton.setTextColor(mAccentColor);

        if (getDialog() == null) {
            view.findViewById(R.id.ll_picker_bottom).setVisibility(View.GONE);
        }

        int circleBackground = ContextCompat.getColor(context, android.R.color.white);
        int backgroundColor = ContextCompat.getColor(context, android.R.color.white);

        mTimePicker.setBackgroundColor(circleBackground);
        view.findViewById(R.id.dialog_time_picker).setBackgroundColor(backgroundColor);
        return view;
    }

    @Override
    public void onConfigurationChanged(final Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        ViewGroup viewGroup = (ViewGroup) getView();
        if (viewGroup != null) {
            viewGroup.removeAllViewsInLayout();
            View view = onCreateView(getActivity().getLayoutInflater(), viewGroup, null);
            viewGroup.addView(view);
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        return dialog;
    }

    @Override
    public void onResume() {
        super.onResume();
        mHapticFeedbackController.start();
    }

    @Override
    public void onPause() {
        super.onPause();
        mHapticFeedbackController.stop();
        if (mDismissOnPause) dismiss();
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);
        if (mOnCancelListener != null) mOnCancelListener.onCancel(dialog);
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        if (mOnDismissListener != null) mOnDismissListener.onDismiss(dialog);
    }

    @Override
    public void tryVibrate() {
        if (mVibrate) mHapticFeedbackController.tryVibrate();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        if (mTimePicker != null) {
            outState.putParcelable(KEY_INITIAL_TIME, mTimePicker.getTime());
            outState.putInt(KEY_CURRENT_ITEM_SHOWING, mTimePicker.getCurrentItemShowing());
            outState.putBoolean(KEY_IN_KB_MODE, mInKbMode);
            if (mInKbMode) {
                outState.putIntegerArrayList(KEY_TYPED_TIMES, mTypedTimes);
            }
            outState.putString(KEY_TITLE, mTitle);
            outState.putInt(KEY_ACCENT, mAccentColor);
            outState.putBoolean(KEY_VIBRATE, mVibrate);
            outState.putBoolean(KEY_DISMISS, mDismissOnPause);
            outState.putInt(KEY_OK_RESID, mOkResid);
            outState.putString(KEY_OK_STRING, mOkString);
            outState.putInt(KEY_OK_COLOR, mOkColor);
            outState.putInt(KEY_CANCEL_RESID, mCancelResid);
            outState.putString(KEY_CANCEL_STRING, mCancelString);
            outState.putInt(KEY_CANCEL_COLOR, mCancelColor);
            outState.putParcelable(KEY_TIMEPOINTLIMITER, mLimiter);
        }
    }

    /**
     * Called by the picker for updating the header display.
     */
    @Override
    public void onValueSelected(Timepoint newValue) {
        setHour(newValue.getHour(), false);
        mTimePicker.setContentDescription(mHourPickerDescription + ": " + newValue.getHour());
        setMinute(newValue.getMinute());
        mTimePicker.setContentDescription(mMinutePickerDescription + ": " + newValue.getMinute());
    }

    @Override
    public void advancePicker(int index) {
        if (!mAllowAutoAdvance) return;
        if (index == HOUR_INDEX) {
            setCurrentItemShowing(MINUTE_INDEX, true, true, false);

            String announcement = mSelectHours + ". " + mTimePicker.getMinutes();
            Utils.tryAccessibilityAnnounce(mTimePicker, announcement);
        }
    }

    @Override
    public void enablePicker() {
        if (!isTypedTimeFullyLegal()) mTypedTimes.clear();
        finishKbMode(true);
    }


    @Override
    public boolean isOutOfRange(Timepoint current, int index) {
        return mLimiter.isOutOfRange(current, index, Timepoint.MINUTE);
    }

    /**
     * Round a given Timepoint to the nearest valid Timepoint
     *
     * @param time Timepoint - The timepoint to round
     * @return Timepoint - The nearest valid Timepoint
     */
    private Timepoint roundToNearest(@NonNull Timepoint time) {
        return roundToNearest(time, -1);
    }

    @Override
    public Timepoint roundToNearest(@NonNull Timepoint time, @Timepoint.TYPE int type) {
        return mLimiter.roundToNearest(time, type, Timepoint.MINUTE);
    }

    private void setHour(int value, boolean announce) {
        final String format = "%02d";

        CharSequence text = String.format(format, value);
        mHourView.setText(text);
        if (announce) {
            Utils.tryAccessibilityAnnounce(mTimePicker, text);
        }
    }

    private void setMinute(int value) {
        if (value == 60) {
            value = 0;
        }
        CharSequence text = String.format(Locale.getDefault(), "%02d", value);
        Utils.tryAccessibilityAnnounce(mTimePicker, text);
        mMinuteView.setText(text);
    }

    // Show either Hours or Minutes.
    private void setCurrentItemShowing(int index, boolean animateCircle, boolean delayLabelAnimate,
                                       boolean announce) {
        mTimePicker.setCurrentItemShowing(index, animateCircle);

        TextView labelToAnimate = null;
        switch (index) {
            case HOUR_INDEX:
                int hours = mTimePicker.getHours();
                mTimePicker.setContentDescription(mHourPickerDescription + ": " + hours);
                if (announce) {
                    Utils.tryAccessibilityAnnounce(mTimePicker, mSelectHours);
                }
                labelToAnimate = mHourView;
                break;
            case MINUTE_INDEX:
                int minutes = mTimePicker.getMinutes();
                mTimePicker.setContentDescription(mMinutePickerDescription + ": " + minutes);
                if (announce) {
                    Utils.tryAccessibilityAnnounce(mTimePicker, mSelectMinutes);
                }
                labelToAnimate = mMinuteView;
                break;
            default:
        }

        int hourColor = (index == HOUR_INDEX) ? mSelectedColor : mUnselectedColor;
        int minuteColor = (index == MINUTE_INDEX) ? mSelectedColor : mUnselectedColor;
        mHourView.setTextColor(hourColor);
        mMinuteView.setTextColor(minuteColor);

        ObjectAnimator pulseAnimator = Utils.getPulseAnimator(labelToAnimate, 0.85f, 1.1f);
        if (delayLabelAnimate) {
            pulseAnimator.setStartDelay(PULSE_ANIMATOR_DELAY);
        }
        pulseAnimator.start();
    }

    /**
     * For keyboard mode, processes key events.
     *
     * @param keyCode the pressed key.
     * @return true if the key was successfully processed, false otherwise.
     */
    private boolean processKeyUp(int keyCode) {
        if (keyCode == KeyEvent.KEYCODE_ESCAPE || keyCode == KeyEvent.KEYCODE_BACK) {
            if (isCancelable()) dismiss();
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_TAB) {
            if (mInKbMode) {
                if (isTypedTimeFullyLegal()) {
                    finishKbMode(true);
                }
                return true;
            }
        } else if (keyCode == KeyEvent.KEYCODE_ENTER) {
            if (mInKbMode) {
                if (!isTypedTimeFullyLegal()) {
                    return true;
                }
                finishKbMode(false);
            }
            if (mCallback != null) {
                mCallback.onTimeSet(this, mTimePicker.getHours(), mTimePicker.getMinutes());
            }
            dismiss();
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_DEL) {
            if (mInKbMode) {
                if (!mTypedTimes.isEmpty()) {
                    int deleted = deleteLastTypedKey();
                    String deletedKeyStr = String.format("%d", getValFromKeyCode(deleted));
                    Utils.tryAccessibilityAnnounce(mTimePicker,
                            String.format(mDeletedKeyFormat, deletedKeyStr));
                    updateDisplay(true);
                }
            }
        } else if (keyCode == KeyEvent.KEYCODE_0 || keyCode == KeyEvent.KEYCODE_1
                || keyCode == KeyEvent.KEYCODE_2 || keyCode == KeyEvent.KEYCODE_3
                || keyCode == KeyEvent.KEYCODE_4 || keyCode == KeyEvent.KEYCODE_5
                || keyCode == KeyEvent.KEYCODE_6 || keyCode == KeyEvent.KEYCODE_7
                || keyCode == KeyEvent.KEYCODE_8 || keyCode == KeyEvent.KEYCODE_9) {
            if (!mInKbMode) {
                if (mTimePicker == null) {
                    // Something's wrong, because time picker should definitely not be null.
                    return true;
                }
                mTypedTimes.clear();
                tryStartingKbMode(keyCode);
                return true;
            }
            // We're already in keyboard mode.
            if (addKeyIfLegal(keyCode)) {
                updateDisplay(false);
            }
            return true;
        }
        return false;
    }

    /**
     * Try to start keyboard mode with the specified key, as long as the timepicker is not in the
     * middle of a touch-event.
     *
     * @param keyCode The key to use as the first press. Keyboard mode will not be started if the
     *                key is not legal to start with. Or, pass in -1 to get into keyboard mode without a starting
     *                key.
     */
    private void tryStartingKbMode(int keyCode) {
        if (mTimePicker.trySettingInputEnabled(false) &&
                (keyCode == -1 || addKeyIfLegal(keyCode))) {
            mInKbMode = true;
            mOkButton.setEnabled(false);
            updateDisplay(false);
        }
    }

    private boolean addKeyIfLegal(int keyCode) {
        // If we're in 24hour mode, we'll need to check if the input is full. If in AM/PM mode,
        int textSize = 4;
        if (mTypedTimes.size() == textSize) {
            return false;
        }

        mTypedTimes.add(keyCode);
        if (!isTypedTimeLegalSoFar()) {
            deleteLastTypedKey();
            return false;
        }

        int val = getValFromKeyCode(keyCode);
        Utils.tryAccessibilityAnnounce(mTimePicker, String.format(Locale.getDefault(), "%d", val));
        // Automatically fill in 0's if AM or PM was legally entered.
        if (isTypedTimeFullyLegal()) {
            mOkButton.setEnabled(true);
        }

        return true;
    }

    /**
     * Traverse the tree to see if the keys that have been typed so far are legal as is,
     * or may become legal as more keys are typed (excluding backspace).
     */
    private boolean isTypedTimeLegalSoFar() {
        Node node = mLegalTimesTree;
        for (int keyCode : mTypedTimes) {
            node = node.canReach(keyCode);
            if (node == null) {
                return false;
            }
        }
        return true;
    }

    /**
     * Check if the time that has been typed so far is completely legal, as is.
     */
    private boolean isTypedTimeFullyLegal() {
        // For 24-hour mode, the time is legal if the hours and minutes are each legal. Note:
        // getEnteredTime() will ONLY call isTypedTimeFullyLegal() when NOT in 24hour mode.
        int[] values = getEnteredTime(null);
        return (values[0] >= 0 && values[1] >= 0 && values[1] < 60 && values[2] >= 0 && values[2] < 60);
    }

    private int deleteLastTypedKey() {
        int deleted = mTypedTimes.remove(mTypedTimes.size() - 1);
        if (!isTypedTimeFullyLegal()) {
            mOkButton.setEnabled(false);
        }
        return deleted;
    }

    /**
     * Get out of keyboard mode. If there is nothing in typedTimes, revert to TimePicker's time.
     *
     * @param updateDisplays If true, update the displays with the relevant time.
     */
    private void finishKbMode(boolean updateDisplays) {
        mInKbMode = false;
        if (!mTypedTimes.isEmpty()) {
            int values[] = getEnteredTime(null);
            mTimePicker.setTime(new Timepoint(values[0], values[1]));
            mTypedTimes.clear();
        }
        if (updateDisplays) {
            updateDisplay(false);
            mTimePicker.trySettingInputEnabled(true);
        }
    }

    /**
     * Update the hours, minutes, seconds and AM/PM displays with the typed times. If the typedTimes
     * is empty, either show an empty display (filled with the placeholder text), or update from the
     * timepicker's values.
     *
     * @param allowEmptyDisplay if true, then if the typedTimes is empty, use the placeholder text.
     *                          Otherwise, revert to the timepicker's values.
     */
    private void updateDisplay(boolean allowEmptyDisplay) {
        if (!allowEmptyDisplay && mTypedTimes.isEmpty()) {
            int hour = mTimePicker.getHours();
            int minute = mTimePicker.getMinutes();
            setHour(hour, true);
            setMinute(minute);
            setCurrentItemShowing(mTimePicker.getCurrentItemShowing(), true, true, true);
            mOkButton.setEnabled(true);
        } else {
            Boolean[] enteredZeros = {false, false, false};
            int[] values = getEnteredTime(enteredZeros);
            String hourFormat = enteredZeros[0] ? "%02d" : "%2d";
            String minuteFormat = (enteredZeros[1]) ? "%02d" : "%2d";
            String hourStr = (values[0] == -1) ? mDoublePlaceholderText :
                    String.format(hourFormat, values[0]).replace(' ', mPlaceholderText);
            String minuteStr = (values[1] == -1) ? mDoublePlaceholderText :
                    String.format(minuteFormat, values[1]).replace(' ', mPlaceholderText);
            mHourView.setText(hourStr);
            mHourView.setTextColor(mUnselectedColor);
            mMinuteView.setText(minuteStr);
            mMinuteView.setTextColor(mUnselectedColor);
        }
    }

    private static int getValFromKeyCode(int keyCode) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_0:
                return 0;
            case KeyEvent.KEYCODE_1:
                return 1;
            case KeyEvent.KEYCODE_2:
                return 2;
            case KeyEvent.KEYCODE_3:
                return 3;
            case KeyEvent.KEYCODE_4:
                return 4;
            case KeyEvent.KEYCODE_5:
                return 5;
            case KeyEvent.KEYCODE_6:
                return 6;
            case KeyEvent.KEYCODE_7:
                return 7;
            case KeyEvent.KEYCODE_8:
                return 8;
            case KeyEvent.KEYCODE_9:
                return 9;
            default:
                return -1;
        }
    }

    /**
     * Get the currently-entered time, as integer values of the hours, minutes and seconds typed.
     *
     * @param enteredZeros A size-2 boolean array, which the caller should initialize, and which
     *                     may then be used for the caller to know whether zeros had been explicitly entered as either
     *                     hours of minutes. This is helpful for deciding whether to show the dashes, or actual 0's.
     * @return A size-3 int array. The first value will be the hours, the second value will be the
     * minutes, and the third will be either TimePickerDialog.AM or TimePickerDialog.PM.
     */
    private int[] getEnteredTime(Boolean[] enteredZeros) {
        int startIndex = 1;
        int minute = -1;
        int hour = -1;
        for (int i = startIndex; i <= mTypedTimes.size(); i++) {
            int val = getValFromKeyCode(mTypedTimes.get(mTypedTimes.size() - i));
            if (i == startIndex) {
                minute = val;
            } else if (i == startIndex + 1) {
                minute += 10 * val;
                if (enteredZeros != null && val == 0) {
                    enteredZeros[1] = true;
                }
            } else if (i == startIndex + 2) {
                hour = val;
            } else if (i == startIndex + 3) {
                hour += 10 * val;
                if (enteredZeros != null && val == 0) {
                    enteredZeros[0] = true;
                }
            }
        }

        return new int[]{hour, minute};
    }

    /**
     * Create a tree for deciding what keys can legally be typed.
     */
    private void generateLegalTimesTree() {
        // Create a quick cache of numbers to their keycodes.
        int k0 = KeyEvent.KEYCODE_0;
        int k1 = KeyEvent.KEYCODE_1;
        int k2 = KeyEvent.KEYCODE_2;
        int k3 = KeyEvent.KEYCODE_3;
        int k4 = KeyEvent.KEYCODE_4;
        int k5 = KeyEvent.KEYCODE_5;
        int k6 = KeyEvent.KEYCODE_6;
        int k7 = KeyEvent.KEYCODE_7;
        int k8 = KeyEvent.KEYCODE_8;
        int k9 = KeyEvent.KEYCODE_9;

        // The root of the tree doesn't contain any numbers.
        mLegalTimesTree = new Node();

        // We'll be re-using these nodes, so we'll save them.
        Node minuteFirstDigit = new Node(k0, k1, k2, k3, k4, k5);
        Node minuteSecondDigit = new Node(k0, k1, k2, k3, k4, k5, k6, k7, k8, k9);
        // The first digit must be followed by the second digit.
        minuteFirstDigit.addChild(minuteSecondDigit);

        // The first digit may be 0-1.
        Node firstDigit = new Node(k0, k1);
        mLegalTimesTree.addChild(firstDigit);

        // When the first digit is 0-1, the second digit may be 0-5.
        Node secondDigit = new Node(k0, k1, k2, k3, k4, k5);
        firstDigit.addChild(secondDigit);
        // We may now be followed by the first minute digit. E.g. 00:09, 15:58.
        secondDigit.addChild(minuteFirstDigit);

        // When the first digit is 0-1, and the second digit is 0-5, the third digit may be 6-9.
        Node thirdDigit = new Node(k6, k7, k8, k9);
        // The time must now be finished. E.g. 0:55, 1:08.
        secondDigit.addChild(thirdDigit);

        // When the first digit is 0-1, the second digit may be 6-9.
        secondDigit = new Node(k6, k7, k8, k9);
        firstDigit.addChild(secondDigit);
        // We must now be followed by the first minute digit. E.g. 06:50, 18:20.
        secondDigit.addChild(minuteFirstDigit);

        // The first digit may be 2.
        firstDigit = new Node(k2);
        mLegalTimesTree.addChild(firstDigit);

        // When the first digit is 2, the second digit may be 0-3.
        secondDigit = new Node(k0, k1, k2, k3);
        firstDigit.addChild(secondDigit);
        // We must now be followed by the first minute digit. E.g. 20:50, 23:09.
        secondDigit.addChild(minuteFirstDigit);

        // When the first digit is 2, the second digit may be 4-5.
        secondDigit = new Node(k4, k5);
        firstDigit.addChild(secondDigit);
        // We must now be followd by the last minute digit. E.g. 2:40, 2:53.
        secondDigit.addChild(minuteSecondDigit);

        // The first digit may be 3-9.
        firstDigit = new Node(k3, k4, k5, k6, k7, k8, k9);
        mLegalTimesTree.addChild(firstDigit);
        // We must now be followed by the first minute digit. E.g. 3:57, 8:12.
        firstDigit.addChild(minuteFirstDigit);
    }

    /**
     * Simple node class to be used for traversal to check for legal times.
     * mLegalKeys represents the keys that can be typed to get to the node.
     * mChildren are the children that can be reached from this node.
     */
    private static class Node {
        private int[] mLegalKeys;
        private ArrayList<Node> mChildren;

        public Node(int... legalKeys) {
            mLegalKeys = legalKeys;
            mChildren = new ArrayList<>();
        }

        public void addChild(Node child) {
            mChildren.add(child);
        }

        public boolean containsKey(int key) {
            for (int legalKey : mLegalKeys) {
                if (legalKey == key) return true;
            }
            return false;
        }

        public Node canReach(int key) {
            if (mChildren == null) {
                return null;
            }
            for (Node child : mChildren) {
                if (child.containsKey(key)) {
                    return child;
                }
            }
            return null;
        }
    }

    private class KeyboardListener implements View.OnKeyListener {
        @Override
        public boolean onKey(View v, int keyCode, KeyEvent event) {
            if (event.getAction() == KeyEvent.ACTION_UP) {
                return processKeyUp(keyCode);
            }
            return false;
        }
    }

    public void notifyOnDateListener() {
        if (mCallback != null) {
            mCallback.onTimeSet(this, mTimePicker.getHours(), mTimePicker.getMinutes());
        }
    }

    public Timepoint getSelectedTime() {
        return mTimePicker.getTime();
    }
}

