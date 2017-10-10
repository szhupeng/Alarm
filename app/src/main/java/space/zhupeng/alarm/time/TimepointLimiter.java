package space.zhupeng.alarm.time;

import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

@SuppressWarnings("WeakerAccess")
public interface TimepointLimiter extends Parcelable {
    boolean isOutOfRange(@Nullable Timepoint point, int index, @Timepoint.TYPE int resolution);

    @NonNull
    Timepoint roundToNearest(
            @NonNull Timepoint time,
            @Timepoint.TYPE int type,
            @Timepoint.TYPE int resolution
    );
}