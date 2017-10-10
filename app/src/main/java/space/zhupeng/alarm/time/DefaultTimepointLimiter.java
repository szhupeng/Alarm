package space.zhupeng.alarm.time;

import android.os.Parcel;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.Arrays;
import java.util.TreeSet;

import static space.zhupeng.alarm.time.TimePickerDialog.HOUR_INDEX;
import static space.zhupeng.alarm.time.TimePickerDialog.MINUTE_INDEX;

/**
 * An implementation of TimepointLimiter which implements the most common ways to restrict Timepoints
 * in a TimePickerDialog
 */

class DefaultTimepointLimiter implements TimepointLimiter {
    private TreeSet<Timepoint> mSelectableTimes = new TreeSet<>();
    private TreeSet<Timepoint> mDisabledTimes = new TreeSet<>();
    private TreeSet<Timepoint> exclusiveSelectableTimes = new TreeSet<>();
    private Timepoint mMinTime;
    private Timepoint mMaxTime;

    DefaultTimepointLimiter() {
    }

    @SuppressWarnings("WeakerAccess")
    public DefaultTimepointLimiter(Parcel in) {
        mMinTime = in.readParcelable(Timepoint.class.getClassLoader());
        mMaxTime = in.readParcelable(Timepoint.class.getClassLoader());
        mSelectableTimes.addAll(Arrays.asList((Timepoint[]) in.readParcelableArray(Timepoint[].class.getClassLoader())));
        mDisabledTimes.addAll(Arrays.asList((Timepoint[]) in.readParcelableArray(Timepoint[].class.getClassLoader())));
        exclusiveSelectableTimes = getExclusiveSelectableTimes(mSelectableTimes, mDisabledTimes);
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeParcelable(mMinTime, flags);
        out.writeParcelable(mMaxTime, flags);
        out.writeParcelableArray((Timepoint[]) mSelectableTimes.toArray(), flags);
        out.writeParcelableArray((Timepoint[]) mDisabledTimes.toArray(), flags);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<DefaultTimepointLimiter> CREATOR
            = new Creator<DefaultTimepointLimiter>() {
        public DefaultTimepointLimiter createFromParcel(Parcel in) {
            return new DefaultTimepointLimiter(in);
        }

        public DefaultTimepointLimiter[] newArray(int size) {
            return new DefaultTimepointLimiter[size];
        }
    };

    void setMinTime(@NonNull Timepoint minTime) {
        if (mMaxTime != null && minTime.compareTo(mMaxTime) > 0)
            throw new IllegalArgumentException("Minimum time must be smaller than the maximum time");
        mMinTime = minTime;
    }

    void setMaxTime(@NonNull Timepoint maxTime) {
        if (mMinTime != null && maxTime.compareTo(mMinTime) < 0)
            throw new IllegalArgumentException("Maximum time must be greater than the minimum time");
        mMaxTime = maxTime;
    }

    void setSelectableTimes(@NonNull Timepoint[] selectableTimes) {
        mSelectableTimes.addAll(Arrays.asList(selectableTimes));
        exclusiveSelectableTimes = getExclusiveSelectableTimes(mSelectableTimes, mDisabledTimes);
    }

    void setDisabledTimes(@NonNull Timepoint[] disabledTimes) {
        mDisabledTimes.addAll(Arrays.asList(disabledTimes));
        exclusiveSelectableTimes = getExclusiveSelectableTimes(mSelectableTimes, mDisabledTimes);
    }

    private TreeSet<Timepoint> getExclusiveSelectableTimes(TreeSet<Timepoint> selectable, TreeSet<Timepoint> disabled) {
        TreeSet<Timepoint> output = (TreeSet<Timepoint>) selectable.clone();
        output.removeAll(disabled);
        return output;
    }

    @Override
    public boolean isOutOfRange(@Nullable Timepoint current, int index, @Timepoint.TYPE int resolution) {
        if (current == null) return false;

        if (index == HOUR_INDEX) {
            if (mMinTime != null && mMinTime.getHour() > current.getHour()) return true;

            if (mMaxTime != null && mMaxTime.getHour() + 1 <= current.getHour()) return true;

            if (!exclusiveSelectableTimes.isEmpty()) {
                Timepoint ceil = exclusiveSelectableTimes.ceiling(current);
                Timepoint floor = exclusiveSelectableTimes.floor(current);
                return !(current.equals(ceil, Timepoint.HOUR) || current.equals(floor, Timepoint.HOUR));
            }

            if (!mDisabledTimes.isEmpty() && resolution == Timepoint.HOUR) {
                Timepoint ceil = mDisabledTimes.ceiling(current);
                Timepoint floor = mDisabledTimes.floor(current);
                return current.equals(ceil, Timepoint.HOUR) || current.equals(floor, Timepoint.HOUR);
            }

            return false;
        } else if (index == MINUTE_INDEX) {
            if (mMinTime != null) {
                Timepoint roundedMin = new Timepoint(mMinTime.getHour(), mMinTime.getMinute());
                if (roundedMin.compareTo(current) > 0) return true;
            }

            if (mMaxTime != null) {
                Timepoint roundedMax = new Timepoint(mMaxTime.getHour(), mMaxTime.getMinute());
                if (roundedMax.compareTo(current) < 0) return true;
            }

            if (!exclusiveSelectableTimes.isEmpty()) {
                Timepoint ceil = exclusiveSelectableTimes.ceiling(current);
                Timepoint floor = exclusiveSelectableTimes.floor(current);
                return !(current.equals(ceil, Timepoint.MINUTE) || current.equals(floor, Timepoint.MINUTE));
            }

            if (!mDisabledTimes.isEmpty() && resolution == Timepoint.MINUTE) {
                Timepoint ceil = mDisabledTimes.ceiling(current);
                Timepoint floor = mDisabledTimes.floor(current);
                boolean ceilExclude = current.equals(ceil, Timepoint.MINUTE);
                boolean floorExclude = current.equals(floor, Timepoint.MINUTE);
                return ceilExclude || floorExclude;
            }

            return false;
        } else return isOutOfRange(current);
    }

    public boolean isOutOfRange(@NonNull Timepoint current) {
        if (mMinTime != null && mMinTime.compareTo(current) > 0) return true;

        if (mMaxTime != null && mMaxTime.compareTo(current) < 0) return true;

        if (!exclusiveSelectableTimes.isEmpty()) return !exclusiveSelectableTimes.contains(current);

        return mDisabledTimes.contains(current);
    }

    @Override
    public Timepoint roundToNearest(@NonNull Timepoint time, @Timepoint.TYPE int type, @Timepoint.TYPE int resolution) {
        if (mMinTime != null && mMinTime.compareTo(time) > 0) return mMinTime;

        if (mMaxTime != null && mMaxTime.compareTo(time) < 0) return mMaxTime;

        if (!exclusiveSelectableTimes.isEmpty()) {
            Timepoint floor = exclusiveSelectableTimes.floor(time);
            Timepoint ceil = exclusiveSelectableTimes.ceiling(time);

            if (floor == null || ceil == null) {
                Timepoint t = floor == null ? ceil : floor;
                if (t.getHour() != time.getHour()) return time;
                if (type == Timepoint.MINUTE && t.getMinute() != time.getMinute()) return time;
                return t;
            }

            if (type == Timepoint.HOUR) {
                if (floor.getHour() != time.getHour() && ceil.getHour() == time.getHour())
                    return ceil;
                if (floor.getHour() == time.getHour() && ceil.getHour() != time.getHour())
                    return floor;
                if (floor.getHour() != time.getHour() && ceil.getHour() != time.getHour())
                    return time;
            }

            if (type == Timepoint.MINUTE) {
                if (floor.getHour() != time.getHour() && ceil.getHour() != time.getHour())
                    return time;
                if (floor.getHour() != time.getHour() && ceil.getHour() == time.getHour()) {
                    return ceil.getMinute() == time.getMinute() ? ceil : time;
                }
                if (floor.getHour() == time.getHour() && ceil.getHour() != time.getHour()) {
                    return floor.getMinute() == time.getMinute() ? floor : time;
                }
                if (floor.getMinute() != time.getMinute() && ceil.getMinute() == time.getMinute())
                    return ceil;
                if (floor.getMinute() == time.getMinute() && ceil.getMinute() != time.getMinute())
                    return floor;
                if (floor.getMinute() != time.getMinute() && ceil.getMinute() != time.getMinute())
                    return time;
            }

            int floorDist = Math.abs(time.compareTo(floor));
            int ceilDist = Math.abs(time.compareTo(ceil));

            return floorDist < ceilDist ? floor : ceil;
        }

        if (!mDisabledTimes.isEmpty()) {
            // if type matches resolution: cannot change anything, return input
            if (type == resolution) return time;

            if (resolution == Timepoint.MINUTE) {
                Timepoint ceil = mDisabledTimes.ceiling(time);
                Timepoint floor = mDisabledTimes.floor(time);
                boolean ceilDisabled = time.equals(ceil, Timepoint.MINUTE);
                boolean floorDisabled = time.equals(floor, Timepoint.MINUTE);

                if (ceilDisabled || floorDisabled)
                    return searchValidTimePoint(time, type, resolution);
                return time;
            }

            if (resolution == Timepoint.HOUR) {
                Timepoint ceil = mDisabledTimes.ceiling(time);
                Timepoint floor = mDisabledTimes.floor(time);
                boolean ceilDisabled = time.equals(ceil, Timepoint.HOUR);
                boolean floorDisabled = time.equals(floor, Timepoint.HOUR);

                if (ceilDisabled || floorDisabled)
                    return searchValidTimePoint(time, type, resolution);
                return time;
            }
        }

        return time;
    }

    private Timepoint searchValidTimePoint(@NonNull Timepoint time, @Timepoint.TYPE int type, @Timepoint.TYPE int resolution) {
        Timepoint forward = new Timepoint(time);
        Timepoint backward = new Timepoint(time);
        int iteration = 0;
        int resolutionMultiplier = 1;
        if (resolution == Timepoint.MINUTE) resolutionMultiplier = 60;

        while (iteration < 24 * resolutionMultiplier) {
            iteration++;
            forward.add(resolution, 1);
            backward.add(resolution, -1);

            if (forward.get(type) == time.get(type)) {
                Timepoint forwardCeil = mDisabledTimes.ceiling(forward);
                Timepoint forwardFloor = mDisabledTimes.floor(forward);
                if (!forward.equals(forwardCeil, resolution) && !forward.equals(forwardFloor, resolution))
                    return forward;
            }

            if (backward.get(type) == time.get(type)) {
                Timepoint backwardCeil = mDisabledTimes.ceiling(backward);
                Timepoint backwardFloor = mDisabledTimes.floor(backward);
                if (!backward.equals(backwardCeil, resolution) && !backward.equals(backwardFloor, resolution))
                    return backward;
            }

            if (backward.get(type) != time.get(type) && forward.get(type) != time.get(type))
                break;
        }
        // If this step is reached, the user has disabled all timepoints
        return time;
    }
}
