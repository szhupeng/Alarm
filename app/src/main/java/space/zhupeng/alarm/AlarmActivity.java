package space.zhupeng.alarm;

import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import space.zhupeng.alarm.slide.SlideConfig;
import space.zhupeng.alarm.slide.SlideHandler;

/**
 * Created by zhupeng on 2017/10/9.
 */

public class AlarmActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_alarm);

        SlideConfig config = new SlideConfig.Builder()
                .primaryColor(getResources().getColor(R.color.colorPrimary))
                .secondaryColor(getResources().getColor(R.color.colorAccent))
                .position(SlideConfig.BOTTOM)
                .sensitivity(1f)
                .scrimColor(Color.BLACK)
                .scrimStartAlpha(0.8f)
                .scrimEndAlpha(0f)
                .velocityThreshold(2400)
                .distanceThreshold(0.25f)
                .edgeOnly(true)
                .edgeSize(0.18f)
                .build();

        SlideHandler.attach(this, config);

        TextView tvTips = (TextView) findViewById(R.id.tv_tips);
        ArrowDrawable drawable = new ArrowDrawable(Color.WHITE, 4f, 48);
        drawable.setBounds(0, 0, drawable.getMinimumWidth(), drawable.getIntrinsicHeight());
        tvTips.setCompoundDrawables(null, drawable, null, null);
    }
}
