package space.zhupeng.alarm.time;

/**
 * A collection of methods which need to be shared with all components of the TimePicker
 */
interface TimePickerController {
    /**
     * @return int - the accent color currently in use
     */
    int getAccentColor();

    /**
     * Request the device to vibrate
     */
    void tryVibrate();

    /**
     * @param time  Timepoint - the selected point in time
     * @param index int - The current view to consider when calculating the range
     * @return boolean - true if this is not a selectable value
     */
    boolean isOutOfRange(Timepoint time, int index);

    /**
     * Will round the given Timepoint to the nearest valid Timepoint given the following restrictions:
     * - TYPE.HOUR, it will just round to the next valid point, possible adjusting minutes and seconds
     * - TYPE.MINUTE, it will round to the next valid point, without adjusting the hour, but possibly adjusting the seconds
     *
     * @param time Timepoint - the timepoint to validate
     * @param type int - whether we should round the hours, minutes
     * @return timepoint - the nearest valid timepoint
     */
    Timepoint roundToNearest(Timepoint time, @Timepoint.TYPE int type);
}
