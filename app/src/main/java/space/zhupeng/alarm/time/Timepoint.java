package space.zhupeng.alarm.time;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.IntDef;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;


/**
 * Simple utility class that represents a time
 * The time input is expected to use 24 hour mode.
 * Fields are modulo'd into their correct ranges.
 * It does not handle timezones.
 */
@SuppressWarnings("WeakerAccess")
public class Timepoint implements Parcelable, Comparable<Timepoint> {
    public static final int HOUR = 0;
    public static final int MINUTE = 1;

    private int hour;
    private int minute;

    @IntDef({HOUR, MINUTE})
    @Retention(RetentionPolicy.SOURCE)
    public @interface TYPE {
    }

    public Timepoint(Timepoint time) {
        this(time.hour, time.minute);
    }

    public Timepoint(@IntRange(from = 0, to = 23) int hour,
                     @IntRange(from = 0, to = 59) int minute) {
        this.hour = hour % 24;
        this.minute = minute % 60;
    }

    public Timepoint(@IntRange(from = 0, to = 23) int hour) {
        this(hour, 0);
    }

    public Timepoint(Parcel in) {
        hour = in.readInt();
        minute = in.readInt();
    }

    @IntRange(from = 0, to = 23)
    public int getHour() {
        return hour;
    }

    @IntRange(from = 0, to = 59)
    public int getMinute() {
        return minute;
    }

    public void add(@TYPE int type, int value) {
        if (type == MINUTE) value *= 60;
        if (type == HOUR) value *= 3600;
        value += toSeconds();

        switch (type) {
            case MINUTE:
                minute = (value % 3600) / 60;
            case HOUR:
                hour = (value / 3600) % 24;
        }
    }

    public int get(@TYPE int type) {
        if (MINUTE == type) {
            return getMinute();
        } else {
            return getHour();
        }
    }

    public int toSeconds() {
        return 3600 * hour + 60 * minute;
    }

    @Override
    public int hashCode() {
        return toSeconds();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Timepoint timepoint = (Timepoint) o;

        return hashCode() == timepoint.hashCode();
    }

    public boolean equals(@Nullable Timepoint time, @TYPE int resolution) {
        if (time == null) return false;
        boolean output = true;
        switch (resolution) {
            case MINUTE:
                output = output && time.getMinute() == getMinute();
            case HOUR:
                output = output && time.getHour() == getHour();
        }
        return output;
    }

    @Override
    public int compareTo(@NonNull Timepoint t) {
        return hashCode() - t.hashCode();
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeInt(hour);
        out.writeInt(minute);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<Timepoint> CREATOR = new Creator<Timepoint>() {
        public Timepoint createFromParcel(Parcel in) {
            return new Timepoint(in);
        }

        public Timepoint[] newArray(int size) {
            return new Timepoint[size];
        }
    };
}
