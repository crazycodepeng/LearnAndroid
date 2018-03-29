package liup.code.learnandroid.animation;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;

import liup.code.learnandroid.R;

/**
 * @title 此应用程序演示了如何使用XML中的animateLayoutChanges标签来自动转换动画，因为项目从容器中移除或添加到容器。
 * @desc .
 * Created by liupeng on 2018/3/29.
 */
public class LayoutAnimationsByDefault extends Activity {
    private int numButtons = 1;

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_animations_by_default);

        final GridLayout gridContainer = (GridLayout) findViewById(R.id.gridContainer);

        Button addButton = (Button) findViewById(R.id.addNewButton);
        addButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Button newButton = new Button(LayoutAnimationsByDefault.this);
                newButton.setText(String.valueOf(numButtons++));
                newButton.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        gridContainer.removeView(v);
                    }
                });
                gridContainer.addView(newButton, Math.min(1, gridContainer.getChildCount()));
            }
        });
    }


}
