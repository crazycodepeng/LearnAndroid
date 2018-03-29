package liup.code.learnandroid.accessibility;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.ImageButton;

import liup.code.learnandroid.R;

/**
 * @title 事例主要讲述Android 辅助功能的入门级使用
 * @desc . {@link android.accessibilityservice.AccessibilityService}
 * Created by liupeng on 2018/3/28.
 */

public class ClockBackActivity extends Activity {

    /** 进入系统辅助设置界面 */
    private static final Intent sSettingsIntent =
            new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.accessibility_service);

        ImageButton button = (ImageButton) findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(sSettingsIntent);
            }
        });
    }
}
